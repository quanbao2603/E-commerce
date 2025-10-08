package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
	
}