package com.ecommerce.cart.service;

import com.ecommerce.cart.client.CatalogServiceClient;
import com.ecommerce.cart.client.PricingServiceClient;
import com.ecommerce.cart.dto.CartItemResponse;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.external.ProductDTO;
import com.ecommerce.cart.entity.Cart;
import com.ecommerce.cart.entity.CartItem;
import com.ecommerce.cart.exception.CartItemNotFoundException;
import com.ecommerce.cart.exception.CartLimitExceededException;
import com.ecommerce.cart.exception.CartNotFoundException;
import com.ecommerce.cart.exception.ProductNotFoundException;
import com.ecommerce.cart.repository.CartItemRepository;
import com.ecommerce.cart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final CatalogServiceClient catalogClient;
    private final PricingServiceClient pricingClient;

    @Value("${cart.max-items-per-cart:50}")
    private int maxItemsPerCart;

    @Transactional
    public Cart getOrCreateCart(String userId, String sessionId) {
        log.debug("Getting or creating cart for userId={}, sessionId={}", userId, sessionId);

        if (userId != null) {
            return cartRepository.findByUserIdWithItems(userId)
                    .orElseGet(() -> createCart(userId, null));
        } else if (sessionId != null) {
            return cartRepository.findBySessionIdWithItems(sessionId)
                    .orElseGet(() -> createCart(null, sessionId));
        } else {
            throw new IllegalArgumentException("Either userId or sessionId must be provided");
        }
    }

    private Cart createCart(String userId, String sessionId) {
        Cart cart = Cart.builder()
                .userId(userId)
                .sessionId(sessionId)
                .build();
        Cart savedCart = cartRepository.save(cart);
        log.info("Created new cart: {}", savedCart.getId());
        return savedCart;
    }

    @Transactional(readOnly = true)
    public CartResponse getCartById(UUID cartId) {
        log.debug("Fetching cart: {}", cartId);

        Cart cart = cartRepository.findByIdWithItems(cartId)
                .orElseThrow(() -> new CartNotFoundException("Cart not found: " + cartId));

        return enrichCart(cart);
    }

    @Transactional(readOnly = true)
    public CartResponse getCart(String userId, String sessionId) {
        Cart cart = getOrCreateCart(userId, sessionId);
        return enrichCart(cart);
    }

    @Transactional
    public CartResponse addItem(String userId, String sessionId, String productId, Integer quantity) {
        log.info("Adding item to cart: productId={}, quantity={}", productId, quantity);

        ProductDTO product = catalogClient.getProductBlocking(productId);
        if (product == null) {
            throw new ProductNotFoundException("Product not found: " + productId);
        }

        Cart cart = getOrCreateCart(userId, sessionId);

        if (cart.getTotalItems() + quantity > maxItemsPerCart) {
            throw new CartLimitExceededException(
                    "Cart limit exceeded. Maximum " + maxItemsPerCart + " items allowed");
        }

        BigDecimal currentPrice = null;
        try {
            currentPrice = pricingClient.getFinalPriceBlocking(productId);
        } catch (Exception e) {
            log.warn("Could not fetch price for product {}, using base price", productId);
            currentPrice = product.getBasePrice();
        }

        CartItem existingItem = cartItemRepository
                .findByCartIdAndProductId(cart.getId(), productId)
                .orElse(null);

        if (existingItem != null) {
            existingItem.setQuantity(existingItem.getQuantity() + quantity);
            existingItem.setCachedPrice(currentPrice);
            cartItemRepository.save(existingItem);
            log.info("Updated cart item quantity: {}", existingItem.getId());
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .productId(productId)
                    .quantity(quantity)
                    .cachedPrice(currentPrice)
                    .build();
            cartItemRepository.save(newItem);
            cart.getItems().add(newItem);
            log.info("Added new item to cart: {}", newItem.getId());
        }

        cartRepository.save(cart);
        return enrichCart(cart);
    }

    @Transactional
    public CartResponse updateItemQuantity(UUID itemId, Integer newQuantity) {
        log.info("Updating cart item {} to quantity {}", itemId, newQuantity);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));

        item.setQuantity(newQuantity);
        cartItemRepository.save(item);

        Cart cart = item.getCart();
        return enrichCart(cart);
    }

    @Transactional
    public CartResponse removeItem(UUID itemId) {
        log.info("Removing cart item: {}", itemId);

        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new CartItemNotFoundException("Cart item not found: " + itemId));

        Cart cart = item.getCart();
        cartItemRepository.delete(item);
        cart.getItems().remove(item);

        return enrichCart(cart);
    }

    @Transactional
    public void clearCart(String userId, String sessionId) {
        log.info("Clearing cart for userId={}, sessionId={}", userId, sessionId);

        Cart cart = getOrCreateCart(userId, sessionId);
        cartItemRepository.deleteByCartId(cart.getId());
        cart.clear();
        cartRepository.save(cart);
    }

    private CartResponse enrichCart(Cart cart) {
        if (cart.getItems().isEmpty()) {
            return buildEmptyCartResponse(cart);
        }

        List<CartItemResponse> enrichedItems = cart.getItems().stream()
                .map(this::enrichCartItem)
                .collect(Collectors.toList());

        BigDecimal totalPrice = enrichedItems.stream()
                .map(CartItemResponse::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .sessionId(cart.getSessionId())
                .items(enrichedItems)
                .totalItems(cart.getTotalItems())
                .totalPrice(totalPrice)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }

    private CartItemResponse enrichCartItem(CartItem item) {
        String productId = item.getProductId();

        try {
            ProductDTO product = catalogClient.getProduct(productId).block();
            BigDecimal currentPrice = pricingClient.getFinalPrice(productId).block();

            if (currentPrice == null) {
                currentPrice = item.getCachedPrice();
            }

            return CartItemResponse.builder()
                    .id(item.getId())
                    .productId(productId)
                    .productName(product != null ? product.getName() : "Unknown Product")
                    .productDescription(product != null ? product.getDescription() : null)
                    .quantity(item.getQuantity())
                    .unitPrice(currentPrice)
                    .totalPrice(currentPrice != null ?
                            currentPrice.multiply(BigDecimal.valueOf(item.getQuantity())) : null)
                    .addedAt(item.getAddedAt())
                    .updatedAt(item.getUpdatedAt())
                    .priceAvailable(currentPrice != null)
                    .build();
        } catch (Exception e) {
            log.error("Error enriching cart item {}", item.getId(), e);
            return CartItemResponse.builder()
                    .id(item.getId())
                    .productId(productId)
                    .productName("Product Info Unavailable")
                    .quantity(item.getQuantity())
                    .unitPrice(item.getCachedPrice())
                    .totalPrice(item.getCachedPrice() != null ?
                            item.getCachedPrice().multiply(BigDecimal.valueOf(item.getQuantity())) : null)
                    .addedAt(item.getAddedAt())
                    .updatedAt(item.getUpdatedAt())
                    .priceAvailable(false)
                    .build();
        }
    }

    private CartResponse buildEmptyCartResponse(Cart cart) {
        return CartResponse.builder()
                .id(cart.getId())
                .userId(cart.getUserId())
                .sessionId(cart.getSessionId())
                .items(List.of())
                .totalItems(0)
                .totalPrice(BigDecimal.ZERO)
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .build();
    }
}