package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.ProductService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Product}.
 */
@RestController
@RequestMapping("/api")
public class ProductResource {

    private final Logger log = LoggerFactory.getLogger(ProductResource.class);

    private static final String ENTITY_NAME = "product";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final ProductService productService;

    private final ProductRepository productRepository;

    public ProductResource(ProductService productService, ProductRepository productRepository) {
        this.productService = productService;
        this.productRepository = productRepository;
    }

    @PostMapping("/products")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @CacheEvict(value = "products", allEntries = true)
    public Product createProduct(@Valid @RequestBody Product product) throws URISyntaxException { // Thay đổi kiểu trả về
        log.debug("REST request to save Product : {}", product);
        if (product.getId() != null) {
            throw new BadRequestAlertException("A new product cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Product result = productService.save(product);
        // Trả về body trực tiếp
        return result;
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @CacheEvict(value = "products", allEntries = true)
    public Product updateProduct( // Thay đổi kiểu trả về
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Product product
    ) {
        log.debug("REST request to update Product : {}, {}", id, product);
        if (product.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, product.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!productRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Product result = productService.update(product);
        // Trả về body trực tiếp
        return result;
    }

    @PatchMapping(value = "/products/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @CacheEvict(value = "products", allEntries = true)
    public Product partialUpdateProduct( // Thay đổi kiểu trả về
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Product product
    ) {
        log.debug("REST request to partial update Product partially : {}, {}", id, product);
        if (product.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, product.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        if (!productRepository.existsById(id)) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }

        Optional<Product> result = productService.partialUpdate(product);

        // Trả về body trực tiếp
        return result.orElse(null);
    }

    @PatchMapping("/products/{id}/toggle-featured")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Product> toggleProductFeatured(@PathVariable Long id) {
        log.debug("REST request to toggle pinned status for Product : {}", id);
        // is_pinned column was removed from the schema. Inform the caller.
        throw new BadRequestAlertException(
            "Pin/unpin feature is not supported by the current database schema. Use sales_count or implement a separate pin store.",
            ENTITY_NAME,
            "notimplemented"
        );
    }

    @GetMapping("/products")
    @Cacheable(value = "products", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort}")
    public List<Product> getAllProducts( // Thay đổi kiểu trả về
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(value = "categorySlug", required = false) String categorySlug,
        @RequestParam(value = "nameContains", required = false) String nameContains,
        @RequestParam Map<String, String> allRequestParams
    ) {
        log.debug("REST request to get a page of Products with filters");
        // Support multiple frontend query param names for backward compatibility:
        // - nameContains (current)
        // - name.contains (used by some frontends)
        // - search (used by navbar)
        String resolvedName = nameContains;
        if ((resolvedName == null || resolvedName.isEmpty()) && allRequestParams != null) {
            String p = allRequestParams.get("name.contains");
            if (p != null && !p.isEmpty()) {
                resolvedName = p;
            }
        }
        if ((resolvedName == null || resolvedName.isEmpty()) && allRequestParams != null) {
            String p2 = allRequestParams.get("search");
            if (p2 != null && !p2.isEmpty()) {
                resolvedName = p2;
            }
        }

        Page<Product> page = productService.findAllWithFilters(pageable, categorySlug, resolvedName);
        // Không cần tạo HttpHeaders ở đây nếu không trả về ResponseEntity
        return page.getContent(); // Chỉ trả về body
    }

    @GetMapping("/products/featured")
    @Cacheable(value = "products", key = "'featured'")
    public List<Product> getFeaturedProducts() { // Thay đổi kiểu trả về
        log.debug("REST request to get all featured Products");
        List<Product> products = productService.findAllFeatured();
        return products; // Chỉ trả về body
    }

    @GetMapping("/products/{id}")
    @Cacheable(value = "products", key = "#id")
    public Product getProduct(@PathVariable Long id) { // Thay đổi kiểu trả về
        log.debug("REST request to get Product : {}", id);
        Optional<Product> product = productService.findOne(id);
        return product.orElse(null); // Chỉ trả về body
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    @CacheEvict(value = "products", allEntries = true)
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.debug("REST request to delete Product : {}", id);

        // Kiểm tra sản phẩm có tồn tại không
        if (!productRepository.existsById(id)) {
            throw new BadRequestAlertException("Sản phẩm không tồn tại", ENTITY_NAME, "idnotfound");
        }

        try {
            productService.delete(id);
            return ResponseEntity.noContent()
                .headers(HeaderUtil.createEntityDeletionAlert(applicationName, true, ENTITY_NAME, id.toString()))
            .build();
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            log.error("Cannot delete product due to foreign key constraint: {}", e.getMessage());
            throw new BadRequestAlertException(
                "Không thể xóa sản phẩm. Sản phẩm đang được sử dụng trong đơn hàng.",
                ENTITY_NAME,
                "productinuse"
            );
        } catch (Exception e) {
            log.error("Unexpected error deleting product: {}", e.getMessage(), e);
            throw new BadRequestAlertException("Lỗi khi xóa sản phẩm: " + e.getMessage(), ENTITY_NAME, "deleteerror");
        }
    }
}
