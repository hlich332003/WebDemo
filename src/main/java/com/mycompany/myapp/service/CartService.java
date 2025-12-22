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
import org.springframework.cache.annotation.CacheEvict;
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
        String userEmail = getCurrentUserEmail();
        return cartRepository.findOneByUser_Email(userEmail).orElseGet(() -> createCartForUser(userEmail));
    }

    public String getCurrentUserEmail() {
        return SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Current user not found"));
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
    
    @CacheEvict(value = "userCart", key = "#root.target.getCurrentUserEmail()")
    public void addToCart(Long productId, Integer quantity) {
        try {
            log.debug("🛒 Thêm sản phẩm {} vào giỏ hàng với số lượng {}", productId, quantity);
            Cart cart = getCartForCurrentUser();
            Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found: " + productId));

            // Kiểm tra số lượng tồn kho
            if (product.getQuantity() == null || product.getQuantity() < quantity) {
                throw new RuntimeException("Insufficient stock for product: " + productId);
            }

            Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();

            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                int newQuantity = item.getQuantity() + quantity;
                if (product.getQuantity() < newQuantity) {
                    throw new RuntimeException("Insufficient stock for product: " + productId);
                }
                item.setQuantity(newQuantity);
                log.debug("✅ Cập nhật số lượng sản phẩm {} thành {}", productId, newQuantity);
            } else {
                CartItem newItem = new CartItem();
                newItem.setCart(cart);
                newItem.setProduct(product);
                newItem.setQuantity(quantity);
                cart.getItems().add(newItem);
                log.debug("✅ Thêm mới sản phẩm {} vào giỏ hàng", productId);
            }
            cart.setUpdatedDate(Instant.now());
            cartRepository.save(cart);
            log.debug("✅ Lưu giỏ hàng thành công");
        } catch (Exception e) {
            log.error("❌ Lỗi khi thêm sản phẩm {} vào giỏ hàng: {}", productId, e.getMessage(), e);
            throw e;
        }
    }

    @CacheEvict(value = "userCart", key = "#root.target.getCurrentUserEmail()")
    public void updateCartItem(Long productId, Integer quantity) {
        log.debug("✏️ Cập nhật sản phẩm {} trong giỏ hàng: số lượng {}", productId, quantity);
        Cart cart = getCartForCurrentUser();
        CartItem item = cart.getItems().stream()
            .filter(i -> i.getProduct().getId().equals(productId))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("CartItem not found"));

        item.setQuantity(quantity);
        cart.setUpdatedDate(Instant.now());
        cartRepository.save(cart);
    }

    @CacheEvict(value = "userCart", key = "#root.target.getCurrentUserEmail()")
    public void removeFromCart(Long productId) {
        log.debug("🗑️ Xóa sản phẩm {} khỏi giỏ hàng", productId);
        Cart cart = getCartForCurrentUser();
        cart.getItems().removeIf(item -> item.getProduct().getId().equals(productId));
        cart.setUpdatedDate(Instant.now());
        cartRepository.save(cart);
    }

    @CacheEvict(value = "userCart", key = "#root.target.getCurrentUserEmail()")
    public void clearCart() {
        log.debug("🧹 Xóa toàn bộ giỏ hàng");
        Cart cart = getCartForCurrentUser();
        cart.getItems().clear();
        cart.setUpdatedDate(Instant.now());
        cartRepository.save(cart);
    }
}
