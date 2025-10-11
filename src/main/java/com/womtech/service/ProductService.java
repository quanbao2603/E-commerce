package com.womtech.service;

import com.womtech.entity.Brand;
import com.womtech.entity.Category;
import com.womtech.entity.Product;
import com.womtech.entity.Subcategory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {
    // BASIC CRUD
    List<Product> getAllProducts();
    List<Product> getActiveProducts();
    List<Product> getActiveProductsNewest();
    Optional<Product> getProductById(String id);
    Product saveProduct(Product product);
    void deleteProduct(String id);

//    // FIND BY RELATIONSHIPS
//    List<Product> getProductsBySubcategory(Subcategory subcategory);
//    List<Product> getProductsBySubcategorySubcategoryID(String subcategoryID);
//    List<Product> getProductsByBrand(Brand brand);
//    List<Product> getProductsByBrandBrandID(String brandID);
//    List<Product> getProductsByCategory(Category category);
//    List<Product> getProductsByCategoryCategoryID(String categoryID);
//    List<Product> getActiveProductsByCategoryID(String categoryID);
//
//    // SEARCH & FILTER
//    List<Product> searchProducts(String keyword);
//    List<Product> searchByName(String keyword);
//    List<Product> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice);
//    List<Product> getProductsOnSale();
//    List<Product> getDiscountedProducts();
//
//    // COUNT & STATISTICS
      long getTotalCount();
//    long countByStatus(Integer status);
//    long countActiveProducts();
//    long countBySubcategory(Subcategory subcategory);
//    long countByBrand(Brand brand);
//
//    // BUSINESS LOGIC
//    //Product createProduct(String name, String description, BigDecimal price, Brand brand, Subcategory subcategory);
//    void activateProduct(String productID);
//    void deactivateProduct(String productID);
//    void setOutOfStock(String productID);
	  Page<Product> getAllProducts(Pageable pageable);
}