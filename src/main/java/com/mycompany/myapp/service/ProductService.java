package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Product;
import com.mycompany.myapp.repository.ProductRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service Implementation for managing {@link Product}.
 */
@Service
@Transactional
public class ProductService {

    private final Logger log = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * Save a product.
     *
     * @param product the entity to save.
     * @return the persisted entity.
     */
    @CacheEvict(value = { "products" }, allEntries = true)
    public Product save(Product product) {
        log.debug("Request to save Product : {}", product);
        return productRepository.save(product);
    }

    /**
     * Update a product.
     *
     * @param product the entity to save.
     * @return the persisted entity.
     */
    @CacheEvict(value = { "products" }, allEntries = true)
    public Product update(Product product) {
        log.debug("Request to update Product : {}", product);
        return productRepository.save(product);
    }

    /**
     * Partially update a product.
     *
     * @param product the entity to update partially.
     * @return the persisted entity.
     */
    @CacheEvict(value = { "products" }, allEntries = true)
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

    /**
     * Get all the products.
     *
     * @param pageable the pagination information.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable) {
        log.debug("Request to get all Products");
        return productRepository.findAll(pageable);
    }

    /**
     * Get all the products with filters.
     *
     * @param pageable the pagination information.
     * @param categorySlug the slug of the category to filter by.
     * @param nameContains the name to filter by.
     * @return the list of entities.
     */
    @Transactional(readOnly = true)
    public Page<Product> findAllWithFilters(Pageable pageable, String categorySlug, String nameContains) {
        log.debug("Request to get a page of Products with filters - categorySlug: {}, nameContains: {}", categorySlug, nameContains);
        if (categorySlug != null && !categorySlug.isEmpty() && nameContains != null && !nameContains.isEmpty()) {
            return productRepository.findByCategory_SlugAndNameContainingIgnoreCase(pageable, categorySlug, nameContains);
        } else if (categorySlug != null && !categorySlug.isEmpty()) {
            return productRepository.findByCategory_Slug(pageable, categorySlug);
        } else if (nameContains != null && !nameContains.isEmpty()) {
            return productRepository.findByNameContainingIgnoreCase(pageable, nameContains);
        } else {
            return productRepository.findAll(pageable);
        }
    }

    /**
     * Get one product by id.
     *
     * @param id the id of the entity.
     * @return the entity.
     */
    @Transactional(readOnly = true)
    public Optional<Product> findOne(Long id) {
        log.debug("Request to get Product : {}", id);
        return productRepository.findById(id);
    }

    /**
     * Delete the product by id.
     *
     * @param id the id of the entity.
     */
    @CacheEvict(value = { "products" }, allEntries = true)
    public void delete(Long id) {
        log.debug("Request to delete Product : {}", id);
        productRepository.deleteById(id);
    }
}
