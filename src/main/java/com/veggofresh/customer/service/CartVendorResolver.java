package com.veggofresh.customer.service;

import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.vendor.service.ProductCatalogService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Single owner of the multi-cart vendor-overlap math (PROJECT_STATE section 2).
 *
 * <p>Why this is a component and not private methods on CartServiceImpl: the
 * same three questions have to be answered in two places, and the previous
 * split (add-time in CartServiceImpl, checkout-time in OrderServiceImpl) is how
 * the two drifted apart in the first place. Both now ask this class:
 *
 * <ol>
 *   <li>which cart, if any, can absorb a new product ({@link Session#bestCartFor})</li>
 *   <li>what is a given cart's real, live vendor overlap ({@link Session#effectiveVendorIds})</li>
 *   <li>does a cart still hold together after the catalog moved ({@link Session#revalidate})</li>
 * </ol>
 *
 * <h2>The invariant this class exists to maintain</h2>
 *
 * A cart's stored {@code candidateVendorIds} must ALWAYS equal the
 * intersection of its items' live vendor sets. The old implementation broke
 * that invariant: it treated the stored set as authoritative and narrowed it
 * destructively on every add, so the stored set could only ever shrink. That
 * makes cart selection a strictly greedy, order-dependent walk over
 * permanently-degraded data, and it over-splits. Concretely:
 *
 * <pre>
 *   item A, vendors {V1,V2}  -> new cart 1, stored {V1,V2}
 *   item B, vendors {V2,V3}  -> joins cart 1, stored narrows to {V2}
 *   item C, vendors {V1}     -> cart 1 now reads {V2}; {V2} n {V1} = {}
 *                              -> spuriously opens cart 2
 * </pre>
 *
 * Cart 1's items could still be served by V1, but the narrowing had already
 * thrown that information away. Recomputing the live intersection per item set
 * keeps the stored set truthful, and the stored set stays truthful across item
 * removal too, which is what makes removal able to widen a cart again instead
 * of leaving it permanently over-restricted.
 *
 * <h2>What is deliberately NOT done here</h2>
 *
 * No global re-partitioning of a customer's already-open carts. Re-deriving the
 * minimum number of carts from scratch is a set-partition problem, and solving
 * it would silently move already-chosen items between carts and renumber the
 * customer's "Cart 1 / Cart 2" tabs mid-shop. Cart stickiness is a product
 * decision, not an oversight, so this stays a best-fit incremental choice.
 * {@link Session#bestCartFor} picks the cart that retains the MOST vendor
 * overlap rather than the first that retains any, which is the cheapest
 * available reduction in over-splitting.
 */
@Component
@RequiredArgsConstructor
public class CartVendorResolver {

    private final ProductCatalogService productCatalogService;

    /**
     * Opens a resolution session pinned to one reference location. Sessions are
     * per-operation and never reused across requests -- vendor eligibility is
     * location-dependent, so a cached set from one location is meaningless at
     * another.
     */
    public Session newSession(double latitude, double longitude) {
        return new Session(productCatalogService, latitude, longitude);
    }

    /**
     * A single location-pinned working set. Caches each product's vendor set
     * because the same product is otherwise re-queried once per cart on every
     * add (and once per line item at checkout), which is the N+1 that made
     * multi-cart expensive.
     */
    public static final class Session {

        private final ProductCatalogService productCatalogService;
        private final double latitude;
        private final double longitude;
        private final Map<UUID, Set<UUID>> vendorCache = new HashMap<>();

        private Session(ProductCatalogService productCatalogService, double latitude, double longitude) {
            this.productCatalogService = productCatalogService;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public double latitude() {
            return latitude;
        }

        public double longitude() {
            return longitude;
        }

        /** Vendors currently able to serve this product to this customer. Never null; may be empty. */
        public Set<UUID> vendorsFor(UUID productId) {
            return vendorCache.computeIfAbsent(productId, id -> {
                Set<UUID> vendors = productCatalogService.getShopIdsForProduct(id, latitude, longitude);
                // Copy: the caller (and the cart entity) may hold/mutate this.
                return vendors == null ? Set.of() : Set.copyOf(vendors);
            });
        }

        /**
         * The cart's real, live vendor overlap: the intersection across every
         * one of its items. An empty cart has no overlap. A cart holding a
         * product that has since gone out of range collapses to an empty set,
         * which correctly reports the cart as no longer shippable as one order.
         */
        public Set<UUID> effectiveVendorIds(Cart cart) {
            return intersectionFor(cart == null ? List.of() : cart.getItems());
        }

        /** The intersection of the vendor sets of every given item. Empty input or any dead item yields an empty set. */
        public Set<UUID> intersectionFor(Collection<CartItem> items) {
            if (items == null || items.isEmpty()) {
                return Set.of();
            }
            Set<UUID> running = null;
            for (CartItem item : items) {
                Set<UUID> itemVendors = vendorsFor(item.getProductId());
                if (running == null) {
                    running = new HashSet<>(itemVendors);
                } else {
                    running.retainAll(itemVendors);
                }
                if (running.isEmpty()) {
                    return Set.of();
                }
            }
            return running == null ? Set.of() : running;
        }

        /**
         * True when this cart's items can still be fulfilled by a single vendor,
         * i.e. the cart is one coherent order. Re-run at checkout time because a
         * cart's overlap can rot without the customer touching it (PROJECT_STATE
         * section 2, "Revisit-after-a-delay edge case").
         */
        public boolean revalidate(Cart cart) {
            return !effectiveVendorIds(cart).isEmpty();
        }

        /**
         * Best existing cart for a new product, or empty if the product starts a
         * new cart.
         *
         * <p>Considers only carts that still hold items, and scores each by how
         * much vendor redundancy survives the join
         * ({@code |cart overlap ∩ product vendors|}) rather than stopping at the
         * first cart that fits. Picking the best-fitting cart is what stops a
         * product that could serve either of two carts from being wedged into
         * whichever one happened to be created first, permanently over-restricting
         * it and forcing the next add into yet another cart. Ties break toward
         * the earliest cart in the list, which is oldest-first from the
         * repository, so the customer's cart order and labels stay stable.
         */
        public Optional<Cart> bestCartFor(List<Cart> carts, UUID productId) {
            if (carts == null || carts.isEmpty()) {
                return Optional.empty();
            }
            Set<UUID> productVendors = vendorsFor(productId);
            if (productVendors.isEmpty()) {
                return Optional.empty();
            }

            Cart best = null;
            int bestScore = 0;
            for (Cart cart : carts) {
                if (cart == null || cart.getItems() == null || cart.getItems().isEmpty()) {
                    continue;
                }
                Set<UUID> overlap = new HashSet<>(effectiveVendorIds(cart));
                overlap.retainAll(productVendors);
                if (overlap.size() > bestScore) {
                    bestScore = overlap.size();
                    best = cart;
                }
            }
            return Optional.ofNullable(best);
        }

        /**
         * Repairs {@code cart.candidateVendorIds} to match its items' live
         * intersection. Call after ANY change to a cart's line items — add,
         * merge, or remove. This is what makes the stored set self-healing
         * rather than monotonically degrading.
         */
        public Set<UUID> resync(Cart cart) {
            Set<UUID> live = new HashSet<>(effectiveVendorIds(cart));
            cart.setCandidateVendorIds(live);
            return live;
        }

        /**
         * The usable items of a cart at this location, in list order, paired with
         * their resolved product. Items whose product is no longer available are
         * omitted rather than throwing, so one dead line item degrades a single
         * cart instead of aborting a whole checkout.
         */
        public List<CartItem> onlyShippableItems(Cart cart) {
            List<CartItem> shippable = new ArrayList<>();
            if (cart == null || cart.getItems() == null) {
                return shippable;
            }
            for (CartItem item : cart.getItems()) {
                if (item != null && !vendorsFor(item.getProductId()).isEmpty()) {
                    shippable.add(item);
                }
            }
            return shippable;
        }
    }
}
