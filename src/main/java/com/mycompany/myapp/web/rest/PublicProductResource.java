package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.service.ProductService;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;

/**
 * Public wrapper for products to expose endpoints under /api/public/**
 */
@RestController
@RequestMapping("/api/public")
public class PublicProductResource {

    private final Logger log = LoggerFactory.getLogger(PublicProductResource.class);

    private final ProductService productService;

    public PublicProductResource(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllPublicProducts(
        @org.springdoc.core.annotations.ParameterObject Pageable pageable,
        @RequestParam(value = "categorySlug", required = false) String categorySlug,
        @RequestParam(value = "nameContains", required = false) String nameContains,
        @RequestParam Map<String, String> allRequestParams
    ) {
        log.debug("Public REST request to get a page of Products");
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
        // Call the updated method with null for the new filter parameters
        var page = productService.findAllWithFilters(pageable, categorySlug, resolvedName, null, null, null);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }
}
