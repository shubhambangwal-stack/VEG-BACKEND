package com.veggofresh.customer.service;

import com.veggofresh.admin.service.PlatformSettingsService;
import com.veggofresh.customer.dto.response.CartCheckoutBreakdownDto;
import com.veggofresh.customer.dto.response.CheckoutSummaryDto;
import com.veggofresh.customer.entity.Address;
import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.customer.repository.AddressRepository;
import com.veggofresh.customer.repository.CartRepository;
import com.veggofresh.customer.service.impl.CartServiceImpl;
import com.veggofresh.platform.exception.BusinessException;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * The checkout summary is the endpoint that reports "here is what you are about
 * to be charged". It must agree with the cart screen on which carts exist, what
 * they are labelled, and how many items each holds — the client uses the label
 * to line a breakdown up against a cart, and the customer compares the totals
 * against the badge.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceCheckoutSummaryTest {

    private static final double LAT = 12.9716;
    private static final double LNG = 77.5946;
    private static final BigDecimal DELIVERY_FEE = new BigDecimal("20");
    private static final BigDecimal PLATFORM_FEE = new BigDecimal("5");

    @Mock
    private CartRepository cartRepository;
    @Mock
    private AddressRepository addressRepository;
    @Mock
    private ProductCatalogService productCatalogService;
    @Mock
    private PlatformSettingsService platformSettingsService;

    private Object orderService;
    private List<Cart> openCarts;
    private Address address;

    private final UUID userId = UUID.randomUUID();
    private final UUID vendor1 = UUID.randomUUID();
    private final UUID vendor2 = UUID.randomUUID();

    @BeforeEach
    void setUp() throws Exception {
        orderService = instantiateOrderService();

        openCarts = new ArrayList<>();
        address = new Address();
        address.setId(UUID.randomUUID());
        address.setUserId(userId);
        address.setLatitude(LAT);
        address.setLongitude(LNG);
        address.setDefault(true);

        when(addressRepository.findByIdAndUserId(address.getId(), userId)).thenReturn(Optional.of(address));
        when(cartRepository.findByUserIdOrderByCreatedAtAscIdAsc(userId))
                .thenAnswer(invocation -> openCarts.stream()
                        .filter(cart -> !cart.isDeleted())
                        .collect(java.util.stream.Collectors.toList()));
        when(platformSettingsService.getDeliveryFeeAmount()).thenReturn(DELIVERY_FEE);
        when(platformSettingsService.getPlatformFeeAmount()).thenReturn(PLATFORM_FEE);
    }

    /**
     * OrderServiceImpl has 12 collaborators, none of which the checkout summary
     * touches. They are supplied as nulls via reflection so this test exercises
     * the real production class without dragging in the whole service graph.
     */
    private Object instantiateOrderService() throws Exception {
        Class<?> type = Class.forName("com.veggofresh.customer.service.impl.OrderServiceImpl");
        Class<?>[] paramTypes = new Class<?>[type.getDeclaredConstructors()[0].getParameterCount()];
        Object[] args = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            paramTypes[i] = type.getDeclaredConstructors()[0].getParameterTypes()[i];
            if (paramTypes[i].isAssignableFrom(CartRepository.class)) {
                args[i] = cartRepository;
            } else if (paramTypes[i].isAssignableFrom(AddressRepository.class)) {
                args[i] = addressRepository;
            } else if (paramTypes[i].isAssignableFrom(ProductCatalogService.class)) {
                args[i] = productCatalogService;
            } else if (paramTypes[i].isAssignableFrom(PlatformSettingsService.class)) {
                args[i] = platformSettingsService;
            } else if (paramTypes[i].isAssignableFrom(CartVendorResolver.class)) {
                args[i] = new CartVendorResolver(productCatalogService);
            }
        }
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return constructor.newInstance(args);
    }

    /** Previews every open cart — the cartIds argument is null/empty. */
    private CheckoutSummaryDto summary() {
        return (CheckoutSummaryDto) invoke("getCheckoutSummary",
                new Class<?>[] { UUID.class, UUID.class, List.class }, userId, address.getId(), null);
    }

    private CheckoutSummaryDto summaryOf(List<UUID> cartIds) {
        return (CheckoutSummaryDto) invoke("getCheckoutSummary",
                new Class<?>[] { UUID.class, UUID.class, List.class }, userId, address.getId(), cartIds);
    }

    /**
     * Calls the real production method and lets any business exception surface
     * unchanged — reflection would otherwise bury it in an
     * InvocationTargetException and make every error-path assertion useless.
     */
    private Object invoke(String method, Class<?>[] paramTypes, Object... args) {
        try {
            java.lang.reflect.Method m = orderService.getClass().getDeclaredMethod(method, paramTypes);
            m.setAccessible(true);
            return m.invoke(orderService, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            if (e.getCause() instanceof RuntimeException runtime) {
                throw runtime;
            }
            throw new IllegalStateException(method + " threw a checked exception", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("could not invoke " + method, e);
        }
    }

    private void stubProduct(UUID productId, BigDecimal price, Set<UUID> vendors) {
        lenient().when(productCatalogService.getShopIdsForProduct(productId, LAT, LNG)).thenReturn(vendors);
        lenient().when(productCatalogService.findEligibleProductById(productId, LAT, LNG))
                .thenReturn(Optional.of(ProductDto.builder()
                        .id(productId)
                        .name("Product " + productId)
                        .price(price)
                        .unit("kg")
                        .build()));
    }

    private void stubUnavailable(UUID productId) {
        lenient().when(productCatalogService.getShopIdsForProduct(productId, LAT, LNG)).thenReturn(Set.of());
        lenient().when(productCatalogService.findEligibleProductById(productId, LAT, LNG))
                .thenReturn(Optional.empty());
    }

    private Cart givenOpenCart(int[] quantities, UUID... productIds) {
        Cart cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        for (int i = 0; i < productIds.length; i++) {
            CartItem item = new CartItem();
            item.setId(UUID.randomUUID());
            item.setCart(cart);
            item.setProductId(productIds[i]);
            item.setQuantity(quantities[i]);
            cart.getItems().add(item);
        }
        openCarts.add(cart);
        return cart;
    }

    // ── the reported symptom ────────────────────────────────────────────────

    @Test
    @DisplayName("Two carts produce two breakdowns, not three")
    void twoCartsProduceTwoBreakdowns() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        stubProduct(a, new BigDecimal("10"), Set.of(vendor1));
        stubProduct(b, new BigDecimal("20"), Set.of(vendor2));
        givenOpenCart(new int[] { 1 }, a);
        givenOpenCart(new int[] { 1 }, b);

        CheckoutSummaryDto summary = summary();

        assertEquals(2, summary.getCarts().size());
        assertEquals("Cart 1", summary.getCarts().get(0).getCartLabel());
        assertEquals("Cart 2", summary.getCarts().get(1).getCartLabel());
    }

    @Test
    @DisplayName("A leftover empty cart is skipped instead of consuming a label or a breakdown slot")
    void emptyCartIsSkippedNotCounted() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        stubProduct(a, new BigDecimal("10"), Set.of(vendor1));
        stubProduct(b, new BigDecimal("20"), Set.of(vendor2));
        givenOpenCart(new int[] { 1 }, a);
        givenOpenCart(new int[] { 1 }, b);
        Cart ghost = new Cart();
        ghost.setId(UUID.randomUUID());
        ghost.setUserId(userId);
        openCarts.add(ghost);

        CheckoutSummaryDto summary = summary();

        assertEquals(2, summary.getCarts().size(), "three open carts, one empty, must give two breakdowns");
        assertEquals("Cart 1", summary.getCarts().get(0).getCartLabel());
        assertEquals("Cart 2", summary.getCarts().get(1).getCartLabel());
    }

    @Test
    @DisplayName("An empty first cart does not push the real cart to 'Cart 2'")
    void emptyFirstCartDoesNotShiftLabels() {
        UUID b = UUID.randomUUID();
        stubProduct(b, new BigDecimal("20"), Set.of(vendor2));
        Cart ghost = new Cart();
        ghost.setId(UUID.randomUUID());
        ghost.setUserId(userId);
        openCarts.add(ghost);
        givenOpenCart(new int[] { 1 }, b);

        CheckoutSummaryDto summary = summary();

        assertEquals(1, summary.getCarts().size());
        assertEquals("Cart 1", summary.getCarts().get(0).getCartLabel(),
                "labels are positional among real carts, so this must be Cart 1");
    }

    // ── the count contract ───────────────────────────────────────────────────

    @Test
    @DisplayName("itemCount sums quantities, matching the cart screen and the badge")
    void itemCountSumsQuantitiesNotLineItems() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        stubProduct(a, new BigDecimal("10"), Set.of(vendor1));
        stubProduct(b, new BigDecimal("20"), Set.of(vendor1));
        givenOpenCart(new int[] { 2, 5 }, a, b);

        CheckoutSummaryDto summary = summary();

        assertEquals(7, summary.getCarts().get(0).getItemCount(),
                "2 + 5 units is 7 items; the old code reported the 2 line items");
        assertEquals(7, summary.getTotalItemCount());
    }

    @Test
    @DisplayName("The grand total is the sum of the per-cart totals, one fee pair per cart")
    void grandTotalSumsPerCartTotals() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        stubProduct(a, new BigDecimal("10"), Set.of(vendor1));
        stubProduct(b, new BigDecimal("20"), Set.of(vendor2));
        givenOpenCart(new int[] { 3 }, a);
        givenOpenCart(new int[] { 2 }, b);

        CheckoutSummaryDto summary = summary();

        assertEquals(new BigDecimal("30"), summary.getCarts().get(0).getSubtotal());
        assertEquals(new BigDecimal("40"), summary.getCarts().get(1).getSubtotal());
        assertEquals(new BigDecimal("55"), summary.getCarts().get(0).getTotal(), "30 + 20 + 5");
        assertEquals(new BigDecimal("65"), summary.getCarts().get(1).getTotal(), "40 + 20 + 5");
        assertEquals(new BigDecimal("120"), summary.getGrandTotal());
    }

    @Test
    @DisplayName("A cart that is wholly unavailable is omitted from the summary, like it is from the cart screen")
    void unavailableLinesAreExcludedAndReported() {
        UUID dead = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        stubUnavailable(dead);
        stubProduct(healthy, new BigDecimal("50"), Set.of(vendor2));
        givenOpenCart(new int[] { 4 }, dead);
        givenOpenCart(new int[] { 2 }, healthy);

        CheckoutSummaryDto summary = summary();

        // The dead cart is not listed AND not reported. It is invisible on the
        // cart screen too, so raising an issue about a cart the customer cannot
        // see would leave them nothing to act on -- and numbering the healthy
        // cart from a list the customer cannot see is the label desync this
        // whole change exists to prevent. Carts that are visible but cannot ship
        // as one order are still reported; see brokenOverlapIsReportedNotCharged.
        assertEquals(1, summary.getCarts().size(), "only the buyable cart is summarised");
        assertTrue(summary.getIssues().isEmpty(), "an invisible cart is not raised as an issue");
        assertEquals(2, summary.getTotalItemCount(), "only the healthy cart counts, and it holds 2 units");
        assertEquals(new BigDecimal("125"), summary.getGrandTotal(), "50x2 + 20 + 5; dead cart not charged");
    }

    @Test
    @DisplayName("A cart that still holds together but has one dead line counts only the live line")
    void partialCartCountsOnlyLiveLines() {
        UUID alive = UUID.randomUUID();
        UUID dead = UUID.randomUUID();
        // The dead product is still listed, so the vendor overlap survives, but
        // the product itself no longer resolves for pricing.
        stubProduct(alive, new BigDecimal("10"), Set.of(vendor1));
        stubProduct(dead, new BigDecimal("99"), Set.of(vendor1));
        lenient().when(productCatalogService.findEligibleProductById(dead, LAT, LNG)).thenReturn(Optional.empty());
        givenOpenCart(new int[] { 3, 4 }, alive, dead);

        CheckoutSummaryDto summary = summary();

        assertEquals(1, summary.getCarts().size());
        assertEquals(3, summary.getCarts().get(0).getItemCount());
        assertEquals(1, summary.getCarts().get(0).getUnavailableItemCount());
        assertEquals(new BigDecimal("30"), summary.getCarts().get(0).getSubtotal());
    }

    // ── issues up front ─────────────────────────────────────────────────────

    @Test
    @DisplayName("A cart whose overlap has broken is reported as an issue and left out of the total")
    void brokenOverlapIsReportedNotCharged() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        stubProduct(a, new BigDecimal("10"), Set.of(vendor1));
        stubProduct(b, new BigDecimal("20"), Set.of(vendor2));
        stubProduct(healthy, new BigDecimal("30"), Set.of(vendor1, vendor2));
        // Two items that share no vendor can never be one order.
        givenOpenCart(new int[] { 1, 1 }, a, b);
        givenOpenCart(new int[] { 1 }, healthy);

        CheckoutSummaryDto summary = summary();

        assertEquals(1, summary.getIssues().size(), "the {V1} + {V2} cart cannot be one order");
        assertEquals(1, summary.getCarts().size());
        assertEquals(new BigDecimal("55"), summary.getGrandTotal(), "30 + 20 + 5, broken cart not charged");
    }

    @Test
    @DisplayName("A cart skipped as an issue keeps its positional label, so it still matches the cart screen")
    void skippedCartKeepsItsLabel() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID healthy = UUID.randomUUID();
        stubProduct(a, new BigDecimal("10"), Set.of(vendor1));
        stubProduct(b, new BigDecimal("20"), Set.of(vendor2));
        stubProduct(healthy, new BigDecimal("30"), Set.of(vendor1, vendor2));
        givenOpenCart(new int[] { 1, 1 }, a, b);
        givenOpenCart(new int[] { 1 }, healthy);

        CheckoutSummaryDto summary = summary();

        assertEquals("Cart 1", summary.getIssues().get(0).getCartLabel());
        assertEquals("Cart 2", summary.getCarts().get(0).getCartLabel(),
                "the second cart is still 'Cart 2' even though the first is being skipped");
    }

    @Test
    @DisplayName("A promo on a cart is reflected in the breakdown and the grand total")
    void promoIsReflectedInSummary() {
        UUID a = UUID.randomUUID();
        stubProduct(a, new BigDecimal("100"), Set.of(vendor1));
        Cart cart = givenOpenCart(new int[] { 1 }, a);
        cart.setPromoCode("FRESH10");
        cart.setPromoDiscount(new BigDecimal("10"));

        CheckoutSummaryDto summary = summary();

        CartCheckoutBreakdownDto breakdown = summary.getCarts().get(0);
        assertEquals("FRESH10", breakdown.getPromoCode());
        assertEquals(new BigDecimal("10"), breakdown.getPromoDiscount());
        assertEquals(new BigDecimal("115"), breakdown.getTotal(), "100 + 20 + 5 - 10");
        assertEquals(new BigDecimal("115"), summary.getGrandTotal());
    }

    @Test
    @DisplayName("A discount larger than the subtotal cannot drive the total negative")
    void oversizedPromoIsCapped() {
        UUID a = UUID.randomUUID();
        stubProduct(a, new BigDecimal("10"), Set.of(vendor1));
        Cart cart = givenOpenCart(new int[] { 1 }, a);
        cart.setPromoCode("STALE");
        cart.setPromoDiscount(new BigDecimal("9999"));

        CheckoutSummaryDto summary = summary();

        assertEquals(new BigDecimal("10"), summary.getCarts().get(0).getPromoDiscount());
        assertTrue(summary.getGrandTotal().compareTo(BigDecimal.ZERO) >= 0);
    }

    // ── guards ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("A summary for a customer with no carts is a clear error, not an empty grand total")
    void noCartsIsAnError() {
        BusinessException ex = assertThrows(BusinessException.class, this::summary);
        assertEquals("CART_EMPTY", ex.getErrorCode());
    }

    @Test
    @DisplayName("A summary for an address belonging to someone else is rejected")
    void foreignAddressIsRejected() {
        when(addressRepository.findByIdAndUserId(any(), any())).thenReturn(Optional.empty());

        BusinessException ex = assertThrows(BusinessException.class, this::summary);
        assertEquals("ADDRESS_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    @DisplayName("When no cart can be delivered, the summary is a clear error rather than a zero total")
    void allCartsUndeliverableIsAnError() {
        UUID a = UUID.randomUUID();
        stubUnavailable(a);
        givenOpenCart(new int[] { 1 }, a);

        BusinessException ex = assertThrows(BusinessException.class, this::summary);
        assertEquals("CART_EMPTY", ex.getErrorCode());
    }

    // ── selective checkout (cartIds, from e8ba06c) ─────────────────────────

    @Test
    @DisplayName("Previewing a subset of carts summarises only those carts")
    void cartIdsRestrictsThePreviewToThatSubset() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        stubProduct(first, new BigDecimal("50"), Set.of(UUID.randomUUID()));
        stubProduct(second, new BigDecimal("30"), Set.of(UUID.randomUUID()));
        Cart cartOne = givenOpenCart(new int[] { 1 }, first);
        Cart cartTwo = givenOpenCart(new int[] { 1 }, second);

        CheckoutSummaryDto summary = summaryOf(List.of(cartTwo.getId()));

        assertEquals(1, summary.getCarts().size(), "only the requested cart is previewed");
        assertEquals(cartTwo.getId(), summary.getCarts().get(0).getCartId());
    }

    @Test
    @DisplayName("A preview of one cart keeps that cart's real label, so it matches the cart screen")
    void cartIdsDoNotRenumberThePreview() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        stubProduct(first, new BigDecimal("50"), Set.of(UUID.randomUUID()));
        stubProduct(second, new BigDecimal("30"), Set.of(UUID.randomUUID()));
        givenOpenCart(new int[] { 1 }, first);
        Cart cartTwo = givenOpenCart(new int[] { 1 }, second);

        // The SECOND cart, previewed on its own, must still be "Cart 2". If the
        // subset were renumbered to 1..k the client would show a different label
        // from the cart screen and from what checkout reports back.
        CheckoutSummaryDto summary = summaryOf(List.of(cartTwo.getId()));

        assertEquals("Cart 2", summary.getCarts().get(0).getCartLabel());
    }

    @Test
    @DisplayName("Previewing cart IDs the customer does not own is an error, not a silent empty total")
    void unknownCartIdsAreRejected() {
        // A real, visible cart must exist, otherwise the service would correctly
        // fail earlier with CART_EMPTY and the CART_NOT_FOUND branch — the one
        // under test — would never be reached.
        UUID owned = UUID.randomUUID();
        stubProduct(owned, new BigDecimal("50"), Set.of(vendor1));
        givenOpenCart(new int[] { 1 }, owned);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> summaryOf(List.of(UUID.randomUUID())));

        assertEquals("CART_NOT_FOUND", ex.getErrorCode());
    }
}
