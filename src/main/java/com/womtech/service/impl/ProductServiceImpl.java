package com.womtech.service.impl;

import com.womtech.entity.Brand;
import com.womtech.entity.Category;
import com.womtech.entity.Product;
import com.womtech.entity.Subcategory;
import com.womtech.repository.ProductRepository;
import com.womtech.service.ProductService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductRepository productRepository;

    // BASIC CRUD

    @Override
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    @Override
    public Page<Product> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
    
    @Override
    public List<Product> getActiveProducts() {
        return productRepository.findByStatus(1);
    }
    
    @Override
	  public Page<Product> getActiveProducts(Pageable pageable) {
    	return productRepository.findByStatus(1, pageable);
	  }

    @Override
    public List<Product> getActiveProductsNewest() {
        return productRepository.findActiveProductsOrderByNewest();
    }

    @Override
    public Optional<Product> getProductById(String id) {
        return productRepository.findById(id);
    }

    @Override
    public Product saveProduct(Product product) {
    	if (product.getProductID() != null && product.getProductID().trim().isEmpty()) {
    		product.setProductID(null); // <- ép về null để Hibernate tự sinh UUID
        }
        return productRepository.save(product);
    }

    @Override
    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }

//    // FIND BY RELATIONSHIPS
//
//    @Override
//    public List<Product> getProductsBySubcategory(Subcategory subcategory) {
//        return productRepository.findBySubcategory(subcategory);
//    }
//
//    @Override
//    public List<Product> getProductsBySubcategorySubcategoryID(String subcategoryID) {
//        return productRepository.findBySubcategorySubcategoryID(subcategoryID);
//    }
//
//    @Override
//    public List<Product> getProductsByBrand(Brand brand) {
//        return productRepository.findByBrand(brand);
//    }
//
//    @Override
//    public List<Product> getProductsByBrandBrandID(String brandID) {
//        return productRepository.findByBrandBrandID(brandID);
//    }
//
//    @Override
//    public List<Product> getProductsByCategory(Category category) {
//        return productRepository.findBySubcategoryCategory(category);
//    }
//
//    @Override
//    public List<Product> getProductsByCategoryCategoryID(String categoryID) {
//        return productRepository.findBySubcategoryCategoryCategoryID(categoryID);
//    }
//
//    @Override
//    public List<Product> getActiveProductsByCategoryID(String categoryId) {
//        return productRepository.findActiveByCategoryID(categoryId);
//    }
//
//    // SEARCH & FILTER
//
//    @Override
//    public List<Product> searchProducts(String keyword) {
//        if (keyword == null || keyword.trim().isEmpty()) {
//            return getActiveProducts();
//        }
//        return productRepository.searchProducts(keyword.trim());
//    }
//
//    @Override
//    public List<Product> searchByName(String keyword) {
//        return productRepository.findByNameContainingIgnoreCase(keyword);
//    }
//
//    @Override
//    public List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice) {
//        return productRepository.findByPriceBetween(minPrice, maxPrice);
//    }
//
//    @Override
//    public List<Product> getProductsOnSale() {
//        return productRepository.findProductsOnSale();
//    }
//
//    @Override
//    public List<Product> getDiscountedProducts() {
//        return productRepository.findByDiscount_priceIsNotNull();
//    }
//
//    // COUNT & STATISTICS
//
      @Override
	    public long getTotalCount() {
	        return productRepository.count();
	    }

//    @Override
//    public long countByStatus(Integer status) {
//        return productRepository.countByStatus(status);
//    }
//
//    @Override
//    public long countActiveProducts() {
//        return productRepository.countByStatus(1);
//    }
//
//    @Override
//    public long countBySubcategory(Subcategory subcategory) {
//        return productRepository.countBySubcategory(subcategory);
//    }
//
//    @Override
//    public long countByBrand(Brand brand) {
//        return productRepository.countByBrand(brand);
//    }
//
//    // BUSINESS LOGIC
//
//    //@Override
////    public Product createProduct(String name, String description, BigDecimal price, 
////                                Brand brand, Subcategory subcategory) {
////        Product product = new Product(name, description, price, brand, subcategory);
////        return saveProduct(product);
////    }
//
//    @Override
//    public void activateProduct(String productId) {
//        Optional<Product> productOpt = getProductById(productId);
//        if (productOpt.isPresent()) {
//            Product product = productOpt.get();
//            product.setStatus(1);
//            saveProduct(product);
//        }
//    }
//
//    @Override
//    public void deactivateProduct(String productId) {
//        Optional<Product> productOpt = getProductById(productId);
//        if (productOpt.isPresent()) {
//            Product product = productOpt.get();
//            product.setStatus(0);
//            saveProduct(product);
//        }
//    }
//
//    @Override
//    public void setOutOfStock(String productId) {
//        Optional<Product> productOpt = getProductById(productId);
//        if (productOpt.isPresent()) {
//            Product product = productOpt.get();
//            product.setStatus(2);
//            saveProduct(product);
//        }
//    }
}