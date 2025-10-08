package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Cart;

@Repository
public interface CartRepository extends JpaRepository<Cart, String> {

}