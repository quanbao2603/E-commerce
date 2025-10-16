package com.womtech.service.impl;

import com.womtech.entity.Order;
import com.womtech.entity.OrderItem;
import com.womtech.entity.User;
import com.womtech.repository.OrderItemRepository;
import com.womtech.repository.OrderRepository;
import com.womtech.service.OrderService;
import com.womtech.util.OrderStatusHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Override
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @Override
    public Optional<Order> getOrderById(String orderId) {
        return orderRepository.findById(orderId);
    }

    @Override
    public List<Order> getOrdersByUser(User user) {
        return orderRepository.findByUserOrderByCreateAtDesc(user);
    }

    @Override
    public List<Order> getOrdersByStatus(Integer status) {
        return orderRepository.findByStatusOrderByCreateAtDesc(status);
    }

    @Override
    public List<Order> getOrdersByVendorId(String vendorId) {
        return orderRepository.findOrdersByVendorId(vendorId);
    }

    @Override
    public List<Order> getOrdersByVendorIdAndStatus(String vendorId, Integer status) {
        return orderRepository.findOrdersByVendorIdAndStatus(vendorId, status);
    }

    @Override
    public List<Order> getOrdersByVendorIdAndDateRange(String vendorId, LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findOrdersByVendorIdAndDateRange(vendorId, startDate, endDate);
    }

    @Override
    public Long countOrdersByVendorId(String vendorId) {
        return orderRepository.countOrdersByVendorId(vendorId);
    }

    @Override
    public Long countOrdersByVendorIdAndStatus(String vendorId, Integer status) {
        return orderRepository.countOrdersByVendorIdAndStatus(vendorId, status);
    }

    @Override
    public List<OrderItem> getOrderItemsByOrderIdAndVendorId(String orderId, String vendorId) {
        return orderItemRepository.findOrderItemsByOrderIdAndVendorId(orderId, vendorId);
    }

    @Override
    @Transactional
    public Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public void updateOrderStatus(String orderId, Integer newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(newStatus);

        Integer itemStatus = OrderStatusHelper.orderStatusToItemStatus(newStatus);
        for (OrderItem item : order.getItems()) {
            item.setStatus(itemStatus);
        }

        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void updateVendorOrderItemsStatus(String orderId, String vendorId, Integer newItemStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean hasVendorItems = false;
        for (OrderItem item : order.getItems()) {
            if (item.getProduct().getOwnerUser() != null &&
                item.getProduct().getOwnerUser().getUserID().equals(vendorId)) {
                item.setStatus(newItemStatus);
                hasVendorItems = true;
            }
        }

        if (!hasVendorItems) {
            throw new RuntimeException("No items from this vendor in the order");
        }

        Integer maxItemStatus = order.getItems().stream()
                .map(OrderItem::getStatus)
                .max(Integer::compareTo)
                .orElse(newItemStatus);

        order.setStatus(OrderStatusHelper.itemStatusToOrderStatus(maxItemStatus));
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelOrder(String orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        order.setStatus(OrderStatusHelper.STATUS_CANCELLED);
        for (OrderItem item : order.getItems()) {
            item.setStatus(OrderStatusHelper.ITEM_STATUS_CANCELLED);
        }

        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void cancelVendorOrderItems(String orderId, String vendorId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        boolean hasVendorItems = false;
        for (OrderItem item : order.getItems()) {
            if (item.getProduct().getOwnerUser() != null &&
                item.getProduct().getOwnerUser().getUserID().equals(vendorId)) {
                item.setStatus(OrderStatusHelper.ITEM_STATUS_CANCELLED);
                hasVendorItems = true;
            }
        }

        if (!hasVendorItems) {
            throw new RuntimeException("No items from this vendor in the order");
        }

        boolean allCancelled = order.getItems().stream()
                .allMatch(item -> item.getStatus().equals(OrderStatusHelper.ITEM_STATUS_CANCELLED));

        if (allCancelled) {
            order.setStatus(OrderStatusHelper.STATUS_CANCELLED);
        }

        orderRepository.save(order);
    }

    @Override
    public Map<String, Object> getVendorOrderStatistics(String vendorId, LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = new HashMap<>();

        List<Order> orders = getOrdersByVendorIdAndDateRange(vendorId, startDate, endDate);
        List<Order> relevantOrders = orders.stream()
                .filter(order -> order.getItems().stream()
                        .anyMatch(item -> item.getProduct().getOwnerUser() != null
                                && item.getProduct().getOwnerUser().getUserID().equals(vendorId)))
                .collect(Collectors.toList());

        BigDecimal totalRevenue = BigDecimal.ZERO;
        for (Order order : relevantOrders) {
            BigDecimal orderRevenue = order.getItems().stream()
                    .filter(item -> item.getProduct().getOwnerUser() != null
                            && item.getProduct().getOwnerUser().getUserID().equals(vendorId))
                    .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            totalRevenue = totalRevenue.add(orderRevenue);
        }

        Map<String, Long> ordersByStatus = new HashMap<>();
        ordersByStatus.put("PENDING", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_PENDING));
        ordersByStatus.put("CONFIRMED", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_CONFIRMED));
        ordersByStatus.put("PREPARING", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_PREPARING));
        ordersByStatus.put("PACKED", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_PACKED));
        ordersByStatus.put("SHIPPED", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_SHIPPED));
        ordersByStatus.put("DELIVERED", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_DELIVERED));
        ordersByStatus.put("CANCELLED", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_CANCELLED));
        ordersByStatus.put("RETURNED", countOrdersByVendorIdAndStatus(vendorId, OrderStatusHelper.STATUS_RETURNED));

        stats.put("totalRevenue", totalRevenue);
        stats.put("totalOrders", relevantOrders.size());
        stats.put("ordersByStatus", ordersByStatus);
        stats.put("startDate", startDate);
        stats.put("endDate", endDate);

        return stats;
    }

    @Override
    @Transactional
    public void deleteOrder(String orderId) {
        orderRepository.deleteById(orderId);
    }
}
