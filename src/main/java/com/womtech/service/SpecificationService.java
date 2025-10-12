package com.womtech.service;

import com.womtech.entity.Specification;
import com.womtech.entity.Product;

import java.util.List;
import java.util.Optional;

public interface SpecificationService {

    List<Specification> getAllSpecifications();

    List<Specification> getSpecificationsByProduct(Product product);

    List<Specification> getSpecificationsByProductID(String productID);

    Optional<Specification> getSpecificationByID(String id);

    Specification saveSpecification(Specification specification);

    void deleteSpecification(String id);

    long getTotalCount();
}
