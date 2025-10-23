package com.womtech.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.womtech.entity.*;
import com.womtech.service.*;
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
	private final VoucherService voucherService;
	private final CartVoucherService cartVoucherService;
	private final AuthUtils authUtils;
	
	@GetMapping("")
	public String showCheckoutPage(HttpSession session, Model model, Principal principal) {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}    
		User user = userOpt.get();
		
		// Cart
		Cart cart = cartService.findByUser(user);
		if (cart.getItems().isEmpty())
			return "redirect:/cart";
		
		// Address
		Optional<Address> defaultAddressOpt = addressService.findByUserAndIsDefaultTrue(user);
		List<Address> addresses = addressService.findByUser(user);
		
		// Price
		BigDecimal totalPrice = cartService.totalPrice(cart);
		
		// Tính toán vouchers
		cartVoucherService.applyVouchersToCart(cart, totalPrice);
		
		List<CartVoucher> cartVouchers = cartVoucherService.findByCart(cart);
		BigDecimal totalDiscountPrice = cartVoucherService.getTotalDiscountPrice(cart);
		BigDecimal finalPrice = totalPrice.subtract(totalDiscountPrice);
		
		for (CartVoucher cv : cartVouchers) {
			if (cv.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0)
				model.addAttribute("voucherError", "Một số mã giảm giá chưa đủ điều kiện áp dụng");
		}
		
		// Model
		model.addAttribute("cart", cart);
		model.addAttribute("defaultAddress", defaultAddressOpt.get());
		model.addAttribute("addresses", addresses);
		model.addAttribute("totalPrice", totalPrice);
		model.addAttribute("cartVouchers", cartVouchers);
		model.addAttribute("totalDiscountPrice", totalDiscountPrice);
		model.addAttribute("finalPrice", finalPrice);
		return "/user/checkout";
	}
	
	@PostMapping("/add-voucher")
	public String addVoucher(HttpSession session, Model model, Principal principal,
							 RedirectAttributes ra,
							 @RequestParam String voucherCode) {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}    
		User user = userOpt.get();
		
		// Cart
		Cart cart = cartService.findByUser(user);
		if (cart.getItems().isEmpty())
			return "redirect:/cart";
        
		// Price
		BigDecimal totalPrice = cartService.totalPrice(cart);
        
		// Add Voucher
        Optional<Voucher> voucherOpt = voucherService.findByCode(voucherCode.trim());
        if (voucherOpt.isEmpty()) {
            ra.addFlashAttribute("voucherError", "Mã giảm giá không hợp lệ");
            return "redirect:/checkout";
        }
        Voucher voucher = voucherOpt.get();
        
        if (!voucherService.isValid(voucher)) {
            ra.addFlashAttribute("voucherError", "Mã giảm giá đã hết hạn hoặc không còn hiệu lực");
            return "redirect:/checkout";
        }
        
        if (!voucherService.isUsable(voucher, user)) {
        	ra.addFlashAttribute("voucherError", "Bạn đã sử dụng mã giảm giá này rồi");
            return "redirect:/checkout";
        }
        
        if (!voucherService.isApplicable(voucher, totalPrice))
        	ra.addFlashAttribute("voucherError", "Mã giảm giá chưa đủ điều kiện để áp dụng");
        
        cartVoucherService.addVoucherToCart(cart, voucher);
		return "redirect:/checkout";
	}
	
	@PostMapping("/remove-voucher")
	public String removeVoucher(HttpSession session, Model model, Principal principal,
							 RedirectAttributes ra,
							 @RequestParam String voucherCode) {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}    
		User user = userOpt.get();
		
		// Cart
		Cart cart = cartService.findByUser(user);
		if (cart.getItems().isEmpty())
			return "redirect:/cart";
        
		// Remove Voucher
        Optional<Voucher> voucherOpt = voucherService.findByCode(voucherCode.trim());
        if (voucherOpt.isEmpty()) {
            ra.addFlashAttribute("voucherError", "Mã giảm giá không hợp lệ");
            return "redirect:/checkout";
        }
        Voucher voucher = voucherOpt.get();
        
		CartVoucherID cartVoucherID = new CartVoucherID(cart.getCartID(), voucher.getVoucherID());
        cartVoucherService.deleteById(cartVoucherID);
		return "redirect:/checkout";
	}
	
	@PostMapping("/confirm")
	public String processCheckout(HttpSession session, Model model, Principal principal,
								  @RequestParam("selectedAddressId") String addressID,
								  @RequestParam String payment_method,
								  @RequestParam String voucherCode) throws Exception {
		Optional<User> userOpt = authUtils.getCurrentUser(principal);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		User user = userOpt.get();
		
		Address address = addressService.findById(addressID).orElse(null);
		Order order = orderService.createOrder(user, address, payment_method, voucherCode);
		
		return "redirect:/order/" + order.getOrderID();
	}
}
