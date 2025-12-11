package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.service.WishlistService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WishlistResource {

    private final Logger log = LoggerFactory.getLogger(WishlistResource.class);

    private final WishlistService wishlistService;

    public WishlistResource(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    /**
     * {@code GET  /wishlist} : get all the wishlist items for the current user.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of products in body.
     */
    @GetMapping("/wishlist")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<Product>> getCurrentUserWishlist() {
        log.debug("REST request to get current user's wishlist");
        List<Product> products = wishlistService.getWishlistProductsForCurrentUser();
        return ResponseEntity.ok(products);
    }

    /**
     * {@code POST  /wishlist/{productId}} : add a product to the current user's wishlist.
     *
     * @param productId the id of the product to add.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)}.
     */
    @PostMapping("/wishlist/{productId}")
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(value = "com.mycompany.myapp.domain.Product", allEntries = true)
    public ResponseEntity<Void> addToWishlist(@PathVariable Long productId) {
        log.debug("REST request to add product to wishlist : {}", productId);
        wishlistService.addToWishlist(productId);
        return ResponseEntity.ok().build();
    }

    /**
     * {@code DELETE  /wishlist/{productId}} : remove a product from the current user's wishlist.
     *
     * @param productId the id of the product to remove.
     * @return the {@link ResponseEntity} with status {@code 204 (No Content)}.
     */
    @DeleteMapping("/wishlist/{productId}")
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(value = "com.mycompany.myapp.domain.Product", allEntries = true)
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        log.debug("REST request to remove product from wishlist : {}", productId);
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.noContent().build();
    }
}
