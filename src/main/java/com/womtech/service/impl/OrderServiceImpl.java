package com.womtech.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.womtech.entity.Address;
import com.womtech.entity.Cart;
import com.womtech.entity.Order;
import com.womtech.entity.User;
import com.womtech.repository.OrderRepository;
import com.womtech.service.AddressService;
import com.womtech.service.CartItemService;
import com.womtech.service.CartService;
import com.womtech.service.OrderItemService;
import com.womtech.service.OrderService;
import com.womtech.util.OrderStatusHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends BaseServiceImpl<Order, String> implements OrderService {
	@Autowired
	CartService cartService;
	@Autowired
    CartItemService cartItemService;
	@Autowired
    AddressService addressService;
	@Autowired
	OrderItemService orderItemService;
	@Autowired
    OrderRepository orderRepository;

	public OrderServiceImpl(JpaRepository<Order, String> repo) {
		super(repo);
	}

    @Override
	public Order createOrder(User user, Address address, String payment_method) {
        Cart cart = cartService.findByUser(user);
        // Chưa thêm voucher
        BigDecimal total = cartService.totalPrice(cart);
        
        Order order = Order.builder()
                .user(user)
                .address(address)
                .totalPrice(total)
                .paymentMethod(payment_method)
                .totalPrice(total)
                .createAt(LocalDateTime.now())
                .updateAt(LocalDateTime.now())
                .build();

        orderItemService.createItemsFromCart(order, cart);
        
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void deleteOrder(String orderId) {
        orderRepository.deleteById(orderId);
    }
    
    @Override
    public Map<String, Object> getRevenueChartData(String vendorId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = orderRepository.findDailyRevenueByVendorAndPeriod(vendorId, start, end);
        
        List<String> dates = new ArrayList<>();
        List<Double> revenues = new ArrayList<>();
        
        // Tạo danh sách tất cả các ngày trong khoảng thời gian
        LocalDate currentDate = start.toLocalDate();
        LocalDate endDate = end.toLocalDate();
        
        Map<LocalDate, Double> revenueMap = new HashMap<>();
        for (Object[] result : results) {
            LocalDate date = ((java.sql.Date) result[0]).toLocalDate();
            BigDecimal revenue = (BigDecimal) result[1];
            revenueMap.put(date, revenue.doubleValue());
        }
        
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate.format(DateTimeFormatter.ofPattern("dd/MM")));
            revenues.add(revenueMap.getOrDefault(currentDate, 0.0));
            currentDate = currentDate.plusDays(1);
        }
        
        Map<String, Object> chartData = new HashMap<>();
        chartData.put("dates", dates);
        chartData.put("revenues", revenues);
        
        return chartData;
    }
    
    @Override
    public Map<String, Object> getCategoryRevenueData(String vendorId, LocalDateTime start, LocalDateTime end) {
        List<Object[]> results = orderItemRepository.findRevenueByCategoryAndVendor(vendorId, start, end);
        
        List<String> labels = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        
        double totalRevenue = 0.0;
        
        for (Object[] result : results) {
            String categoryName = (String) result[0];
            BigDecimal revenue = (BigDecimal) result[1];
            
            if (categoryName == null) {
                categoryName = "Không phân loại";
            }
            
            double revenueValue = revenue.doubleValue();
            labels.add(categoryName);
            values.add(revenueValue);
            totalRevenue += revenueValue;
        }
        
        // Nếu không có dữ liệu, trả về mảng rỗng
        if (labels.isEmpty()) {
            labels.add("Không có dữ liệu");
            values.add(1.0); // Giá trị mặc định để hiển thị biểu đồ
        }
        
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("labels", labels);
        categoryData.put("values", values);
        categoryData.put("totalRevenue", totalRevenue);
        
        return categoryData;
    }
    
    @Override
    public Map<String, Object> getTopProductsData(String vendorId, LocalDateTime start, LocalDateTime end) {
        // Lấy top 5 sản phẩm
        Pageable topFive = PageRequest.of(0, 5);
        List<Object[]> results = orderItemRepository.findTopProductsByVendor(vendorId, start, end, topFive);
        
        List<String> labels = new ArrayList<>();
        List<Long> values = new ArrayList<>();
        
        for (Object[] result : results) {
            String productName = (String) result[0];
            Long quantity = ((Number) result[1]).longValue();
            
            // Rút gọn tên sản phẩm nếu quá dài
            if (productName.length() > 20) {
                productName = productName.substring(0, 17) + "...";
            }
            
            labels.add(productName);
            values.add(quantity);
        }
        
        // Nếu không có dữ liệu, thêm item mặc định
        if (labels.isEmpty()) {
            labels.add("Không có sản phẩm nào");
            values.add(0L);
        }
        
        Map<String, Object> topProductsData = new HashMap<>();
        topProductsData.put("labels", labels);
        topProductsData.put("values", values);
        
        return topProductsData;
    }
    
    @Override
    public Map<String, Object> getVendorOrderStatistics(String vendorId, LocalDateTime start, LocalDateTime end) {
        Map<String, Object> statistics = new HashMap<>();
        
        BigDecimal totalRevenue = orderItemRepository.calculateTotalRevenueByVendor(vendorId, start, end);
        
        Long totalOrders = orderItemRepository.countDistinctOrdersByVendor(vendorId, start, end);

        List<Object[]> ordersByStatus = orderItemRepository.countOrdersByStatus(vendorId, start, end);
        
        Map<String, Long> statusMap = new HashMap<>();
        for (Object[] result : ordersByStatus) {
            Integer status = (Integer) result[0];
            Long count = (Long) result[1];
            statusMap.put(status.toString(), count);
        }
        
        statistics.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        statistics.put("totalOrders", totalOrders != null ? totalOrders : 0L);
        statistics.put("ordersByStatus", statusMap);
        statistics.put("startDate", start);
        statistics.put("endDate", end);
        
        return statistics;
    }
}
