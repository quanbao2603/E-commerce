package com.womtech.controller;

import com.womtech.entity.Order;
import com.womtech.entity.User;
import com.womtech.service.DeliveryProofService;
import com.womtech.service.OrderService;
import com.womtech.service.UserService;
import com.womtech.util.AuthUtils;
import com.womtech.util.OrderStatusHelper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
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
    private final DeliveryProofService deliveryProofService;

    // NEW: dùng lại util đã có trong dự án bạn (như OrderController)
    private final AuthUtils authUtils;

    private static final BigDecimal PER_ORDER_REWARD = new BigDecimal("15000");

    // ===== Helpers =====
    private Optional<User> currentUserOpt(Principal principal) {
        return authUtils.getCurrentUser(principal); // đã dùng ổn ở OrderController
    }

    private User requireCurrentUserOrRedirect(Principal principal, RedirectAttributes ra) {
        return currentUserOpt(principal).orElseThrow(() -> new IllegalStateException("AUTH_REDIRECT"));
    }

    private void assertOrderBelongsToShipper(Order o, String shipperId) {
        if (o == null || o.getShipper() == null || o.getShipper().getUserID() == null
                || !o.getShipper().getUserID().equals(shipperId)) {
            throw new IllegalStateException("Bạn không có quyền trên đơn này.");
        }
    }

    private long countByStatus(List<Order> orders, Integer status) {
        if (orders == null || orders.isEmpty()) return 0;
        return orders.stream().filter(o -> Objects.equals(o.getStatus(), status)).count();
    }

    private BigDecimal sumAllOrdersExceptCancelled(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return BigDecimal.ZERO;
        return orders.stream()
                .filter(o -> !Objects.equals(o.getStatus(), OrderStatusHelper.STATUS_CANCELLED))
                .map(o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumCODCollected(List<Order> orders) {
        if (orders == null || orders.isEmpty()) return BigDecimal.ZERO;
        return orders.stream()
                .filter(o -> o.getPaymentMethod() != null && o.getPaymentMethod().toUpperCase().contains("COD"))
                .filter(o -> Objects.equals(o.getPaymentStatus(), 1))
                .filter(o -> !Objects.equals(o.getStatus(), OrderStatusHelper.STATUS_CANCELLED)
                        && !Objects.equals(o.getStatus(), OrderStatusHelper.STATUS_RETURNED))
                .map(o -> o.getTotalPrice() == null ? BigDecimal.ZERO : o.getTotalPrice())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ===== Dashboard =====
    @GetMapping("/dashboard")
    public String dashboard(Model model, Principal principal, RedirectAttributes ra) {
        try {
            User current = requireCurrentUserOrRedirect(principal, ra);

            List<Order> all = orderService.getOrdersByShipper(current.getUserID());
            long assignedCount = all.size();
            long inTransitCount = countByStatus(all, OrderStatusHelper.STATUS_SHIPPED);
            long deliveredCount = countByStatus(all, OrderStatusHelper.STATUS_DELIVERED);
            long failedCount = countByStatus(all, OrderStatusHelper.STATUS_CANCELLED)
                    + countByStatus(all, OrderStatusHelper.STATUS_RETURNED);

            BigDecimal codToCollect = sumAllOrdersExceptCancelled(all);
            BigDecimal codCollected = sumCODCollected(all);

            List<Order> recentOrders = all.stream()
                    .sorted(Comparator.comparing((Order o) ->
                                    Optional.ofNullable(o.getUpdateAt()).orElse(o.getCreateAt()),
                            Comparator.nullsLast(Comparator.naturalOrder()))
                            .reversed())
                    .limit(5)
                    .collect(Collectors.toList());

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
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }

    // ===== Danh sách giao hàng =====
    @GetMapping("/deliveries")
    public String deliveries(@RequestParam(required = false) Integer status,
                             @RequestParam(required = false, name = "search") String search,
                             @RequestParam(required = false) String startDate,
                             @RequestParam(required = false) String endDate,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(defaultValue = "10") int size,
                             Principal principal, Model model, RedirectAttributes ra) {

        try {
            User current = requireCurrentUserOrRedirect(principal, ra);

            List<Order> orders = (status != null)
                    ? orderService.getOrdersByShipperAndStatus(current.getUserID(), status)
                    : orderService.getOrdersByShipper(current.getUserID());

            LocalDateTime from = (startDate != null && !startDate.isBlank())
                    ? LocalDate.parse(startDate).atStartOfDay() : null;
            LocalDateTime to = (endDate != null && !endDate.isBlank())
                    ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

            if (from != null || to != null) {
                orders = orders.stream().filter(o -> {
                    LocalDateTime t = o.getCreateAt();
                    if (t == null) return false;
                    if (from != null && t.isBefore(from)) return false;
                    if (to != null && t.isAfter(to)) return false;
                    return true;
                }).collect(Collectors.toList());
            }

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

            int start = Math.min(page * size, orders.size());
            int end = Math.min(start + size, orders.size());
            List<Order> pageContent = orders.subList(start, end);
            Pageable pageable = PageRequest.of(page, size);
            PageImpl<Order> pageImpl = new PageImpl<>(pageContent, pageable, orders.size());

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
            model.addAttribute("search", search);
            return "shipper/deliveries";
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/shipper/dashboard";
        }
    }

    // ===== Chi tiết đơn giao =====
    @GetMapping("/deliveries/{orderId}")
    public String deliveryDetail(@PathVariable String orderId, Principal principal, Model model, RedirectAttributes ra) {
        try {
            User current = requireCurrentUserOrRedirect(principal, ra);
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng"));
            assertOrderBelongsToShipper(order, current.getUserID());

            model.addAttribute("order", order);
            model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
            model.addAttribute("currentUser", current);
            return "shipper/delivery-detail";
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/shipper/deliveries";
        }
    }

    // ===== Upload ảnh minh chứng giao hàng (POD) =====
    @PostMapping("/deliveries/{orderId}/upload-proof")
    @PreAuthorize("hasRole('SHIPPER')")
    public String uploadProof(@PathVariable String orderId,
                              @RequestParam("image") MultipartFile image,
                              @RequestParam(value = "markDelivered", defaultValue = "true") boolean markDelivered,
                              Principal principal,
                              RedirectAttributes ra) {
        try {
            User current = requireCurrentUserOrRedirect(principal, ra);

            // Bảo vệ: chỉ cho shipper phụ trách đơn
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng"));
            assertOrderBelongsToShipper(order, current.getUserID());

            // Truyền username hiện tại cho service (khớp UserRepository.findByUsername)
            deliveryProofService.uploadProof(orderId, current.getUsername(), image, markDelivered);

            ra.addFlashAttribute("success", "Tải ảnh minh chứng thành công.");
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
        } catch (SecurityException se) {
            ra.addFlashAttribute("error", "Không có quyền: " + se.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", "Lỗi không xác định khi upload ảnh.");
        }
        return "redirect:/shipper/deliveries/" + orderId;
    }

    // ===== Chuyển trạng thái khi giao (+ auto set payment COD) =====
    @PostMapping("/deliveries/{orderId}/transition")
    public String transition(@PathVariable String orderId,
                             @RequestParam("targetStatus") Integer targetStatus,
                             Principal principal, RedirectAttributes ra) {
        try {
            User current = requireCurrentUserOrRedirect(principal, ra);
            Order order = orderService.getOrderById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn hàng"));
            assertOrderBelongsToShipper(order, current.getUserID());

            Integer cur = order.getStatus();
            if (Objects.equals(cur, OrderStatusHelper.STATUS_CANCELLED)
                    || Objects.equals(cur, OrderStatusHelper.STATUS_DELIVERED)
                    || Objects.equals(cur, OrderStatusHelper.STATUS_RETURNED)) {
                throw new IllegalStateException("Đơn đã kết thúc, không thể chuyển trạng thái.");
            }

            if (Objects.equals(targetStatus, OrderStatusHelper.STATUS_SHIPPED)) {
                if (cur == null || cur < OrderStatusHelper.STATUS_PACKED || cur >= OrderStatusHelper.STATUS_SHIPPED) {
                    throw new IllegalStateException("Chỉ chuyển sang 'Đang giao' khi đơn đã Đóng gói và chưa Đang giao.");
                }
            } else if (Objects.equals(targetStatus, OrderStatusHelper.STATUS_DELIVERED)
                    || Objects.equals(targetStatus, OrderStatusHelper.STATUS_RETURNED)) {
                if (!Objects.equals(cur, OrderStatusHelper.STATUS_SHIPPED)) {
                    throw new IllegalStateException("Chỉ chuyển trạng thái kết thúc khi đơn đang giao.");
                }
            } else if (!Objects.equals(targetStatus, OrderStatusHelper.STATUS_CANCELLED)) {
                throw new IllegalStateException("Trạng thái đích không hợp lệ.");
            }

            orderService.updateOrderStatus(orderId, targetStatus);

            ra.addFlashAttribute("success", "Cập nhật trạng thái thành công.");
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/shipper/deliveries/" + orderId;
    }

    // ===== Xác nhận đã thu COD =====
    @PostMapping("/deliveries/{orderId}/confirm-cod")
    public String confirmCOD(@PathVariable String orderId, Principal principal, RedirectAttributes ra) {
        try {
            User current = requireCurrentUserOrRedirect(principal, ra);
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

            order.setUpdateAt(LocalDateTime.now());
            orderService.saveOrder(order);

            ra.addFlashAttribute("success", "Đã xác nhận thu COD.");
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
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
    public String stats(@RequestParam(required = false, name = "startDate") String startDate,
                        @RequestParam(required = false, name = "endDate") String endDate,
                        Principal principal, Model model, RedirectAttributes ra) {

        try {
            User current = requireCurrentUserOrRedirect(principal, ra);

            LocalDateTime end = (endDate != null && !endDate.isBlank())
                    ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                    : LocalDateTime.now();
            LocalDateTime start = (startDate != null && !startDate.isBlank())
                    ? LocalDate.parse(startDate).atStartOfDay()
                    : end.minusDays(30);

            List<Order> allAssigned = orderService.getOrdersByShipper(current.getUserID());

            List<Order> range = allAssigned.stream().filter(o -> {
                LocalDateTime t = o.getCreateAt();
                return t != null && !t.isBefore(start) && !t.isAfter(end);
            }).collect(Collectors.toList());

            Map<String, Long> totals = new HashMap<>();
            totals.put("assigned", (long) range.size());
            totals.put("shipped", countByStatus(range, OrderStatusHelper.STATUS_SHIPPED));
            totals.put("delivered", countByStatus(range, OrderStatusHelper.STATUS_DELIVERED));
            totals.put("returned", countByStatus(range, OrderStatusHelper.STATUS_RETURNED));

            List<Order> deliveredRange = range.stream()
                    .filter(o -> Objects.equals(o.getStatus(), OrderStatusHelper.STATUS_DELIVERED))
                    .collect(Collectors.toList());

            BigDecimal totalIncome = PER_ORDER_REWARD.multiply(BigDecimal.valueOf(deliveredRange.size()));

            Map<LocalDate, Long> deliveredPerDay = deliveredRange.stream()
                    .collect(Collectors.groupingBy(o -> o.getCreateAt().toLocalDate(), TreeMap::new, Collectors.counting()));

            Map<LocalDate, Long> returnedPerDay = range.stream()
                    .filter(o -> Objects.equals(o.getStatus(), OrderStatusHelper.STATUS_RETURNED))
                    .collect(Collectors.groupingBy(o -> o.getCreateAt().toLocalDate(), TreeMap::new, Collectors.counting()));

            List<String> dailyLabels = new ArrayList<>();
            List<Long> dailyDelivered = new ArrayList<>();
            List<Long> dailyReturned = new ArrayList<>();
            List<BigDecimal> dailyIncome = new ArrayList<>();

            LocalDate cur = start.toLocalDate();
            LocalDate ed = end.toLocalDate();
            while (!cur.isAfter(ed)) {
                long d = deliveredPerDay.getOrDefault(cur, 0L);
                long r = returnedPerDay.getOrDefault(cur, 0L);

                dailyLabels.add(cur.toString());
                dailyDelivered.add(d);
                dailyReturned.add(r);
                dailyIncome.add(PER_ORDER_REWARD.multiply(BigDecimal.valueOf(d)));

                cur = cur.plusDays(1);
            }

            model.addAttribute("currentUser", current);
            model.addAttribute("startDate", start.toLocalDate().toString());
            model.addAttribute("endDate", end.toLocalDate().toString());

            model.addAttribute("totals", totals);
            model.addAttribute("totalIncome", totalIncome);

            model.addAttribute("dailyLabels", dailyLabels);
            model.addAttribute("dailyDelivered", dailyDelivered);
            model.addAttribute("dailyReturned", dailyReturned);
            model.addAttribute("dailyIncome", dailyIncome);

            model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
            return "shipper/stats";
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/shipper/dashboard";
        }
    }

    @GetMapping("/help")
    public String shipperHelp(Model model, Principal principal, RedirectAttributes ra) {
        try {
            User current = requireCurrentUserOrRedirect(principal, ra);
            model.addAttribute("currentUser", current);
            model.addAttribute("OrderStatusHelper", OrderStatusHelper.class);
            return "shipper/help";
        } catch (IllegalStateException e) {
            if ("AUTH_REDIRECT".equals(e.getMessage())) return "redirect:/auth/login";
            ra.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
    }
}
