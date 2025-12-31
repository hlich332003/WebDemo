package com.mycompany.myapp.service.scheduler;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.domain.WishlistItem;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.repository.WishlistItemRepository;
import com.mycompany.myapp.service.NotificationService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Scheduled jobs để kiểm tra và gửi thông báo tự động
 */
@Service
@Transactional
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final NotificationService notificationService;
    private final ProductRepository productRepository;
    private final WishlistItemRepository wishlistItemRepository;

    public NotificationScheduler(
        NotificationService notificationService,
        ProductRepository productRepository,
        WishlistItemRepository wishlistItemRepository
    ) {
        this.notificationService = notificationService;
        this.productRepository = productRepository;
        this.wishlistItemRepository = wishlistItemRepository;
    }

    /**
     * Kiểm tra sản phẩm low stock mỗi 1 giờ
     * Chạy vào phút 0 mỗi giờ
     */
    @Scheduled(cron = "0 0 * * * *")
    public void checkLowStockProducts() {
        log.info("🔍 Starting low stock check...");
        try {
            // Tìm tất cả sản phẩm có stock < 10
            List<Product> lowStockProducts = productRepository.findByQuantityLessThan(10);

            for (Product product : lowStockProducts) {
                if (product.getQuantity() == 0) {
                    // Hết hàng hoàn toàn
                    notificationService.notifyAdminOutOfStock(product.getId(), product.getName());
                } else {
                    // Sắp hết hàng
                    notificationService.notifyAdminLowStock(product.getId(), product.getName(), product.getQuantity());
                }
            }

            log.info("✅ Low stock check completed. Found {} products with low stock.", lowStockProducts.size());
        } catch (Exception e) {
            log.error("❌ Error during low stock check", e);
        }
    }

    /**
     * Kiểm tra wishlist low stock mỗi ngày lúc 9:00 AM
     */
    @Scheduled(cron = "0 0 9 * * *")
    public void checkWishlistLowStock() {
        log.info("🔍 Starting wishlist low stock check...");
        try {
            // Lấy tất cả wishlist items
            List<WishlistItem> wishlistItems = wishlistItemRepository.findAll();

            int notificationCount = 0;
            for (WishlistItem item : wishlistItems) {
                Product product = item.getProduct();
                if (product != null && product.getQuantity() != null && product.getQuantity() < 5) {
                    // Sản phẩm trong wishlist còn ít hơn 5
                    notificationService.notifyUserWishlistLowStock(
                        item.getUser().getId(),
                        product.getId(),
                        product.getName(),
                        product.getQuantity()
                    );
                    notificationCount++;
                }
            }

            log.info("✅ Wishlist low stock check completed. Sent {} notifications.", notificationCount);
        } catch (Exception e) {
            log.error("❌ Error during wishlist low stock check", e);
        }
    }

    /**
     * Kiểm tra giá wishlist giảm mỗi ngày lúc 10:00 AM
     * Note: Cần có bảng lưu lịch sử giá để so sánh
     * Hiện tại chỉ là placeholder
     */
    @Scheduled(cron = "0 0 10 * * *")
    public void checkWishlistPriceChanges() {
        log.info("🔍 Starting wishlist price change check...");
        try {
            // TODO: Implement price tracking logic
            // Cần có bảng price_history để lưu lịch sử giá
            // So sánh giá hiện tại với giá ngày hôm trước
            // Nếu giá giảm, gửi notification

            log.info("⚠️ Wishlist price change check not implemented yet. Need price_history table.");
        } catch (Exception e) {
            log.error("❌ Error during wishlist price change check", e);
        }
    }
}
