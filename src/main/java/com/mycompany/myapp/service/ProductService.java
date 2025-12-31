package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Category;
import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.repository.CategoryRepository;
import com.mycompany.myapp.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class ProductService {

    private final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final NotificationService notificationService;

    public ProductService(
        ProductRepository productRepository,
        CategoryRepository categoryRepository,
        NotificationService notificationService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.notificationService = notificationService;
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product save(Product product) {
        log.debug("Request to save Product : {}", product);

        // If category only has ID, fetch the full category from database
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Long categoryId = product.getCategory().getId();
            Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));
            product.setCategory(category);
        }

        // Process data URL if present
        processImageDataUrl(product);

        Product savedProduct = productRepository.save(product);

        // Kiểm tra stock level và gửi notification
        checkStockLevel(savedProduct);

        return savedProduct;
    }

    @CacheEvict(value = "products", allEntries = true)
    public Product update(Product product) {
        log.debug("Request to update Product : {}", product);

        // If category only has ID, fetch the full category from database
        if (product.getCategory() != null && product.getCategory().getId() != null) {
            Long categoryId = product.getCategory().getId();
            Category category = categoryRepository
                .findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Category not found with id: " + categoryId));
            product.setCategory(category);
        }

        // Process data URL if present
        processImageDataUrl(product);

        Product updatedProduct = productRepository.save(product);

        // Kiểm tra stock level và gửi notification
        checkStockLevel(updatedProduct);

        return updatedProduct;
    }

    /**
     * Process data URL and extract binary data
     * Convert data:image/jpeg;base64,... to byte[] and store in imageData field
     */
    private void processImageDataUrl(Product product) {
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && imageUrl.startsWith("data:image/")) {
            try {
                // Extract content type and base64 data
                // Format: data:image/jpeg;base64,/9j/4AAQ...
                String[] parts = imageUrl.split(",");
                if (parts.length == 2) {
                    String meta = parts[0]; // data:image/jpeg;base64
                    String base64Data = parts[1];

                    // Extract content type
                    String contentType = meta.substring(5, meta.indexOf(";"));

                    // Decode base64 to byte array
                    byte[] imageData = Base64.getDecoder().decode(base64Data);

                    // Store in database fields
                    product.setImageData(imageData);
                    product.setImageContentType(contentType);

                    // Keep the data URL in imageUrl field for backward compatibility
                    // Or you can set it to null if you want to only use imageData
                    // product.setImageUrl(null);

                    log.debug("Processed image data URL, size: {} bytes, type: {}", imageData.length, contentType);
                }
            } catch (Exception e) {
                log.error("Error processing image data URL", e);
            }
        }
    }

    /**
     * Kiểm tra mức tồn kho và gửi notification nếu cần
     */
    private void checkStockLevel(Product product) {
        if (product.getQuantity() == null) {
            return;
        }

        int quantity = product.getQuantity();
        String productName = product.getName();
        Long productId = product.getId();

        if (quantity == 0) {
            // Hết hàng hoàn toàn
            log.warn("⚠️ Sản phẩm {} đã hết hàng!", productName);
            notificationService.notifyAdminOutOfStock(productId, productName);
        } else if (quantity < 10) {
            // Sắp hết hàng (dưới 10)
            log.warn("⚠️ Sản phẩm {} chỉ còn {} sản phẩm!", productName, quantity);
            notificationService.notifyAdminLowStock(productId, productName, quantity);
        }
    }

    public Optional<Product> partialUpdate(Product product) {
        log.debug("Request to partially update Product : {}", product);
        return productRepository
            .findById(product.getId())
            .map(existingProduct -> {
                if (product.getName() != null) {
                    existingProduct.setName(product.getName());
                }
                if (product.getDescription() != null) {
                    existingProduct.setDescription(product.getDescription());
                }
                if (product.getPrice() != null) {
                    existingProduct.setPrice(product.getPrice());
                }
                if (product.getQuantity() != null) {
                    existingProduct.setQuantity(product.getQuantity());
                }
                if (product.getImageUrl() != null) {
                    existingProduct.setImageUrl(product.getImageUrl());
                }
                if (product.getCategory() != null) {
                    existingProduct.setCategory(product.getCategory());
                }
                return existingProduct;
            })
            .map(productRepository::save);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all_page_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<Product> findAll(Pageable pageable) {
        log.debug("Request to get all Products");
        return productRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "categories", key = "'all'")
    public List<Category> getCategories() {
        return categoryRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Product> findAllWithFilters(
        Pageable pageable,
        String categorySlug,
        String nameContains,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Boolean inStock,
        Boolean isActive,
        Boolean isPinned
    ) {
        log.debug("Request to get a page of Products with dynamic filters");

        Specification<Product> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(categorySlug)) {
                predicates.add(criteriaBuilder.equal(root.get("category").get("slug"), categorySlug));
            }
            if (StringUtils.hasText(nameContains)) {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), "%" + nameContains.toLowerCase() + "%"));
            }
            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("price"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("price"), maxPrice));
            }
            if (inStock != null) {
                if (inStock) {
                    predicates.add(criteriaBuilder.greaterThan(root.get("quantity"), 0));
                } else {
                    predicates.add(criteriaBuilder.equal(root.get("quantity"), 0));
                }
            }
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }
            if (isPinned != null) {
                predicates.add(criteriaBuilder.equal(root.get("isPinned"), isPinned));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'id_' + #id")
    public Optional<Product> findOne(Long id) {
        log.debug("Request to get Product : {}", id);
        return productRepository.findById(id);
    }

    @CacheEvict(value = { "products", "categories" }, allEntries = true)
    public void delete(Long id) {
        log.debug("Request to delete Product : {}", id);
        productRepository.deleteById(id);
    }

    public Optional<Product> toggleFeatured(Long id) {
        log.debug("Request to toggle featured status for Product : {}", id);
        return productRepository
            .findById(id)
            .map(product -> {
                product.setIsPinned(!product.getIsPinned());
                return product;
            })
            .map(productRepository::save);
    }
}
