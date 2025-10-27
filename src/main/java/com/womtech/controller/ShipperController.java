package com.womtech.controller;

import com.womtech.entity.Order;
import com.womtech.entity.User;
import com.womtech.service.OrderService;
import com.womtech.service.UserService;
import com.womtech.util.OrderStatusHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/shipper")
@RequiredArgsConstructor
public class ShipperController {

	private final OrderService orderService;
	private final UserService userService;

	// ===== Helpers =====
	private User requireCurrentUser(Principal principal) {
		if (principal == null)
			throw new IllegalStateException("Chưa đăng nhập");
		return userService.findById(principal.getName())
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy người dùng"));
	}

	private void assertOrderBelongsToShipper(Order o, String shipperId) {
		if (o.getShipper() == null || o.getShipper().getUserID() == null
				|| !o.getShipper().getUserID().equals(shipperId)) {
			throw new IllegalStateException("Bạn không có quyền trên đơn này.");
		}
	}

	private long countByStatus(List<Order> orders, Integer status) {
		return orders.stream().filter(o -> Objects.equals(o.getStatus(), status)).count();
	}

	private BigDecimal sumCOD(List<Order> orders, boolean collected) {
		// collected=true => paymentStatus == 1 ; false => null/0
		return orders.stream().filter(o -> o.getPaymentMethod() != null
				&& o.getPaymentMethod().toUpperCase().contains("COD")
				&& ((collected && Objects.equals(o.getPaymentStatus(), 1))
						|| (!collected && (o.getPaymentStatus() == null || Objects.equals(o.getPaymentStatus(), 0)))))
				.map(o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	// ===== Dashboard =====
	@GetMapping("/dashboard")
	public String dashboard(Model model, Principal principal) {
		User current = requireCurrentUser(principal);

		List<Order> all = orderService.getOrdersByShipper(current.getUserID());
		long assignedCount = all.size();
		long inTransitCount = countByStatus(all, OrderStatusHelper.STATUS_SHIPPED);
		long deliveredCount = countByStatus(all, OrderStatusHelper.STATUS_DELIVERED);
		long failedCount = countByStatus(all, OrderStatusHelper.STATUS_CANCELLED)
				+ countByStatus(all, OrderStatusHelper.STATUS_RETURNED);

		BigDecimal codToCollect = sumCOD(all, false);
		BigDecimal codCollected = sumCOD(all, true);
		List<Order> recentOrders = all.stream().limit(5).collect(Collectors.toList());

		model.addAttribute("currentUser", current);
		model.addAttribute("assignedCount", assignedCount);
		model.addAttribute("inTransitCount", inTransitCount);
		model.addAttribute("deliveredCount", deliveredCount);
		model.addAttribute("failedCount", failedCount);
		model.addAttribute("codToCollect", codToCollect);
		model.addAttribute("codCollected", codCollected);
		model.addAttribute("recentOrders", recentOrders);
		model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
		return "shipper/dashboard";
	}

	// ===== Danh sách giao hàng =====
	@GetMapping("/deliveries")
	public String deliveries(@RequestParam(required = false) Integer status,
			@RequestParam(required = false, name = "q") String search, @RequestParam(required = false) String startDate,
			@RequestParam(required = false) String endDate, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, Principal principal, Model model) {

		User current = requireCurrentUser(principal);

		List<Order> orders = (status != null) ? orderService.getOrdersByShipperAndStatus(current.getUserID(), status)
				: orderService.getOrdersByShipper(current.getUserID());

		// lọc theo ngày
		LocalDateTime from = (startDate != null && !startDate.isBlank()) ? LocalDate.parse(startDate).atStartOfDay()
				: null;
		LocalDateTime to = (endDate != null && !endDate.isBlank()) ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
				: null;
		if (from != null || to != null) {
			orders = orders.stream().filter(o -> {
				LocalDateTime t = o.getCreateAt();
				if (t == null)
					return false;
				if (from != null && t.isBefore(from))
					return false;
				if (to != null && t.isAfter(to))
					return false;
				return true;
			}).collect(Collectors.toList());
		}

		// search theo orderID / username / phone
		if (search != null && !search.isBlank()) {
			String q = search.toLowerCase().trim();
			orders = orders.stream()
					.filter(o -> (o.getOrderID() != null && o.getOrderID().toLowerCase().contains(q))
							|| (o.getUser() != null && o.getUser().getUsername() != null
									&& o.getUser().getUsername().toLowerCase().contains(q))
							|| (o.getAddress() != null && o.getAddress().getPhone() != null
									&& o.getAddress().getPhone().toLowerCase().contains(q)))
					.collect(Collectors.toList());
		}

		// phân trang
		int start = Math.min(page * size, orders.size());
		int end = Math.min(start + size, orders.size());
		List<Order> pageContent = orders.subList(start, end);
		Pageable pageable = PageRequest.of(page, size);
		PageImpl<Order> pageImpl = new PageImpl<>(pageContent, pageable, orders.size());

		// đếm tab
		Map<String, Long> counts = new HashMap<>();
		List<Order> allForCount = orderService.getOrdersByShipper(current.getUserID());
		counts.put("ALL", (long) allForCount.size());
		counts.put("CONFIRMED", countByStatus(allForCount, OrderStatusHelper.STATUS_CONFIRMED));
		counts.put("PACKED", countByStatus(allForCount, OrderStatusHelper.STATUS_PACKED));
		counts.put("SHIPPED", countByStatus(allForCount, OrderStatusHelper.STATUS_SHIPPED));
		counts.put("DELIVERED", countByStatus(allForCount, OrderStatusHelper.STATUS_DELIVERED));
		counts.put("RETURNED", countByStatus(allForCount, OrderStatusHelper.STATUS_RETURNED));
		counts.put("CANCELLED", countByStatus(allForCount, OrderStatusHelper.STATUS_CANCELLED));

		model.addAttribute("orders", pageContent);
		model.addAttribute("page", pageImpl);
		model.addAttribute("counts", counts);
		model.addAttribute("currentStatus", status);
		model.addAttribute("q", search);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);
		model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
		return "shipper/deliveries";
	}

	// ===== Chi tiết đơn giao =====
	@GetMapping("/deliveries/{orderId}")
	public String deliveryDetail(@PathVariable String orderId, Principal principal, Model model) {
		User current = requireCurrentUser(principal);
		Order order = orderService.getOrderById(orderId)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng"));
		assertOrderBelongsToShipper(order, current.getUserID());

		model.addAttribute("order", order);
		model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
		model.addAttribute("currentUser", current);
		return "shipper/delivery-detail";
	}

	// ===== Chuyển trạng thái khi giao (+ auto set payment COD) =====
	@PostMapping("/deliveries/{orderId}/transition")
	public String transition(@PathVariable String orderId, @RequestParam("targetStatus") Integer targetStatus,
			Principal principal, RedirectAttributes ra) {
		try {
			User current = requireCurrentUser(principal);
			Order order = orderService.getOrderById(orderId)
					.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng"));
			assertOrderBelongsToShipper(order, current.getUserID());

			Integer cur = order.getStatus();
			// Nếu đã kết thúc thì chặn
			if (Objects.equals(cur, OrderStatusHelper.STATUS_CANCELLED)
					|| Objects.equals(cur, OrderStatusHelper.STATUS_DELIVERED)
					|| Objects.equals(cur, OrderStatusHelper.STATUS_RETURNED)) {
				throw new IllegalStateException("Đơn đã kết thúc, không thể chuyển trạng thái.");
			}

			// Rule chuyển trạng thái
			if (Objects.equals(targetStatus, OrderStatusHelper.STATUS_SHIPPED)) {
				if (cur == null || cur < OrderStatusHelper.STATUS_PACKED || cur >= OrderStatusHelper.STATUS_SHIPPED) {
					throw new IllegalStateException(
							"Chỉ chuyển sang 'Đang giao' khi đơn đã Đóng gói và chưa Đang giao.");
				}
			} else if (Objects.equals(targetStatus, OrderStatusHelper.STATUS_DELIVERED)
					|| Objects.equals(targetStatus, OrderStatusHelper.STATUS_RETURNED)) {
				if (!Objects.equals(cur, OrderStatusHelper.STATUS_SHIPPED)) {
					throw new IllegalStateException("Chỉ chuyển trạng thái kết thúc khi đơn đang giao.");
				}
			} else if (!Objects.equals(targetStatus, OrderStatusHelper.STATUS_CANCELLED)) {
				throw new IllegalStateException("Trạng thái đích không hợp lệ.");
			}

			// Cập nhật trạng thái (đồng bộ cả item)
			orderService.updateOrderStatus(orderId, targetStatus);

			// Nếu DELIVERED & COD => auto set paymentStatus = 1
			if (Objects.equals(targetStatus, OrderStatusHelper.STATUS_DELIVERED)) {
				Order saved = orderService.getOrderById(orderId)
						.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng sau khi cập nhật"));
				String pm = saved.getPaymentMethod() != null ? saved.getPaymentMethod().toUpperCase() : "";
				Integer paySt = saved.getPaymentStatus();
				if (pm.contains("COD") && (paySt == null || paySt == 0)) {
					saved.setPaymentStatus(1); // paid
					saved.setUpdateAt(LocalDateTime.now());
					orderService.saveOrder(saved);
				}
			}

			ra.addFlashAttribute("success", "Cập nhật trạng thái thành công.");
		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/shipper/deliveries/" + orderId;
	}

	// ===== Xác nhận đã thu COD (nút riêng khi đã giao) =====
	@PostMapping("/deliveries/{orderId}/confirm-cod")
	public String confirmCOD(@PathVariable String orderId, Principal principal, RedirectAttributes ra) {
		try {
			User current = requireCurrentUser(principal);
			Order order = orderService.getOrderById(orderId)
					.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng"));
			assertOrderBelongsToShipper(order, current.getUserID());

			if (order.getPaymentMethod() == null || !order.getPaymentMethod().toUpperCase().contains("COD")) {
				throw new IllegalStateException("Đơn này không phải COD.");
			}
			if (!Objects.equals(order.getStatus(), OrderStatusHelper.STATUS_DELIVERED)) {
				throw new IllegalStateException("Chỉ xác nhận COD sau khi đơn đã giao.");
			}
			if (Objects.equals(order.getPaymentStatus(), 1)) {
				ra.addFlashAttribute("success", "Đơn đã được xác nhận thanh toán.");
				return "redirect:/shipper/deliveries/" + orderId;
			}

			order.setPaymentStatus(1); // đã thanh toán
			order.setUpdateAt(LocalDateTime.now());
			orderService.saveOrder(order);

			ra.addFlashAttribute("success", "Đã xác nhận thu COD.");
		} catch (Exception e) {
			e.printStackTrace();
			ra.addFlashAttribute("error", e.getMessage());
		}
		return "redirect:/shipper/deliveries/" + orderId;
	}

	// Fallback GET cho các route POST
	@GetMapping("/deliveries/{orderId}/transition")
	public String getTransitionFallback(@PathVariable String orderId) {
		return "redirect:/shipper/deliveries/" + orderId;
	}

	@GetMapping("/deliveries/{orderId}/confirm-cod")
	public String getConfirmCodFallback(@PathVariable String orderId) {
		return "redirect:/shipper/deliveries/" + orderId;
	}

	// ===== Thống kê =====
	@GetMapping("/stats")
	public String stats(@RequestParam(required = false) String from, @RequestParam(required = false) String to,
			Principal principal, Model model) {
		User current = requireCurrentUser(principal);

		LocalDateTime end = (to != null && !to.isBlank()) ? LocalDate.parse(to).atTime(LocalTime.MAX)
				: LocalDateTime.now();
		LocalDateTime start = (from != null && !from.isBlank()) ? LocalDate.parse(from).atStartOfDay()
				: end.minusDays(30);

		List<Order> allAssigned = orderService.getOrdersByShipper(current.getUserID());
		List<Order> range = allAssigned.stream().filter(o -> {
			LocalDateTime t = o.getCreateAt();
			return t != null && !t.isBefore(start) && !t.isAfter(end);
		}).collect(Collectors.toList());

		long totalAssigned = range.size();
		long totalDelivered = countByStatus(range, OrderStatusHelper.STATUS_DELIVERED);
		long totalFailed = countByStatus(range, OrderStatusHelper.STATUS_CANCELLED)
				+ countByStatus(range, OrderStatusHelper.STATUS_RETURNED);
		BigDecimal codCollected = sumCOD(range, true);

		Map<LocalDate, Long> deliveredPerDay = range.stream()
				.filter(o -> Objects.equals(o.getStatus(), OrderStatusHelper.STATUS_DELIVERED)).collect(
						Collectors.groupingBy(o -> o.getCreateAt().toLocalDate(), TreeMap::new, Collectors.counting()));

		List<String> labels = new ArrayList<>();
		List<Long> values = new ArrayList<>();
		LocalDate cur = start.toLocalDate();
		LocalDate ed = end.toLocalDate();
		while (!cur.isAfter(ed)) {
			labels.add(cur.toString());
			values.add(deliveredPerDay.getOrDefault(cur, 0L));
			cur = cur.plusDays(1);
		}

		model.addAttribute("currentUser", current);
		model.addAttribute("from", start.toLocalDate().toString());
		model.addAttribute("to", end.toLocalDate().toString());
		model.addAttribute("totalAssigned", totalAssigned);
		model.addAttribute("totalDelivered", totalDelivered);
		model.addAttribute("totalFailed", totalFailed);
		model.addAttribute("codCollected", codCollected);
		model.addAttribute("chartLabels", labels);
		model.addAttribute("chartValues", values);
		model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
		return "shipper/stats";
	}
}