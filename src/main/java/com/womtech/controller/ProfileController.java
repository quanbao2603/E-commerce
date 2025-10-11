package com.womtech.controller;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.womtech.entity.Address;
import com.womtech.entity.User;
import com.womtech.service.AddressService;
import com.womtech.service.UserService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class ProfileController {

	private final UserService userService;
	private final AddressService addressService;

	@GetMapping("/profile")
	public String showProfilePage(HttpSession session, Model model, Principal principal) {
		// Chỉ dựa vào JWT authentication (Principal), không fallback về session
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "redirect:/auth/login";
		}

		String userId = principal.getName();
		Optional<User> userOpt = userService.findById(userId);
		if (userOpt.isEmpty()) {
			return "redirect:/auth/login";
		}
		User user = userOpt.get();

		Address defaultAddress = addressService.findByUserAndIsDefaultTrue(user).orElse(new Address());
		List<Address> listAddress = addressService.findByUser(user);

		boolean isAdmin = user.getRole() != null && user.getRole().getRolename() != null
				&& user.getRole().getRolename().equalsIgnoreCase("ADMIN");

		model.addAttribute("user", user);
		model.addAttribute("defaultAddress", defaultAddress);
		model.addAttribute("listAddress", listAddress);
		model.addAttribute("isAdmin", isAdmin);

		return "user/profile";
	}

	@PostMapping("/update")
	public String updateProfile(HttpSession session, Principal principal, @RequestParam String email,
			@RequestParam String username) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "redirect:/auth/login";
		}

		String userId = principal.getName();
		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();

		user.setEmail(email);
		user.setUsername(username);
		userService.save(user);

		return "redirect:/user/profile";
	}
	
	@PostMapping("/update-defaultAddress")
	public String updateDefaultAddress(HttpSession session, Principal principal, @ModelAttribute("defaultAddress") Address defaultAddress) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "redirect:/auth/login";
		}

		String userId = principal.getName();
		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();
		
		Optional<Address> addressDbOpt = addressService.findById(defaultAddress.getAddressID());
		
		if (addressDbOpt.isEmpty()) {
			defaultAddress.setAddressID(null);
			defaultAddress.setUser(user);
			defaultAddress.setCreateAt(LocalDateTime.now());
			defaultAddress.setUpdateAt(LocalDateTime.now());
			addressService.save(defaultAddress);
			addressService.setDefaultAddress(defaultAddress);
		} else {
			Address addressDb = addressDbOpt.get();
			addressDb.setFullname(defaultAddress.getFullname());
			addressDb.setPhone(defaultAddress.getPhone());
			addressDb.setStreet(defaultAddress.getStreet());
			addressDb.setWard(defaultAddress.getWard());
			addressDb.setDistrict(defaultAddress.getDistrict());
			addressDb.setCity(defaultAddress.getCity());
			addressDb.setUpdateAt(LocalDateTime.now());
		    addressService.save(addressDb);
		}
		
		return "redirect:/user/profile";
	}
	

	@PostMapping("/add-address")
	public String addAddress(HttpSession session, Principal principal, @RequestParam String fullname,
			@RequestParam String phone, @RequestParam String street, @RequestParam String ward,
			@RequestParam String district, @RequestParam String city,
			@RequestParam(name = "isDefault", defaultValue = "false") boolean isDefault) {

		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "redirect:/auth/login";
		}

		String userId = principal.getName();
		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();

//		if (isDefault) {
//			addressService.unsetDefaultForUser(user); 
//		}

		Address addr = Address.builder().user(user) 
				.fullname(fullname).phone(phone).street(street).ward(ward).district(district).city(city)
				.isDefault(isDefault).createAt(LocalDateTime.now()).updateAt(LocalDateTime.now()).build();

		addressService.save(addr);
		return "redirect:/user/profile";
	}

	@PostMapping("/update-address")
	public String updateAddress(HttpSession session, Principal principal, @ModelAttribute("address") Address address) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "redirect:/auth/login";
		}

		String userId = principal.getName();
		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();

		address.setUser(user);
		address.setUpdateAt(LocalDateTime.now());

//		if (Boolean.TRUE.equals(address.isDefault())) {
//			addressService.unsetDefaultForUser(user);
//		}

		addressService.save(address);
		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(HttpSession session, Principal principal, @RequestParam String currentPassword,
			@RequestParam String newPassword, @RequestParam String confirmPassword) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			return "redirect:/auth/login";
		}

		String userId = principal.getName();
		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();

		if (!newPassword.equals(confirmPassword)) {
			return "redirect:/user/profile?err=pwd_mismatch";
		}
		if (!com.womtech.util.PasswordUtil.matches(currentPassword, user.getPassword())) {
			return "redirect:/user/profile?err=pwd_wrong";
		}
		user.setPassword(com.womtech.util.PasswordUtil.encode(newPassword));
		userService.save(user);

		return "redirect:/user/profile?ok=pwd_changed";
	}

}
