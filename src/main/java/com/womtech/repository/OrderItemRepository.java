package com.womtech.repository;

import com.womtech.entity.Order;
import com.womtech.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
    
    List<OrderItem> findByOrder(Order order);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.product.ownerUser.userID = :vendorId")
    List<OrderItem> findOrderItemsByVendorId(@Param("vendorId") String vendorId);
    
    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.orderID = :orderId AND oi.product.ownerUser.userID = :vendorId")
    List<OrderItem> findOrderItemsByOrderIdAndVendorId(@Param("orderId") String orderId, @Param("vendorId") String vendorId);
}
