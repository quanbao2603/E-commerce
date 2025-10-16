package com.womtech.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.womtech.entity.Order;
import com.womtech.entity.User;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
	List<Order> findByUser(User user);
}