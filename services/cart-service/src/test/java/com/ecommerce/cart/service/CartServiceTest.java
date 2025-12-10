package com.ecommerce.cart.service;

import com.ecommerce.cart.client.CatalogServiceClient;
import com.ecommerce.cart.client.PricingServiceClient;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.external.ProductDTO;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.ProductNotFoundException;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CartService
 */
@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private CatalogServiceClient catalogClient;

    @Mock
    private PricingServiceClient pricingClient;

    @InjectMocks
    private CartService cartService;

    private Cart testCart;
    private ProductDTO testProduct;

    @BeforeEach
    void setUp() {
        // Set the maxItemsPerCart field using reflection
        ReflectionTestUtils.setField(cartService, "maxItemsPerCart", 50);

        testCart = Cart.builder()
                .id(UUID.randomUUID())
                .userId("user-123")
                .items(new ArrayList<>())
                .build();

        testProduct = ProductDTO.builder()
                .id("PROD-001")
                .name("Test Product")
                .description("Test Description")
                .basePrice(new BigDecimal("99.99"))
                .available(true)
                .build();
    }

    @Test
    void addItem_shouldCreateNewCartItem_whenProductExists() {
        // Given
        String productId = "PROD-001";
        Integer quantity = 2;

        when(cartRepository.findByUserIdWithItems("user-123"))
                .thenReturn(Optional.of(testCart));
        when(catalogClient.getProductBlocking(productId))
                .thenReturn(testProduct);
        when(pricingClient.getFinalPriceBlocking(productId))
                .thenReturn(new BigDecimal("89.99"));
        when(cartItemRepository.findByCartIdAndProductId(any(), any()))
                .thenReturn(Optional.empty());
        when(cartItemRepository.save(any(CartItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(cartRepository.save(any(Cart.class)))
                .thenReturn(testCart);

        // Mocks for enrichment
        when(catalogClient.getProduct(productId))
                .thenReturn(Mono.just(testProduct));
        when(pricingClient.getFinalPrice(productId))
                .thenReturn(Mono.just(new BigDecimal("89.99")));

        // When
        CartResponse response = cartService.addItem("user-123", null, productId, quantity);

        // Then
        verify(cartItemRepository).save(any(CartItem.class));
        verify(cartRepository).save(testCart);
    }

    @Test
    void addItem_shouldThrowException_whenProductNotFound() {
        // Given
//        when(cartRepository.findByUserIdWithItems("user-123"))
//                .thenReturn(Optional.of(testCart));
        when(catalogClient.getProductBlocking("INVALID"))
                .thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> cartService.addItem("user-123", null, "INVALID", 1))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found");
    }

    @Test
    void addItem_shouldIncrementQuantity_whenProductAlreadyInCart() {
        // Given
        CartItem existingItem = CartItem.builder()
                .id(UUID.randomUUID())
                .cart(testCart)
                .productId("PROD-001")
                .quantity(1)
                .cachedPrice(new BigDecimal("89.99"))
                .build();

        // Add existing item to cart so enrichment works
        testCart.getItems().add(existingItem);

        when(cartRepository.findByUserIdWithItems("user-123"))
                .thenReturn(Optional.of(testCart));
        when(catalogClient.getProductBlocking("PROD-001"))
                .thenReturn(testProduct);
        when(pricingClient.getFinalPriceBlocking("PROD-001"))
                .thenReturn(new BigDecimal("89.99"));
        when(cartItemRepository.findByCartIdAndProductId(any(), any()))
                .thenReturn(Optional.of(existingItem));
        when(cartItemRepository.save(any(CartItem.class)))
                .thenReturn(existingItem);
        when(cartRepository.save(any(Cart.class)))
                .thenReturn(testCart);

        // Mocks for enrichment - needed because enrichCart is called
        when(catalogClient.getProduct("PROD-001"))
                .thenReturn(Mono.just(testProduct));
        when(pricingClient.getFinalPrice("PROD-001"))
                .thenReturn(Mono.just(new BigDecimal("89.99")));

        // When
        cartService.addItem("user-123", null, "PROD-001", 2);

        // Then
        assertThat(existingItem.getQuantity()).isEqualTo(3);
        verify(cartItemRepository).save(existingItem);
    }

    @Test
    void clearCart_shouldRemoveAllItems() {
        // Given
        when(cartRepository.findByUserIdWithItems("user-123"))
                .thenReturn(Optional.of(testCart));

        // When
        cartService.clearCart("user-123", null);

        // Then
        verify(cartItemRepository).deleteByCartId(testCart.getId());
        verify(cartRepository).save(testCart);
    }
}

