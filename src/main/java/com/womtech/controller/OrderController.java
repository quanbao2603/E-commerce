package com.womtech.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.womtech.entity.Order;
import com.womtech.entity.User;
import com.womtech.service.OrderService;
import com.womtech.util.AuthUtils;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
	private final OrderService orderService;
	private final AuthUtils authUtils;
	
	@GetMapping("/{id}")
	public String showOrder(HttpSession session, Model model, Principal principal,
							@PathVariable String id) {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "/error/403";
		}
		User user = userOpt.get();
		
		Order order = orderService.findById(id).orElse(null);
		if (order == null || !order.getUser().equals(user))
			return "/error/403";
		
		model.addAttribute("order", order);
		return "/user/order";
	}
}
