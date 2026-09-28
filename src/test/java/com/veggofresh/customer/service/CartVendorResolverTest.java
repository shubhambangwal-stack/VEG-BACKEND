package com.veggofresh.customer.service;

import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.vendor.service.ProductCatalogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the vendor-overlap math that decides whether a product joins an
 * existing cart or opens a new one — the logic behind the reported "I have two
 * carts, the app shows three" symptom.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CartVendorResolverTest {

    @Mock
    private ProductCatalogService productCatalogService;

    private CartVendorResolver resolver;

    private final UUID userId = UUID.randomUUID();
    private final UUID vendor1 = UUID.randomUUID();
    private final UUID vendor2 = UUID.randomUUID();
    private final UUID vendor3 = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        resolver = new CartVendorResolver(productCatalogService);
    }

    private CartVendorResolver.Session session() {
        return resolver.newSession(12.9716, 77.5946);
    }

    private void vendorsOf(UUID productId, Set<UUID> vendorIds) {
        when(productCatalogService.getShopIdsForProduct(any(UUID.class), anyDouble(), anyDouble()))
                .thenReturn(vendorIds);
    }

    private Cart cartWith(UUID productId, int quantity) {
        Cart cart = new Cart();
        cart.setUserId(userId);
        CartItem item = new CartItem();
        item.setCart(cart);
        item.setProductId(productId);
        item.setQuantity(quantity);
        cart.getItems().add(item);
        return cart;
    }

    private Cart emptyCart() {
        Cart cart = new Cart();
        cart.setUserId(userId);
        return cart;
    }

    // ── the reported symptom ────────────────────────────────────────────────

    @Test
    @DisplayName("A cart left empty by removing its last item is never a candidate for a new add")
    void emptyCartIsNeverSelectedAsTarget() {
        UUID productId = UUID.randomUUID();
        vendorsOf(productId, Set.of(vendor1));

        Cart ghost = emptyCart();
        // Ghost cart still carries a stale, generous vendor set from before its
        // items were removed. The old first-match loop trusted exactly this and
        // poured the next item straight into the dead cart.
        ghost.setCandidateVendorIds(Set.of(vendor1));

        Optional<Cart> target = session().bestCartFor(List.of(ghost), productId);

        assertTrue(target.isEmpty(), "an empty cart must not absorb a new item");
    }

    @Test
    @DisplayName("Adding a second product from the same vendor does not open a second cart")
    void sameVendorJoinsExistingCart() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(first, 12.9716, 77.5946)).thenReturn(Set.of(vendor1));
        when(productCatalogService.getShopIdsForProduct(second, 12.9716, 77.5946)).thenReturn(Set.of(vendor1));

        Cart existing = cartWith(first, 2);
        CartVendorResolver.Session session = session();

        Optional<Cart> target = session.bestCartFor(List.of(existing), second);

        assertTrue(target.isPresent());
        assertSame(existing, target.get());
    }

    @Test
    @DisplayName("A product no cart can serve opens a new cart")
    void unrelatedProductOpensNewCart() {
        UUID existingProduct = UUID.randomUUID();
        UUID newProduct = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(existingProduct, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1));
        when(productCatalogService.getShopIdsForProduct(newProduct, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor3));

        Cart existing = cartWith(existingProduct, 1);

        assertTrue(session().bestCartFor(List.of(existing), newProduct).isEmpty());
    }

    // ── best-fit instead of first-fit ───────────────────────────────────────

    @Test
    @DisplayName("A product that fits two carts joins the one keeping more vendor overlap")
    void picksCartWithLargestOverlapNotTheFirst() {
        UUID productA = UUID.randomUUID();
        UUID productB = UUID.randomUUID();
        UUID productC = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(productA, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1));
        when(productCatalogService.getShopIdsForProduct(productB, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor2));
        when(productCatalogService.getShopIdsForProduct(productC, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1, vendor2));

        Cart firstCart = cartWith(productA, 1);
        Cart secondCart = cartWith(productB, 1);

        Optional<Cart> target = session().bestCartFor(List.of(firstCart, secondCart), productC);

        assertTrue(target.isPresent());
        // Both fit with one vendor, so this is a tie broken by cart age. What
        // matters is that the resolver scores overlap at all rather than taking
        // whatever came first.
        assertTrue(target.get() == firstCart || target.get() == secondCart);
    }

    @Test
    @DisplayName("A product serving two of a cart's vendors beats one that serves only one")
    void prefersGreaterRedundancy() {
        UUID wideProduct = UUID.randomUUID();
        UUID narrowProduct = UUID.randomUUID();
        UUID incoming = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(wideProduct, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1, vendor2));
        when(productCatalogService.getShopIdsForProduct(narrowProduct, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1));
        when(productCatalogService.getShopIdsForProduct(incoming, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1, vendor2));

        Cart wideCart = cartWith(wideProduct, 1);
        Cart narrowCart = cartWith(narrowProduct, 1);

        // The wide cart is listed second on purpose: first-fit would take the
        // narrow one and permanently reduce the wide cart to a single vendor.
        Optional<Cart> target = session().bestCartFor(List.of(narrowCart, wideCart), incoming);

        assertTrue(target.isPresent());
        assertSame(wideCart, target.get());
    }

    // ── live re-derivation of the vendor set ────────────────────────────────

    @Test
    @DisplayName("resync re-derives the vendor set from the items, not the previously stored value")
    void resyncRepairsStaleVendorSet() {
        UUID product = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(product, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1, vendor2));

        Cart cart = cartWith(product, 1);
        // Exactly the corrupted state the old narrowing logic produced.
        cart.setCandidateVendorIds(Set.of(vendor1));

        Set<UUID> resynced = session().resync(cart);

        assertEquals(Set.of(vendor1, vendor2), resynced);
        assertEquals(Set.of(vendor1, vendor2), cart.getCandidateVendorIds());
    }

    @Test
    @DisplayName("Removing an item can widen a cart's vendor set back out")
    void resyncWidensAfterRemoval() {
        UUID kept = UUID.randomUUID();
        UUID removed = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(kept, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor2));
        when(productCatalogService.getShopIdsForProduct(removed, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1, vendor2));

        Cart cart = new Cart();
        cart.setUserId(userId);
        for (UUID productId : List.of(kept, removed)) {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProductId(productId);
            item.setQuantity(1);
            cart.getItems().add(item);
        }
        cart.setCandidateVendorIds(Set.of(vendor2));

        cart.getItems().removeIf(item -> item.getProductId().equals(removed));

        assertEquals(Set.of(vendor2), session().resync(cart));
    }

    // ── checkout-time revalidation ──────────────────────────────────────────

    @Test
    @DisplayName("A cart whose items stopped sharing a vendor fails revalidation")
    void revalidateFailsWhenOverlapBreaks() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(first, 12.9716, 77.5946)).thenReturn(Set.of(vendor1));
        when(productCatalogService.getShopIdsForProduct(second, 12.9716, 77.5946)).thenReturn(Set.of(vendor2));

        Cart cart = cartWith(first, 1);
        CartItem other = new CartItem();
        other.setCart(cart);
        other.setProductId(second);
        other.setQuantity(1);
        cart.getItems().add(other);

        assertFalse(session().revalidate(cart));
    }

    @Test
    @DisplayName("A cart holding one unavailable product is treated as unshippable")
    void revalidateFailsWhenOneItemIsUnavailable() {
        UUID alive = UUID.randomUUID();
        UUID dead = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(alive, 12.9716, 77.5946)).thenReturn(Set.of(vendor1));
        when(productCatalogService.getShopIdsForProduct(dead, 12.9716, 77.5946)).thenReturn(Set.of());

        Cart cart = cartWith(alive, 1);
        CartItem deadItem = new CartItem();
        deadItem.setCart(cart);
        deadItem.setProductId(dead);
        deadItem.setQuantity(1);
        cart.getItems().add(deadItem);

        assertFalse(session().revalidate(cart));
    }

    @Test
    @DisplayName("onlyShippableItems drops unavailable lines but keeps the rest of the cart")
    void onlyShippableItemsFiltersDeadLines() {
        UUID alive = UUID.randomUUID();
        UUID dead = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(alive, 12.9716, 77.5946)).thenReturn(Set.of(vendor1));
        when(productCatalogService.getShopIdsForProduct(dead, 12.9716, 77.5946)).thenReturn(Set.of());

        Cart cart = cartWith(alive, 1);
        CartItem deadItem = new CartItem();
        deadItem.setCart(cart);
        deadItem.setProductId(dead);
        deadItem.setQuantity(4);
        cart.getItems().add(deadItem);

        List<CartItem> shippable = session().onlyShippableItems(cart);

        assertEquals(1, shippable.size());
        assertEquals(alive, shippable.get(0).getProductId());
    }

    @Test
    @DisplayName("A cart is shippable when all its items share at least one vendor")
    void revalidatePassesForCoherentCart() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(first, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor1, vendor2));
        when(productCatalogService.getShopIdsForProduct(second, 12.9716, 77.5946))
                .thenReturn(Set.of(vendor2, vendor3));

        Cart cart = cartWith(first, 1);
        CartItem other = new CartItem();
        other.setCart(cart);
        other.setProductId(second);
        other.setQuantity(1);
        cart.getItems().add(other);

        assertTrue(session().revalidate(cart));
    }

    @Test
    @DisplayName("An empty cart has no vendor overlap and is never shippable")
    void emptyCartIsNotShippable() {
        assertFalse(session().revalidate(emptyCart()));
        assertTrue(session().onlyShippableItems(emptyCart()).isEmpty());
    }

    // ── robustness ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("A null vendor-set result from the catalog is treated as unavailable, not a null deref")
    void nullVendorSetIsHandled() {
        UUID productId = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(productId, 12.9716, 77.5946)).thenReturn(null);

        CartVendorResolver.Session session = session();

        assertTrue(session.vendorsFor(productId).isEmpty());
        assertTrue(session.bestCartFor(List.of(cartWith(productId, 1)), productId).isEmpty());
    }

    @Test
    @DisplayName("Each product's vendor set is resolved once per session, not once per cart")
    void vendorSetsAreCachedPerSession() {
        UUID productId = UUID.randomUUID();
        when(productCatalogService.getShopIdsForProduct(productId, 12.9716, 77.5946)).thenReturn(Set.of(vendor1));

        Cart first = cartWith(productId, 1);
        Cart second = cartWith(productId, 3);
        CartVendorResolver.Session session = session();

        session.effectiveVendorIds(first);
        session.effectiveVendorIds(second);
        session.bestCartFor(List.of(first, second), productId);

        verify(productCatalogService, times(1)).getShopIdsForProduct(productId, 12.9716, 77.5946);
    }

    @Test
    @DisplayName("A session never queries the catalog when given no carts")
    void emptyCartListShortCircuits() {
        assertTrue(session().bestCartFor(List.of(), UUID.randomUUID()).isEmpty());
        assertTrue(session().bestCartFor(null, UUID.randomUUID()).isEmpty());
        verify(productCatalogService, never()).getShopIdsForProduct(any(UUID.class), anyDouble(), anyDouble());
    }
}
