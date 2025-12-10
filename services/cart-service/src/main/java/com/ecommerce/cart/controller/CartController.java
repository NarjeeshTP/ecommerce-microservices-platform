package com.ecommerce.cart.controller;

import com.ecommerce.cart.dto.AddItemRequest;
import com.ecommerce.cart.dto.CartResponse;
import com.ecommerce.cart.dto.UpdateItemRequest;
import com.ecommerce.cart.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Cart", description = "Shopping cart management APIs")
public class CartController {

    private final CartService cartService;

    @GetMapping
    @Operation(summary = "Get cart", description = "Retrieve cart with current prices for user or session")
    public ResponseEntity<CartResponse> getCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        log.info("GET /api/cart - userId={}, sessionId={}", userId, sessionId);
        CartResponse cart = cartService.getCart(userId, sessionId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Add a product to the shopping cart")
    public ResponseEntity<CartResponse> addItem(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId,
            @Valid @RequestBody AddItemRequest request) {

        log.info("POST /api/cart/items - userId={}, sessionId={}, productId={}, quantity={}",
                userId, sessionId, request.getProductId(), request.getQuantity());

        CartResponse cart = cartService.addItem(
                userId,
                sessionId,
                request.getProductId(),
                request.getQuantity()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(cart);
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update item quantity", description = "Update the quantity of an item in the cart")
    public ResponseEntity<CartResponse> updateItem(
            @Parameter(description = "Cart item ID") @PathVariable UUID itemId,
            @Valid @RequestBody UpdateItemRequest request) {

        log.info("PUT /api/cart/items/{} - quantity={}", itemId, request.getQuantity());
        CartResponse cart = cartService.updateItemQuantity(itemId, request.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item", description = "Remove an item from the cart")
    public ResponseEntity<CartResponse> removeItem(
            @Parameter(description = "Cart item ID") @PathVariable UUID itemId) {

        log.info("DELETE /api/cart/items/{}", itemId);
        CartResponse cart = cartService.removeItem(itemId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "Clear cart", description = "Remove all items from the cart")
    public ResponseEntity<Void> clearCart(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        log.info("DELETE /api/cart/clear - userId={}, sessionId={}", userId, sessionId);
        cartService.clearCart(userId, sessionId);
        return ResponseEntity.noContent().build();
    }
}

