package com.womtech.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.womtech.entity.OrderItem;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, String> {
	@Query("""
	        SELECT COUNT(oi) > 0 FROM OrderItem oi
	        WHERE oi.product.productID = :productId
	          AND oi.order.user.userID = :userId
	          AND oi.order.status = 1
	    """)
	    boolean hasUserPurchasedProduct(@Param("userId") String userId,
	                                    @Param("productId") String productId);
}