package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Cart;
import com.mycompany.myapp.domain.CartItem;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.repository.CartRepository;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    private final Logger log = LoggerFactory.getLogger(CartService.class);

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(
        CartRepository cartRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public Cart getCartForCurrentUser() {
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Current user not found"));
        return cartRepository.findOneByUser_Email(userEmail).orElseGet(() -> createCartForUser(userEmail));
    }

    private Cart createCartForUser(String userEmail) {
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new IllegalStateException("User not found"));
        Cart newCart = new Cart();
        newCart.setUser(user);
        newCart.setCreatedDate(Instant.now());
        newCart.setUpdatedDate(Instant.now());
        newCart.setItems(new HashSet<>());
        return cartRepository.save(newCart);
    }
    
    public void addToCart(Long productId, Integer quantity) {
        Cart cart = getCartForCurrentUser();
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        Optional<CartItem> existingItem = cart.getItems().stream()
            .filter(item -> item.getProduct().getId().equals(productId))
            .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cart.getItems().add(newItem);
        }
        cart.setUpdatedDate(Instant.now());
        cartRepository.save(cart);
    }

    public void updateCartItem(Long productId, Integer quantity) {
        Cart cart = getCartForCurrentUser();
        CartItem item = cart.getItems().stream()
            .filter(i -> i.getProduct().getId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("CartItem not found"));

        item.setQuantity(quantity);
        cart.setUpdatedDate(Instant.now());
        cartRepository.save(cart);
    }

    public void removeFromCart(Long productId) {
        Cart cart = getCartForCurrentUser();
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cart.setUpdatedDate(Instant.now());
        cartRepository.save(cart);
    }

    public void clearCart() {
        Cart cart = getCartForCurrentUser();
        cart.getItems().clear();
        cart.setUpdatedDate(Instant.now());
        cartRepository.save(cart);
    }
}
