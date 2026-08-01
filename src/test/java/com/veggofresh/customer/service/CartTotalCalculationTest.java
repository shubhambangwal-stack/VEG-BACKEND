package com.veggofresh.customer.service;

import com.veggofresh.customer.dto.response.CartResponseDto;
import com.veggofresh.customer.entity.Cart;
import com.veggofresh.customer.entity.CartItem;
import com.veggofresh.customer.repository.CartItemRepository;
import com.veggofresh.customer.repository.CartRepository;
import com.veggofresh.customer.service.impl.CartServiceImpl;
import com.veggofresh.vendor.dto.ProductDto;
import com.veggofresh.vendor.service.ProductCatalogService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class CartTotalCalculationTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductCatalogService productCatalogService;

    @InjectMocks
    private CartServiceImpl cartService;

    private UUID userId;
    private Cart cart;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        cart = new Cart();
        cart.setId(UUID.randomUUID());
        cart.setUserId(userId);
        cart.setItems(new ArrayList<>());
    }

    @Test
    void getOrCreateCart_CalculatesCorrectTotalForMultipleItems() {
        // Arrange
        UUID prod1Id = UUID.randomUUID();
        UUID prod2Id = UUID.randomUUID();

        CartItem item1 = new CartItem();
        item1.setCart(cart);
        item1.setProductId(prod1Id);
        item1.setQuantity(2);

        CartItem item2 = new CartItem();
        item2.setCart(cart);
        item2.setProductId(prod2Id);
        item2.setQuantity(3);

        cart.getItems().add(item1);
        cart.getItems().add(item2);

        ProductDto prod1 = ProductDto.builder().id(prod1Id).name("Apple").price(BigDecimal.valueOf(10.00)).build();
        ProductDto prod2 = ProductDto.builder().id(prod2Id).name("Banana").price(BigDecimal.valueOf(5.50)).build();

        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogService.getProductById(prod1Id)).thenReturn(prod1);
        when(productCatalogService.getProductById(prod2Id)).thenReturn(prod2);

        // Act
        CartResponseDto result = cartService.getOrCreateCart(userId);

        // Assert
        BigDecimal expectedTotal = BigDecimal.valueOf(10.00).multiply(BigDecimal.valueOf(2))
                .add(BigDecimal.valueOf(5.50).multiply(BigDecimal.valueOf(3))); // 20.00 + 16.50 = 36.50
        assertEquals(expectedTotal, result.getTotalAmount());
        assertEquals(2, result.getItems().size());
    }
}
