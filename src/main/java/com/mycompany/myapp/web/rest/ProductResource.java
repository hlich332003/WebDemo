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
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
// import org.springframework.cache.annotation.CacheEvict; // Tạm thời vô hiệu hóa
// import org.springframework.cache.annotation.Cacheable; // Tạm thời vô hiệu hóa
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
    // @CacheEvict(value = "products", allEntries = true) // Tạm thời vô hiệu hóa
    public ResponseEntity<Product> createProduct(@Valid @RequestBody Product product) throws URISyntaxException {
        log.debug("REST request to save Product : {}", product);
        if (product.getId() != null) {
            throw new BadRequestAlertException("A new product cannot already have an ID", ENTITY_NAME, "idexists");
        }
        Product result = productService.save(product);
        return ResponseEntity.created(new URI("/api/products/" + result.getId()))
            .headers(HeaderUtil.createEntityCreationAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    // @CacheEvict(value = "products", allEntries = true) // Tạm thời vô hiệu hóa
    public ResponseEntity<Product> updateProduct(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Product product
    ) throws URISyntaxException {
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
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, product.getId().toString()))
            .body(result);
    }

    @PatchMapping(value = "/products/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    // @CacheEvict(value = "products", allEntries = true) // Tạm thời vô hiệu hóa
    public ResponseEntity<Product> partialUpdateProduct(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Product product
    ) throws URISyntaxException {
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

        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, product.getId().toString())
        );
    }

    @PatchMapping("/products/{id}/toggle-featured")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    // @CacheEvict(value = "products", allEntries = true) // Tạm thời vô hiệu hóa
    public ResponseEntity<Product> toggleProductFeatured(@PathVariable Long id) {
        log.debug("REST request to toggle featured status for Product : {}", id);
        Optional<Product> existingProduct = productService.findOne(id);
        if (existingProduct.isEmpty()) {
            throw new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound");
        }
        Product product = existingProduct.get();
        product.setIsFeatured(!product.getIsFeatured());
        Product result = productService.update(product);
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, result.getId().toString()))
            .body(result);
    }

    @GetMapping("/products")
    // @Cacheable(value = "products", key = "{#pageable.pageNumber, #pageable.pageSize, #pageable.sort}") // Tạm thời vô hiệu hóa
    public ResponseEntity<List<Product>> getAllProducts(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(value = "categorySlug", required = false) String categorySlug,
        @RequestParam(value = "nameContains", required = false) String nameContains
    ) {
        log.debug("REST request to get a page of Products with filters");
        Page<Product> page = productService.findAllWithFilters(pageable, categorySlug, nameContains);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/products/featured")
    // @Cacheable(value = "products", key = "'featured'") // Tạm thời vô hiệu hóa
    public ResponseEntity<List<Product>> getFeaturedProducts() {
        log.debug("REST request to get all featured Products");
        List<Product> products = productService.findAllFeatured();
        return ResponseEntity.ok().body(products);
    }

    @GetMapping("/products/{id}")
    // @Cacheable(value = "products", key = "#id") // Tạm thời vô hiệu hóa
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        log.debug("REST request to get Product : {}", id);
        Optional<Product> product = productService.findOne(id);
        return ResponseUtil.wrapOrNotFound(product);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    // @CacheEvict(value = "products", allEntries = true) // Tạm thời vô hiệu hóa
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
