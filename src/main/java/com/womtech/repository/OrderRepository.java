package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
	
}