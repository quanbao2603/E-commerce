package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Subcategory;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, String> {

}