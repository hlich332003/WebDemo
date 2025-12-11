package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.User;
import com.mycompany.myapp.domain.WishlistItem;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.UserRepository;
import com.mycompany.myapp.repository.WishlistItemRepository;
import com.mycompany.myapp.security.SecurityUtils;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistService {

    private final Logger log = LoggerFactory.getLogger(WishlistService.class);

    private final WishlistItemRepository wishlistItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public WishlistService(
        WishlistItemRepository wishlistItemRepository,
        UserRepository userRepository,
        ProductRepository productRepository
    ) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> getWishlistProductsForCurrentUser() {
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Current user not found"));
        return wishlistItemRepository.findByUser_Email(userEmail).stream().map(WishlistItem::getProduct).collect(Collectors.toList());
    }

    @CacheEvict(value = { "com.mycompany.myapp.domain.Product.products" }, allEntries = true)
    public void addToWishlist(Long productId) {
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Current user not found"));
        User user = userRepository.findOneByEmailIgnoreCase(userEmail).orElseThrow(() -> new IllegalStateException("User not found"));
        Product product = productRepository.findById(productId).orElseThrow(() -> new RuntimeException("Product not found"));

        if (wishlistItemRepository.findOneByUser_EmailAndProductId(userEmail, productId).isPresent()) {
            log.debug("Product {} is already in wishlist for user {}", productId, userEmail);
            return;
        }

        WishlistItem wishlistItem = new WishlistItem();
        wishlistItem.setUser(user);
        wishlistItem.setProduct(product);
        wishlistItem.setAddedDate(Instant.now());
        wishlistItemRepository.save(wishlistItem);
        log.debug("Added product {} to wishlist for user {}", productId, userEmail);
    }

    @CacheEvict(value = { "com.mycompany.myapp.domain.Product.products" }, allEntries = true)
    public void removeFromWishlist(Long productId) {
        String userEmail = SecurityUtils.getCurrentUserLogin().orElseThrow(() -> new IllegalStateException("Current user not found"));
        wishlistItemRepository.deleteByUser_EmailAndProductId(userEmail, productId);
        log.debug("Removed product {} from wishlist for user {}", productId, userEmail);
    }
}
