package com.womtech.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.womtech.entity.Order;
import com.womtech.entity.OrderItem;
import com.womtech.entity.User;
import com.womtech.repository.OrderItemRepository;
import com.womtech.repository.OrderRepository;
import com.womtech.repository.UserRepository;
import com.womtech.util.OrderStatusHelper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeliveryProofService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final CloudinaryService cloudinaryService;
    
    private final OrderItemService orderItemService;
    private final OrderItemRepository orderItemRepository; 

    // Cho phép upload khi PACKED hoặc SHIPPED
    private boolean canUploadForStatus(Integer status) {
        return status != null && (
                status.equals(OrderStatusHelper.STATUS_PACKED) ||
                status.equals(OrderStatusHelper.STATUS_SHIPPED)
        );
    }

    @Transactional
    public void uploadProof(String orderId,
                            String currentUsername,
                            MultipartFile image,
                            boolean markDelivered) {

        // (khuyến nghị) validate đơn giản file
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Chưa chọn file để upload!");
        }
        String ct = Optional.ofNullable(image.getContentType()).orElse("");
        Set<String> allowed = Set.of("image/jpeg","image/png","image/webp");
        if (!allowed.contains(ct)) {
            throw new IllegalArgumentException("Chỉ chấp nhận ảnh JPG/PNG/WebP.");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng."));

        // Nếu hệ thống đăng nhập bằng email, cân nhắc dùng findByUsernameOrEmail
        User current = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy user hiện tại."));

        // Chỉ shipper phụ trách mới được upload
        if (order.getShipper() == null || !Objects.equals(order.getShipper().getUserID(), current.getUserID())) {
            throw new SecurityException("Bạn không phải shipper phụ trách đơn này.");
        }
        if (!canUploadForStatus(order.getStatus())) {
            throw new IllegalStateException("Trạng thái đơn hiện tại không cho phép upload ảnh.");
        }

        // Xoá ảnh cũ nếu có
        if (order.getDeliveryThumbnail() != null) {
            cloudinaryService.deleteImage(order.getDeliveryThumbnail());
        }

        // Upload vào folder theo orderId
        String folder = "womtech/deliveries/" + orderId;
        String secureUrl = cloudinaryService.uploadToFolder(image, folder);

        order.setDeliveryThumbnail(secureUrl);
        order.setDeliveryImageUploadedAt(java.time.LocalDateTime.now());

        // Đánh dấu ĐÃ GIAO => phải set STATUS_DELIVERED (6)
        if (markDelivered) {
            order.setStatus(OrderStatusHelper.STATUS_DELIVERED);
        }
        //✅ Cập nhật tất cả OrderItem → DELIVERED
        List<OrderItem> items = orderItemService.findByOrder(order);
        for (OrderItem item : items) {
            if (item.getStatus() < OrderStatusHelper.ITEM_STATUS_DELIVERED) {
                item.setStatus(OrderStatusHelper.ITEM_STATUS_DELIVERED);
                orderItemRepository.save(item);
            }
        }

        orderRepository.save(order);
    }
}
