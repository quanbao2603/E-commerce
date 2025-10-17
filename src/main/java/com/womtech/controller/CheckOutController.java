package com.womtech.controller;

import java.security.Principal;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.womtech.entity.Address;
import com.womtech.entity.Cart;
import com.womtech.entity.Order;
import com.womtech.entity.User;
import com.womtech.service.AddressService;
import com.womtech.service.CartService;
import com.womtech.service.OrderService;
import com.womtech.util.AuthUtils;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/checkout")
@RequiredArgsConstructor
public class CheckOutController {
	private final CartService cartService;
	private final AddressService addressService;
	private final OrderService orderService;
	private final AuthUtils authUtils;
	
	@GetMapping("")
	public String showCheckoutPage(HttpSession session, Model model, Principal principal) {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		User user = userOpt.get();
		
		Cart cart = cartService.findByUser(user);
		
		Optional<Address> defaultAddressOpt = addressService.findByUserAndIsDefaultTrue(user);
		
		model.addAttribute("cart", cart);
		model.addAttribute("defaultAddress", defaultAddressOpt.get());
		return "/user/checkout";
	}
	
	@PostMapping("")
	public String processCheckout(HttpSession session, Model model, Principal principal,
								  @RequestParam String addressID,
								  @RequestParam String payment_method) {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		User user = userOpt.get();
		
		Address address = addressService.findById(addressID).orElse(null);
		Order order = orderService.createOrder(user, address, payment_method);
		
		return "redirect:/order/" + order.getOrderID();
	}
}
