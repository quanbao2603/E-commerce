package com.womtech.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.womtech.entity.Address;
import com.womtech.entity.User;
import com.womtech.service.AddressService;
import com.womtech.service.UserService;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;



@Controller
@RequestMapping("/profile")
public class ProfileController {
	@Autowired
	UserService userService;
	@Autowired
	AddressService addressService;
	
	@GetMapping("")
	public String showProfilePage(HttpSession session, Model model) {
//		String userId = (String) session.getAttribute("CURRENT_USER_ID");
//		String username = (String) session.getAttribute("CURRENT_USERNAME");
		String userId = "1";
		User user = userService.findById(userId).orElse(null);
		
		Address defaultAddress = addressService.findByUserAndIsDefaultTrue(user).orElse(null);
		List<Address> listAddress = addressService.findByUser(user);
		
		model.addAttribute("user", user);
		model.addAttribute("defaultAddress", defaultAddress);
		model.addAttribute("listAddress", listAddress);
		return "user/profile";
	}
	
	@PostMapping("/update")
	public String updateProfile(@RequestBody String entity) {
		
		return "redirect:/user/profile";
	}
	
	@PostMapping("/add-address")
	public String addAddress(HttpSession session,
							 @RequestParam String fullname,
							 @RequestParam String phone,
							 @RequestParam String street,
							 @RequestParam String ward,
							 @RequestParam String district,
							 @RequestParam String city
							 ) {
		addressService.save(Address.builder()
								.fullname(fullname)
								.phone(phone)
								.street(street)
								.ward(ward)
								.district(district)
								.city(city)
								.createAt(LocalDateTime.now())
								.updateAt(LocalDateTime.now())
								.build());
		return "redirect:/user/profile";
	}
	
	@PostMapping("/update-address")
	public String updateAddress(@ModelAttribute("address") Address address) {
		addressService.save(address);
		return "redirect:/user/profile";
	}
	
	
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam String currentPassword,
								 @RequestParam String newPassword,
								 @RequestParam String confirmPassword) {
		
		return "redirect:/user/profile";
	}
	
}
