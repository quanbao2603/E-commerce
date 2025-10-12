package com.womtech.repository;

import com.womtech.entity.Brand;
import com.womtech.entity.Category;
import com.womtech.entity.Product;
import com.womtech.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    
//    // Find by relationships
//    List<Product> findBySubcategory(Subcategory subcategory);
//    
//    List<Product> findBySubcategorySubcategoryID(String subcategoryID);
//    
//    List<Product> findByBrand(Brand brand);
//    
//    List<Product> findByBrandBrandID(String brandID);
//    
//    List<Product> findBySubcategoryCategory(Category category);
//    
//    List<Product> findBySubcategoryCategoryCategoryID(String categoryID);
//    
//    // Find by status
		List<Product> findByStatus(Integer status);
//    
//    List<Product> findByStatusOrderByCreateAtDesc(Integer status);
//    
//    // Search
//    @Query("SELECT p FROM Product p WHERE " +
//           "LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//           "LOWER(p.brand.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//           "LOWER(p.subcategory.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//           "LOWER(p.subcategory.category.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
//           "LOWER(p.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
//    List<Product> searchProducts(@Param("keyword") String keyword);
//    
//    List<Product> findByNameContainingIgnoreCase(String keyword);
//    
//    // Price queries
//    List<Product> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);
//    
//    List<Product> findByDiscount_priceIsNotNull();
//    
//    // Count queries
//    long countBySubcategory(Subcategory subcategory);
//    
//    long countByBrand(Brand brand);
//    
//    long countByStatus(Integer status);
//    
    // Advanced queries
    @Query("SELECT p FROM Product p WHERE p.status = 1 ORDER BY p.createAt DESC")
    List<Product> findActiveProductsOrderByNewest();
//    
//    @Query("SELECT p FROM Product p WHERE p.subcategory.category.categoryID = :categoryID AND p.status = 1")
//    List<Product> findActiveByCategoryID(@Param("categoryID") String categoryID);
//    
//    @Query("SELECT p FROM Product p WHERE p.discount_price IS NOT NULL AND p.status = 1 ORDER BY p.discount_price ASC")
//    List<Product> findProductsOnSale();
}