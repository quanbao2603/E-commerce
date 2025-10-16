package com.womtech.service;

import com.womtech.entity.Order;
import com.womtech.entity.OrderItem;
import com.womtech.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface OrderService {
    List<Order> getAllOrders();
    Optional<Order> getOrderById(String orderId);
    List<Order> getOrdersByUser(User user);
    List<Order> getOrdersByStatus(Integer status);
    List<Order> getOrdersByVendorId(String vendorId);
    List<Order> getOrdersByVendorIdAndStatus(String vendorId, Integer status);
    List<Order> getOrdersByVendorIdAndDateRange(String vendorId, LocalDateTime startDate, LocalDateTime endDate);
    Long countOrdersByVendorId(String vendorId);
    Long countOrdersByVendorIdAndStatus(String vendorId, Integer status);
    List<OrderItem> getOrderItemsByOrderIdAndVendorId(String orderId, String vendorId);
    
    Order saveOrder(Order order);
    void updateOrderStatus(String orderId, Integer newStatus);
    void updateVendorOrderItemsStatus(String orderId, String vendorId, Integer newItemStatus);
    void cancelOrder(String orderId);
    void cancelVendorOrderItems(String orderId, String vendorId);
    Map<String, Object> getVendorOrderStatistics(String vendorId, LocalDateTime startDate, LocalDateTime endDate);
    void deleteOrder(String orderId);
}
