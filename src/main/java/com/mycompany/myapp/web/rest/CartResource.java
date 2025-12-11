package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Cart;
import com.mycompany.myapp.domain.CartItem;
import com.mycompany.myapp.service.CartService;
import com.mycompany.myapp.web.rest.dto.CartItemDTO;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class CartResource {

    private final Logger log = LoggerFactory.getLogger(CartResource.class);

    private final CartService cartService;

    public CartResource(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping("/cart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CartItemDTO>> getCart() {
        Cart cart = cartService.getCartForCurrentUser();
        List<CartItemDTO> dtos = cart
            .getItems()
            .stream()
            .sorted(Comparator.comparing(CartItem::getId)) // Sort by CartItem ID to maintain order
            .map(CartItemDTO::new)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/cart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addToCart(@RequestBody CartItemDTO cartItemDTO) {
        cartService.addToCart(cartItemDTO.getProductId(), cartItemDTO.getQuantity());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/cart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> updateCartItem(@RequestBody CartItemDTO cartItemDTO) {
        cartService.updateCartItem(cartItemDTO.getProductId(), cartItemDTO.getQuantity());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/cart/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromCart(@PathVariable Long productId) {
        cartService.removeFromCart(productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/cart")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> clearCart() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}
