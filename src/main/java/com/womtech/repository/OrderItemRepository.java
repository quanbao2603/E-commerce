package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {

}