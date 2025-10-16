package com.womtech.repository;

import com.womtech.entity.Order;
import com.womtech.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {
    
	List<Order> findByUserOrderByCreateAtDesc(User user);

	List<Order> findByStatusOrderByCreateAtDesc(Integer status);

	// 1️⃣ Lấy tất cả đơn hàng có chứa sản phẩm thuộc vendor
	@Query("SELECT DISTINCT o FROM Order o JOIN o.items oi WHERE oi.product.ownerUser.userID = :vendorId ORDER BY o.createAt DESC")
	List<Order> findOrdersByVendorId(@Param("vendorId") String vendorId);

	// 2️⃣ Lấy đơn hàng của vendor theo trạng thái
	@Query("SELECT DISTINCT o FROM Order o JOIN o.items oi WHERE oi.product.ownerUser.userID = :vendorId AND o.status = :status ORDER BY o.createAt DESC")
	List<Order> findOrdersByVendorIdAndStatus(@Param("vendorId") String vendorId, @Param("status") Integer status);

	// 3️⃣ Lọc theo khoảng thời gian
	@Query("SELECT DISTINCT o FROM Order o JOIN o.items oi WHERE oi.product.ownerUser.userID = :vendorId AND o.createAt BETWEEN :startDate AND :endDate ORDER BY o.createAt DESC")
	List<Order> findOrdersByVendorIdAndDateRange(@Param("vendorId") String vendorId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

	// 4️⃣ Đếm tổng số đơn hàng có sản phẩm thuộc vendor
	@Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items oi WHERE oi.product.ownerUser.userID = :vendorId")
	Long countOrdersByVendorId(@Param("vendorId") String vendorId);

	// 5️⃣ Đếm số đơn hàng theo trạng thái cho vendor
	@Query("SELECT COUNT(DISTINCT o) FROM Order o JOIN o.items oi WHERE oi.product.ownerUser.userID = :vendorId AND o.status = :status")
	Long countOrdersByVendorIdAndStatus(@Param("vendorId") String vendorId, @Param("status") Integer status);


}
