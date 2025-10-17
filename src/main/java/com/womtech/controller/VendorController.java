package com.womtech.controller;

import com.womtech.entity.*;
import com.womtech.service.*;
import com.womtech.util.CookieUtil;
import com.womtech.util.EmailUtil;
import com.womtech.util.OrderStatusHelper;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/vendor")
public class VendorController {

	@Autowired
	private EmailUtil emailUtil;

	private static final String CK_AT = "AT";
	private static final String CK_RT = "RT";
	private static final String CK_REMEMBER = "WOM_REMEMBER";

	// Session keys cho flow “Vendor Register”
	private static final String SK_VR_OTP = "VR_OTP";
	private static final String SK_VR_EXPIRE = "VR_EXPIRE";
	private static final String SK_VR_LAST_SEND = "VR_LAST_SEND";

	private static final int VR_OTP_TTL_MIN = 10; // OTP sống 10 phút
	private static final int VR_RESEND_COOLDOWN = 60;

	@Autowired
	private ProductService productService;

	@Autowired
	private CategoryService categoryService;

	@Autowired
	private SubcategoryService subcategoryService;

	@Autowired
	private BrandService brandService;

	@Autowired
	private SpecificationService specificationService;

	@Autowired
	private CloudinaryService cloudinaryService;

	@Autowired
	private UserService userService;

	@Autowired
	private OrderService orderService;

	// Helper method to get current user
	private User getCurrentUser(Authentication authentication) {
		if (authentication == null || !authentication.isAuthenticated()) {
			throw new RuntimeException("User not authenticated");
		}
		System.out.println("Current username: " + authentication.getName());

		String UserID = authentication.getName();
		return userService.findById(UserID).orElseThrow(() -> new RuntimeException("User not found"));

	}

	// Helper method to check if user owns the product
	private boolean isOwner(Product product, User user) {
		return product.getOwnerUser() != null && product.getOwnerUser().getUserID().equals(user.getUserID());
	}

	// Helper method to check if user owns the orders

	// ========== DASHBOARD ==========
	@GetMapping("/dashboard")
	public String vendorDashboard(Authentication authentication, Model model) {
		User currentUser = getCurrentUser(authentication);
		List<Product> myProducts = productService.getAllProducts().stream().filter(p -> isOwner(p, currentUser))
				.collect(Collectors.toList());

		// Calculate statistics
		long totalProducts = myProducts.size();
		long activeProducts = myProducts.stream().filter(p -> p.getStatus() == 1).count();
		long outOfStockProducts = myProducts.stream().filter(p -> p.getStatus() == 2).count();

		// Calculate total value
		BigDecimal totalValue = productService.calculateTotalValueByOwnerId(currentUser.getUserID());

		// Order statistics
		Long totalOrders = orderService.countOrdersByVendorId(currentUser.getUserID());
		Long pendingOrders = orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_PENDING);
		Long shippedOrders = orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_SHIPPED);
		Long deliveredOrders = orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_DELIVERED);

		// Get recent orders
		List<Order> recentOrders = orderService.getOrdersByVendorId(currentUser.getUserID()).stream().limit(5)
				.collect(Collectors.toList());

		model.addAttribute("totalProducts", totalProducts);
		model.addAttribute("activeProducts", activeProducts);
		model.addAttribute("outOfStockProducts", outOfStockProducts);
		model.addAttribute("totalValue", totalValue);
		model.addAttribute("recentProducts", myProducts.stream().limit(5).collect(Collectors.toList()));
		model.addAttribute("currentUser", currentUser);
		model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
		model.addAttribute("recentOrders", recentOrders);
		model.addAttribute("deliveredOrders", deliveredOrders);
		model.addAttribute("shippedOrders", shippedOrders);
		model.addAttribute("totalOrders", totalOrders);
		model.addAttribute("pendingOrders", pendingOrders);

		return "vendor/dashboard";
	}

	// ========== PRODUCT MANAGEMENT ==========
	@GetMapping("/products")
	public String listProducts(@RequestParam(required = false) String search,
			@RequestParam(required = false) String categoryID, @RequestParam(required = false) String subcategoryID,
			@RequestParam(required = false) String brandId, @RequestParam(required = false) Integer status,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size,
			Authentication authentication, Model model) {

		User currentUser = getCurrentUser(authentication);

		// Get only products owned by current vendor
		List<Product> allProducts = productService.getAllProducts().stream().filter(p -> isOwner(p, currentUser))
				.collect(Collectors.toList());
		System.out.println(currentUser.getUserID() + " có " + allProducts.size() + " sản phẩm");
		// Apply filters
		if (search != null && !search.trim().isEmpty()) {
			String searchLower = search.toLowerCase().trim();
			allProducts = allProducts.stream().filter(p -> p.getName().toLowerCase().contains(searchLower)
					|| p.getProductID().toLowerCase().contains(searchLower)).collect(Collectors.toList());
		}

		if (categoryID != null && !categoryID.trim().isEmpty()) {
			allProducts = allProducts.stream()
					.filter(p -> p.getSubcategory().getCategory().getCategoryID().equals(categoryID))
					.collect(Collectors.toList());
		}

		if (subcategoryID != null && !subcategoryID.trim().isEmpty()) {
			allProducts = allProducts.stream().filter(p -> p.getSubcategory().getSubcategoryID().equals(subcategoryID))
					.collect(Collectors.toList());
		}

		if (brandId != null && !brandId.trim().isEmpty()) {
			allProducts = allProducts.stream().filter(p -> p.getBrand().getBrandID().equals(brandId))
					.collect(Collectors.toList());
		}

		if (status != null) {
			allProducts = allProducts.stream().filter(p -> p.getStatus().equals(status)).collect(Collectors.toList());
		}

		// Sort by createAt descending
		allProducts = allProducts.stream().sorted((p1, p2) -> p2.getCreateAt().compareTo(p1.getCreateAt()))
				.collect(Collectors.toList());

		// Manual pagination
		int start = Math.min(page * size, allProducts.size());
		int end = Math.min(start + size, allProducts.size());
		List<Product> pagedProducts = allProducts.subList(start, end);

		// Create Page object
		Pageable pageable = PageRequest.of(page, size);
		org.springframework.data.domain.PageImpl<Product> pageImpl = new org.springframework.data.domain.PageImpl<>(
				pagedProducts, pageable, allProducts.size());

		model.addAttribute("products", pagedProducts);
		model.addAttribute("page", pageImpl);
		model.addAttribute("categories", categoryService.getActiveCategories());
		model.addAttribute("brands", brandService.getActiveBrands());

		if (categoryID != null && !categoryID.trim().isEmpty()) {
			model.addAttribute("subcategories", subcategoryService.getSubcategoriesByCategoryId(categoryID));
		}

		return "vendor/products";
	}

	@GetMapping("/products/new")
	public String newProductForm(Authentication authentication, Model model) {
		model.addAttribute("product", new Product());
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("brands", brandService.getAllBrands());
		return "vendor/product-form";
	}

	@GetMapping("/products/edit/{id}")
	public String editProductForm(@PathVariable String id, Authentication authentication, Model model) {
		User currentUser = getCurrentUser(authentication);
		Product product = productService.getProductById(id)
				.orElseThrow(() -> new RuntimeException("Product not found"));

		// Check ownership
		if (!isOwner(product, currentUser)) {
			throw new RuntimeException("You don't have permission to edit this product");
		}

		model.addAttribute("product", product);
		model.addAttribute("categories", categoryService.getAllCategories());
		model.addAttribute("brands", brandService.getAllBrands());
		return "vendor/product-form";
	}

	@PostMapping("/products/save")
	public String saveProduct(@ModelAttribute Product product,
			@RequestParam(required = false) MultipartFile thumbnailFile, Authentication authentication,
			RedirectAttributes redirectAttributes) {

		try {
			User currentUser = getCurrentUser(authentication);

			// If editing, check ownership
			if (product.getProductID() != null && !product.getProductID().isEmpty()) {
				Product existingProduct = productService.getProductById(product.getProductID())
						.orElseThrow(() -> new RuntimeException("Product not found"));

				if (!isOwner(existingProduct, currentUser)) {
					throw new RuntimeException("You don't have permission to edit this product");
				}

				// Preserve existing thumbnail if no new file
				if (thumbnailFile == null || thumbnailFile.isEmpty()) {
					product.setThumbnail(existingProduct.getThumbnail());
				}
			}

			// Auto-assign current user as owner
			product.setOwnerUser(currentUser);

			// Handle thumbnail upload
			if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
				String imageUrl = cloudinaryService.uploadImage(thumbnailFile);
				product.setThumbnail(imageUrl);	
			}

			productService.saveProduct(product);
			redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được lưu thành công!");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu sản phẩm: " + e.getMessage());
		}

		return "redirect:/vendor/products";
	}

	@GetMapping("/products/delete/{id}")
	public String deleteProduct(@PathVariable String id, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			User currentUser = getCurrentUser(authentication);
			Product product = productService.getProductById(id)
					.orElseThrow(() -> new RuntimeException("Product not found"));

			// Check ownership
			if (!isOwner(product, currentUser)) {
				throw new RuntimeException("You don't have permission to delete this product");
			}

			// Delete thumbnail from Cloudinary if exists
			if (product.getThumbnail() != null && !product.getThumbnail().isEmpty()) {
				try {
					cloudinaryService.deleteImage(product.getThumbnail());
				} catch (Exception e) {
					// Log but continue with deletion
					System.err.println("Failed to delete image from Cloudinary: " + e.getMessage());
				}
			}

			productService.deleteProduct(id);
			redirectAttributes.addFlashAttribute("success", "Sản phẩm đã được xóa thành công!");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa sản phẩm: " + e.getMessage());
		}
		return "redirect:/vendor/products";
	}

	// ========== SPECIFICATION MANAGEMENT ==========
	@GetMapping("/specifications/product/{productId}")
	public String listSpecifications(@PathVariable String productId, Authentication authentication, Model model) {
		User currentUser = getCurrentUser(authentication);
		Product product = productService.getProductById(productId)
				.orElseThrow(() -> new RuntimeException("Product not found"));

		// Check ownership
		if (!isOwner(product, currentUser)) {
			throw new RuntimeException("You don't have permission to view specifications for this product");
		}

		List<Specification> specifications = specificationService.getSpecificationsByProduct(product);

		model.addAttribute("product", product);
		model.addAttribute("specifications", specifications);
		model.addAttribute("specCount", specifications.size());

		return "vendor/specifications-by-product";
	}

	@GetMapping("/specifications/new/{productID}")
	public String newSpecificationFormForProduct(@PathVariable String productID, Model model) {
		Product product = productService.getProductById(productID)
				.orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

		Specification specification = new Specification();
		specification.setProduct(product);

		model.addAttribute("specification", specification);
		model.addAttribute("product", product);

		return "vendor/specification-form";
	}

	@GetMapping("/specifications/edit/{id}")
	public String editSpecificationForm(@PathVariable String id, Authentication authentication, Model model) {
		User currentUser = getCurrentUser(authentication);
		Specification specification = specificationService.getSpecificationByID(id)
				.orElseThrow(() -> new RuntimeException("Specification not found"));

		// Check ownership through product
		if (!isOwner(specification.getProduct(), currentUser)) {
			throw new RuntimeException("You don't have permission to edit this specification");
		}

		model.addAttribute("specification", specification);
		model.addAttribute("product", specification.getProduct());

		return "vendor/specification-form";
	}

	@PostMapping("/specifications/save")
	public String saveSpecification(@ModelAttribute Specification specification,
			@RequestParam(required = false) String productID, RedirectAttributes redirectAttributes) {
		try {
			// Nếu có productID từ form, set product
			if (productID != null && !productID.isEmpty()) {
				Product product = productService.getProductById(productID)
						.orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));
				specification.setProduct(product);
			}

			specificationService.saveSpecification(specification);
			redirectAttributes.addFlashAttribute("success", "Thông số kỹ thuật đã được lưu thành công!");

			// Redirect về trang specifications của product nếu có
			if (specification.getProduct() != null) {
				return "redirect:/vendor/specifications/product/" + specification.getProduct().getProductID();
			}

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi lưu thông số: " + e.getMessage());
		}

		return "redirect:/vendor/specifications";
	}

	@GetMapping("/specifications/delete/{id}")
	public String deleteSpecification(@PathVariable String id, Authentication authentication,
			RedirectAttributes redirectAttributes) {
		try {
			User currentUser = getCurrentUser(authentication);
			Specification specification = specificationService.getSpecificationByID(id)
					.orElseThrow(() -> new RuntimeException("Specification not found"));

			String productId = specification.getProduct().getProductID();

			// Check ownership
			if (!isOwner(specification.getProduct(), currentUser)) {
				throw new RuntimeException("You don't have permission to delete this specification");
			}

			specificationService.deleteSpecification(id);
			redirectAttributes.addFlashAttribute("success", "Thông số kỹ thuật đã được xóa thành công!");

			return "redirect:/vendor/specifications/product/" + productId;

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa thông số: " + e.getMessage());
			return "redirect:/vendor/products";
		}
	}

	// ========== ORDER MANAGEMENT ==========
	@GetMapping("/orders")
	public String listOrders(@RequestParam(required = false) Integer status,
			@RequestParam(required = false) String search, @RequestParam(defaultValue = "0") int page,
			@RequestParam(defaultValue = "10") int size, Authentication authentication, Model model) {

		User currentUser = getCurrentUser(authentication);

		// Get orders that contain vendor's products
		List<Order> allOrders;
		if (status != null) {
			allOrders = orderService.getOrdersByVendorIdAndStatus(currentUser.getUserID(), status);
		} else {
			allOrders = orderService.getOrdersByVendorId(currentUser.getUserID());
		}
		if (status != null) {
			allOrders = orderService.getOrdersByVendorIdAndStatus(currentUser.getUserID(), status);
			System.out.println("Số lượng đơn hàng với trạng thái " + status + ": " + allOrders.size());
		} else {
			allOrders = orderService.getOrdersByVendorId(currentUser.getUserID());
			System.out.println("Số lượng đơn hàng tổng: " + allOrders.size());
		}

		// Apply search filter by orderID, username, or shippingPhone
		if (search != null && !search.trim().isEmpty()) {
			String searchLower = search.toLowerCase().trim();
			allOrders = allOrders.stream()
					.filter(o -> o.getOrderID().toLowerCase().contains(searchLower)
							|| (o.getUser() != null && o.getUser().getUsername() != null
									&& o.getUser().getUsername().toLowerCase().contains(searchLower))
							|| (o.getAddress().getPhone() != null
									&& o.getAddress().getPhone().toLowerCase().contains(searchLower)))
					.collect(Collectors.toList());
			System.out.println("Số lượng đơn hàng sau tìm kiếm: " + allOrders.size());
		}

		// Manual pagination
		int start = Math.min(page * size, allOrders.size());
		int end = Math.min(start + size, allOrders.size());
		List<Order> pagedOrders = allOrders.subList(start, end);

		Pageable pageable = PageRequest.of(page, size);
		org.springframework.data.domain.PageImpl<Order> pageImpl = new org.springframework.data.domain.PageImpl<>(
				pagedOrders, pageable, allOrders.size());

		// Count by status
		Map<String, Long> statusCounts = new java.util.HashMap<>();
		statusCounts.put("ALL", orderService.countOrdersByVendorId(currentUser.getUserID()));
		statusCounts.put("PENDING",
				orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(), OrderStatusHelper.STATUS_PENDING));
		statusCounts.put("CONFIRMED", orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_CONFIRMED));
		statusCounts.put("PREPARING", orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_PREPARING));
		statusCounts.put("PACKED",
				orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(), OrderStatusHelper.STATUS_PACKED));
		statusCounts.put("SHIPPED",
				orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(), OrderStatusHelper.STATUS_SHIPPED));
		statusCounts.put("DELIVERED", orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_DELIVERED));
		statusCounts.put("CANCELLED", orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_CANCELLED));
		statusCounts.put("RETURNED", orderService.countOrdersByVendorIdAndStatus(currentUser.getUserID(),
				OrderStatusHelper.STATUS_RETURNED));

		model.addAttribute("orders", pagedOrders);
		model.addAttribute("page", pageImpl);
		model.addAttribute("statusCounts", statusCounts);
		model.addAttribute("currentStatus", status != null ? OrderStatusHelper.getOrderStatusLabel(status) : "ALL");
		model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);

		return "vendor/orders";
	}

	@GetMapping("/orders/{id}")
	public String viewOrderDetail(@PathVariable String id, Authentication authentication, Model model) {
		User currentUser = getCurrentUser(authentication);

		Order order = orderService.getOrderById(id).orElseThrow(() -> new RuntimeException("Order not found"));

		// Get only items from this vendor
		List<OrderItem> vendorItems = orderService.getOrderItemsByOrderIdAndVendorId(id, currentUser.getUserID());

		if (vendorItems.isEmpty()) {
			throw new RuntimeException("You don't have permission to view this order");
		}

		// Calculate vendor's subtotal
		BigDecimal vendorSubtotal = vendorItems.stream()
				.map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
				.reduce(BigDecimal.ZERO, BigDecimal::add);

		model.addAttribute("order", order);
		model.addAttribute("vendorItems", vendorItems);
		model.addAttribute("vendorSubtotal", vendorSubtotal);
		model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
		model.addAttribute("currentUser", currentUser);

		return "vendor/order-detail";
	}

	@PostMapping("/orders/update-status")
	public String updateOrderStatus(@RequestParam String orderId, @RequestParam Integer newItemStatus,
			Authentication authentication, RedirectAttributes redirectAttributes) {

		try {
			User currentUser = getCurrentUser(authentication);

			// Update only vendor's items status in the order
			// newItemStatus should be ITEM_STATUS constant (ITEM_STATUS_CONFIRMED,
			// ITEM_STATUS_SHIPPED, etc.)
			orderService.updateVendorOrderItemsStatus(orderId, currentUser.getUserID(), newItemStatus);
			redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái sản phẩm của bạn thành công!");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
		}

		return "redirect:/vendor/orders/" + orderId;
	}

	@PostMapping("/orders/cancel")
	public String cancelOrder(@RequestParam String orderId, Authentication authentication,
			RedirectAttributes redirectAttributes) {

		try {
			User currentUser = getCurrentUser(authentication);

			// Cancel only vendor's items in the order
			orderService.cancelVendorOrderItems(orderId, currentUser.getUserID());
			redirectAttributes.addFlashAttribute("success", "Đã hủy sản phẩm của bạn trong đơn hàng!");

		} catch (Exception e) {
			redirectAttributes.addFlashAttribute("error", "Lỗi khi hủy: " + e.getMessage());
		}

		return "redirect:/vendor/orders/" + orderId;
	}

	@PostMapping("/orders/update-item-status/{orderId}/{orderItemId}")
	public String updateItemStatus(
	        @PathVariable String orderId,
	        @PathVariable String orderItemId,
	        @RequestParam Integer newStatus,
	        Authentication authentication,
	        RedirectAttributes redirectAttributes) {

	    try {
	        User currentUser = getCurrentUser(authentication);

	        orderService.updateVendorItemStatus(orderId, orderItemId, currentUser.getUserID(), newStatus);

	        redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái sản phẩm thành công!");
	    } catch (Exception e) {
	        redirectAttributes.addFlashAttribute("error", "Lỗi khi cập nhật trạng thái: " + e.getMessage());
	    }

	    return "redirect:/vendor/orders/" + orderId;
	}

    
    @PostMapping("/orders/cancel")
    public String cancelOrder(
            @RequestParam String orderId,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            
            // Cancel only vendor's items in the order
            orderService.cancelVendorOrderItems(orderId, currentUser.getUserID());
            redirectAttributes.addFlashAttribute("success", "Đã hủy sản phẩm của bạn trong đơn hàng!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi hủy: " + e.getMessage());
        }
        
        return "redirect:/vendor/orders/" + orderId;
    }

	@GetMapping("/revenue")
	public String viewRevenue(@RequestParam(required = false) String period,
			@RequestParam(required = false) String startDate, @RequestParam(required = false) String endDate,
			Authentication authentication, Model model) {

		User currentUser = getCurrentUser(authentication);

		// Default to last 30 days
		java.time.LocalDateTime start;
		java.time.LocalDateTime end = java.time.LocalDateTime.now();

		if (period != null) {
			switch (period) {
			case "today":
				start = java.time.LocalDateTime.now().toLocalDate().atStartOfDay();
				break;
			case "week":
				start = java.time.LocalDateTime.now().minusWeeks(1);
				break;
			case "month":
				start = java.time.LocalDateTime.now().minusMonths(1);
				break;
			case "year":
				start = java.time.LocalDateTime.now().minusYears(1);
				break;
			case "custom":
				if (startDate != null && endDate != null) {
					start = java.time.LocalDate.parse(startDate).atStartOfDay();
					end = java.time.LocalDate.parse(endDate).atTime(23, 59, 59);
				} else {
					start = java.time.LocalDateTime.now().minusMonths(1);
				}
				break;
			default:
				start = java.time.LocalDateTime.now().minusMonths(1);
			}
		} else {
			start = java.time.LocalDateTime.now().minusMonths(1);
			period = "month";
		}

		Map<String, Object> statistics = orderService.getVendorOrderStatistics(currentUser.getUserID(), start, end);

		model.addAttribute("statistics", statistics);
		model.addAttribute("period", period);
		model.addAttribute("startDate", startDate);
		model.addAttribute("endDate", endDate);

		return "vendor/revenue";
	}
	
	@GetMapping("/register")
    public String showVendorRegisterOtp(Authentication authentication, Model model, RedirectAttributes ra, HttpServletRequest req) {
        User currentUser = getCurrentUser(authentication);

        
        java.util.List<String> roles = userService.getRolesByUserId(currentUser.getUserID());
        boolean alreadyVendor = roles.stream().anyMatch(r -> "VENDOR".equalsIgnoreCase(r));

        if (alreadyVendor) {
            ra.addFlashAttribute("info", "Tài khoản của bạn đã là Vendor.");
            return "redirect:/";
        }

        String email = currentUser.getEmail();
        if (email == null || email.isBlank()) {
            ra.addFlashAttribute("error", "Tài khoản chưa có email để nhận OTP. Vui lòng cập nhật email trước.");
            return "redirect:/user/profile";
        }

        HttpSession session = req.getSession(true);

        // Chống spam gửi OTP theo cooldown
        Instant last = (Instant) session.getAttribute(SK_VR_LAST_SEND);
        if (last != null && Duration.between(last, Instant.now()).getSeconds() < VR_RESEND_COOLDOWN) {
            long left = VR_RESEND_COOLDOWN - Duration.between(last, Instant.now()).getSeconds();
            ra.addFlashAttribute("error", "Vui lòng chờ " + left + " giây nữa để gửi lại OTP.");
            return "redirect:/vendor/register";
        }

        // Sinh OTP + set session
        String otp = generateOtp();
        Instant expire = Instant.now().plus(Duration.ofMinutes(VR_OTP_TTL_MIN));

        session.setAttribute(SK_VR_OTP, otp);
        session.setAttribute(SK_VR_EXPIRE, expire);
        session.setAttribute(SK_VR_LAST_SEND, Instant.now());

        // Gửi email OTP – tận dụng EmailUtil bạn đã có
        try {
            // Có thể dùng template verify/forgot của bạn. Ví dụ dùng verify:
            emailUtil.sendVerifyOtp(email, currentUser.getUsername(), otp, VR_OTP_TTL_MIN);
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể gửi email OTP. Vui lòng thử lại.");
            // Xóa session OTP để an toàn
            clearVrSession(session);
            return "redirect:/";
        }

        // Hiển thị trang nhập OTP
        model.addAttribute("maskedEmail", maskEmail(email));
        model.addAttribute("ttlSeconds", VR_OTP_TTL_MIN * 60);
        model.addAttribute("cooldown", VR_RESEND_COOLDOWN);
        return "vendor/register-otp";
    }

    // ====== 2) POST /vendor/register – xác thực OTP + đổi role + ép đăng nhập lại ======
    @PostMapping("/register")
    public String handleVendorRegisterOtp(
            @RequestParam("otp") String otp,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes ra) {

        User currentUser = getCurrentUser(authentication);
        HttpSession session = request.getSession(false);

        if (session == null) {
            ra.addFlashAttribute("error", "Phiên OTP không hợp lệ. Vui lòng thử lại.");
            return "redirect:/vendor/register";
        }

        String saved = (String) session.getAttribute(SK_VR_OTP);
        Instant expireAt = (Instant) session.getAttribute(SK_VR_EXPIRE);

        if (saved == null || expireAt == null) {
            ra.addFlashAttribute("error", "OTP không tồn tại hoặc phiên đã hết hạn.");
            clearVrSession(session);
            return "redirect:/vendor/register";
        }
        if (Instant.now().isAfter(expireAt)) {
            ra.addFlashAttribute("error", "OTP đã hết hạn. Vui lòng gửi lại.");
            clearVrSession(session);
            return "redirect:/vendor/register";
        }
        if (otp == null || !otp.trim().equals(saved)) {
            ra.addFlashAttribute("error", "OTP không đúng. Vui lòng kiểm tra lại.");
            return "redirect:/vendor/register";
        }

        // OTP hợp lệ → đổi role sang VENDOR
        try {
            // Nếu muốn "chỉ còn" VENDOR thì triển khai như promoteToVendor(...) xóa role cũ
            // Nếu muốn "giữ cả USER + VENDOR" thì chỉ add role VENDOR nếu chưa có:
        	userService.promoteToVendor(currentUser.getUserID());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Không thể cập nhật quyền Vendor: " + e.getMessage());
            return "redirect:/vendor/register";
        } finally {
            clearVrSession(session);
        }

        // Ép đăng nhập lại: revoke/clear cookies + session → chuyển tới /auth/login
        clearAuthCookies(request, response);
        session.invalidate();
        org.springframework.security.core.context.SecurityContextHolder.clearContext();

        ra.addFlashAttribute("success", "Đăng ký shop thành công! Vui lòng đăng nhập lại để áp dụng quyền Vendor.");
        return "redirect:/auth/login";
    }

    // ====== 3) POST /vendor/register/resend – gửi lại OTP ======
    @PostMapping("/register/resend")
    public String resendVendorOtp(Authentication authentication, HttpServletRequest req, RedirectAttributes ra) {
        User currentUser = getCurrentUser(authentication);
        HttpSession session = req.getSession(true);

        Instant last = (Instant) session.getAttribute(SK_VR_LAST_SEND);
        if (last != null && Duration.between(last, Instant.now()).getSeconds() < VR_RESEND_COOLDOWN) {
            long left = VR_RESEND_COOLDOWN - Duration.between(last, Instant.now()).getSeconds();
            ra.addFlashAttribute("error", "Vui lòng chờ " + left + " giây nữa để gửi lại OTP.");
            return "redirect:/vendor/register";
        }

        String otp = generateOtp();
        Instant expire = Instant.now().plus(Duration.ofMinutes(VR_OTP_TTL_MIN));
        session.setAttribute(SK_VR_OTP, otp);
        session.setAttribute(SK_VR_EXPIRE, expire);
        session.setAttribute(SK_VR_LAST_SEND, Instant.now());

        try {
            emailUtil.sendVerifyOtp(currentUser.getEmail(), currentUser.getUsername(), otp, VR_OTP_TTL_MIN);
        } catch (Exception ex) {
            ra.addFlashAttribute("error", "Không thể gửi lại OTP lúc này. Vui lòng thử lại.");
            return "redirect:/vendor/register";
        }

        ra.addFlashAttribute("success", "Đã gửi lại OTP đến email của bạn.");
        return "redirect:/vendor/register";
    }

	
	// ========== API ENDPOINTS ==========
	@GetMapping("/api/subcategories/category/{categoryID}")
	@ResponseBody
	public List<Subcategory> getSubcategoriesByCategory(@PathVariable String categoryID) {
		return subcategoryService.getSubcategoriesByCategoryId(categoryID);
	}

	private static String generateOtp() {
		return String.format("%06d", new java.util.Random().nextInt(1_000_000));
	}

	private static String maskEmail(String email) {
		if (email == null || !email.contains("@"))
			return "email của bạn";
		String[] parts = email.split("@", 2);
		String local = parts[0];
		if (local.length() <= 2)
			return local.charAt(0) + "***@" + parts[1];
		return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + parts[1];
	}

	private static void clearVrSession(HttpSession s) {
		s.removeAttribute(SK_VR_OTP);
		s.removeAttribute(SK_VR_EXPIRE);
		s.removeAttribute(SK_VR_LAST_SEND);
	}

	private void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
		CookieUtil.delete(request, response, CK_AT);
		CookieUtil.delete(request, response, CK_RT);
		CookieUtil.delete(request, response, CK_REMEMBER);

		// Xóa JSESSIONID (host-only)
		jakarta.servlet.http.Cookie js = new jakarta.servlet.http.Cookie("JSESSIONID", "");
		js.setPath("/");
		js.setHttpOnly(true);
		js.setMaxAge(0);
		response.addCookie(js);

		response.addHeader("Set-Cookie",
				"JSESSIONID=; Path=/; Max-Age=0; HttpOnly; SameSite=Lax; Expires=Thu, 01 Jan 1970 00:00:00 GMT");
	}
}
