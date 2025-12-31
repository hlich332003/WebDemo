package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.repository.ProductRepository;
import com.mycompany.myapp.security.AuthoritiesConstants;
import com.mycompany.myapp.service.ProductService;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

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
    public ResponseEntity<Product> updateProduct(
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
        return ResponseEntity.ok()
            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, product.getId().toString()))
            .body(result);
    }

    @PatchMapping(value = "/products/{id}", consumes = { "application/json", "application/merge-patch+json" })
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Product> partialUpdateProduct(
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
        return ResponseUtil.wrapOrNotFound(
            result,
            HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, product.getId().toString())
        );
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(required = false) String categorySlug,
        @RequestParam(required = false) String nameContains,
        @RequestParam(required = false) BigDecimal minPrice,
        @RequestParam(required = false) BigDecimal maxPrice,
        @RequestParam(required = false) Boolean inStock,
        @RequestParam(required = false) Boolean isActive,
        @RequestParam(required = false) Boolean isPinned,
        @RequestParam Map<String, String> allRequestParams
    ) {
        log.debug("REST request to get a page of Products with filters: {}", allRequestParams);
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

        // Handle isPinned from query params
        if (isPinned == null && allRequestParams != null) {
            String pinnedParam = allRequestParams.get("isPinned.equals");
            if (pinnedParam != null) {
                isPinned = Boolean.parseBoolean(pinnedParam);
            }
        }

        Page<Product> page = productService.findAllWithFilters(
            pageable,
            categorySlug,
            resolvedName,
            minPrice,
            maxPrice,
            inStock,
            isActive,
            isPinned
        );
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        log.debug("REST request to get Product : {}", id);
        Optional<Product> product = productService.findOne(id);
        return ResponseUtil.wrapOrNotFound(product);
    }

    /**
     * GET  /products/:id/image : get product image from database
     */
    @GetMapping("/products/{id}/image")
    public ResponseEntity<byte[]> getProductImage(@PathVariable Long id) {
        log.debug("REST request to get Product image : {}", id);
        Optional<Product> productOpt = productService.findOne(id);

        if (productOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Product product = productOpt.get();
        byte[] imageData = product.getImageData();
        String contentType = product.getImageContentType();

        if (imageData == null || imageData.length == 0) {
            return ResponseEntity.notFound().build();
        }

        HttpHeaders headers = new HttpHeaders();
        if (contentType != null) {
            headers.setContentType(MediaType.parseMediaType(contentType));
        } else {
            headers.setContentType(MediaType.IMAGE_JPEG); // Default
        }
        headers.setContentLength(imageData.length);
        headers.setCacheControl("max-age=31536000"); // Cache for 1 year

        return new ResponseEntity<>(imageData, headers, HttpStatus.OK);
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long id) {
        log.debug("REST request to delete Product : {}", id);
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

    @PatchMapping("/products/{id}/toggle-featured")
    @PreAuthorize("hasAuthority(\"" + AuthoritiesConstants.ADMIN + "\")")
    public ResponseEntity<Product> toggleFeatured(@PathVariable Long id) {
        log.debug("REST request to toggle featured status for Product : {}", id);
        Optional<Product> result = productService.toggleFeatured(id);
        return ResponseUtil.wrapOrNotFound(result, HeaderUtil.createEntityUpdateAlert(applicationName, true, ENTITY_NAME, id.toString()));
    }
}
