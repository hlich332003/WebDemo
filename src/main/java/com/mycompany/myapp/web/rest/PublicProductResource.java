package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.service.ProductService;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tech.jhipster.web.util.PaginationUtil;
import tech.jhipster.web.util.ResponseUtil;

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
        @RequestParam(value = "name", required = false) String name,
        @RequestParam(value = "minPrice", required = false) BigDecimal minPrice,
        @RequestParam(value = "maxPrice", required = false) BigDecimal maxPrice,
        @RequestParam(value = "inStock", required = false) Boolean inStock,
        @RequestParam(value = "isPinned", required = false) Boolean isPinned
    ) {
        log.debug("Public REST request to get a page of Products with filters");
        // Public endpoint always shows only active products
        var page = productService.findAllWithFilters(pageable, categorySlug, name, minPrice, maxPrice, inStock, true, isPinned);
        HttpHeaders headers = PaginationUtil.generatePaginationHttpHeaders(ServletUriComponentsBuilder.fromCurrentRequest(), page);
        return ResponseEntity.ok().headers(headers).body(page.getContent());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> getProduct(@PathVariable Long id) {
        log.debug("Public REST request to get Product : {}", id);
        Optional<Product> product = productService.findOne(id);
        return ResponseUtil.wrapOrNotFound(product);
    }

    @GetMapping("/image-proxy")
    public ResponseEntity<StreamingResponseBody> proxyImage(@RequestParam("url") String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            URLConnection connection = url.openConnection();
            InputStream inputStream = connection.getInputStream();

            StreamingResponseBody responseBody = outputStream -> {
                try {
                    inputStream.transferTo(outputStream);
                } finally {
                    inputStream.close();
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_TYPE, connection.getContentType());
            headers.add(HttpHeaders.CONTENT_LENGTH, String.valueOf(connection.getContentLength()));

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
        } catch (IOException e) {
            log.error("Failed to proxy image: {}", imageUrl, e);
            try {
                ClassPathResource defaultImage = new ClassPathResource("static/content/images/default-product.svg");
                InputStream inputStream = defaultImage.getInputStream();
                StreamingResponseBody responseBody = outputStream -> {
                    try {
                        inputStream.transferTo(outputStream);
                    } finally {
                        inputStream.close();
                    }
                };
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.valueOf("image/svg+xml"));
                return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);
            } catch (IOException ex) {
                log.error("Failed to load default image", ex);
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
    }
}
