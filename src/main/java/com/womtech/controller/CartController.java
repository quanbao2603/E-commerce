package com.womtech.controller;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.womtech.entity.CartItem;
import com.womtech.entity.Product;
import com.womtech.entity.User;
import com.womtech.service.CartService;
import com.womtech.service.ProductService;
import com.womtech.service.UserService;
import com.womtech.util.AuthUtils;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;


@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
	private final UserService userService;
	private final CartService cartService;
	private final ProductService productService;
	private final AuthUtils authUtils;
	
	@GetMapping({"", "/"})
	public String showCart(HttpSession session, Model model, Principal principal) {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		User user = userOpt.get();
		
		List<CartItem> cartItems = cartService.findAllByUser(user);
		
		model.addAttribute("user", user);
		model.addAttribute("cartItems", cartItems);
		
		return "/user/cart";
	}
	
	@PostMapping("/add")
	public String postMethodName(HttpSession session, Model model, Principal principal,
								 @RequestParam String productID,
								 @RequestParam int quantity) throws Exception {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		User user = userOpt.get();
		
		Optional<Product> productOpt = productService.getProductById(productID);
		if (productOpt.isEmpty()) {
			throw new Exception("Không tìm thấy sản phẩm");
		}
		cartService.addToCart(user, productOpt.get(), quantity);
		
		return "redirect:/product/" + productID + "?added=true&quantity=" + quantity;
	}
	
}
