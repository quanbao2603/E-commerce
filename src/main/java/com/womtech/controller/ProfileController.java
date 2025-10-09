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

@Controller
@RequestMapping("/user")
@RequiredArgsConstructor
public class ProfileController {

	private final UserService userService;
	private final AddressService addressService;

	@GetMapping("/profile")
	public String showProfilePage(HttpSession session, Model model, Principal principal) {
		String userId = resolveCurrentUserId(principal, session);
		if (userId == null)
			return "redirect:/auth/login";

		Optional<User> userOpt = userService.findById(userId);
		if (userOpt.isEmpty()) {
			session.invalidate();
			return "redirect:/auth/login";
		}
		User user = userOpt.get();

		Address defaultAddress = addressService.findByUserAndIsDefaultTrue(user).orElse(null);
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
		String userId = resolveCurrentUserId(principal, session);
		if (userId == null)
			return "redirect:/auth/login";

		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();

		user.setEmail(email);
		user.setUsername(username);
		userService.save(user);

		return "redirect:/user/profile";
	}

	@PostMapping("/add-address")
	public String addAddress(HttpSession session, Principal principal, @RequestParam String fullname,
			@RequestParam String phone, @RequestParam String street, @RequestParam String ward,
			@RequestParam String district, @RequestParam String city,
			@RequestParam(name = "isDefault", defaultValue = "false") boolean isDefault) {

		String userId = resolveCurrentUserId(principal, session);
		if (userId == null)
			return "redirect:/auth/login";

		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();

		if (isDefault) {
			addressService.unsetDefaultForUser(user); // cần có trong AddressService/Repository
		}

		Address addr = Address.builder().user(user) // QUAN TRỌNG: gán user
				.fullname(fullname).phone(phone).street(street).ward(ward).district(district).city(city)
				.isDefault(isDefault).createAt(LocalDateTime.now()).updateAt(LocalDateTime.now()).build();

		addressService.save(addr);
		return "redirect:/user/profile";
	}

	@PostMapping("/update-address")
	public String updateAddress(HttpSession session, Principal principal, @ModelAttribute("address") Address address) {
		String userId = resolveCurrentUserId(principal, session);
		if (userId == null)
			return "redirect:/auth/login";

		var userOpt = userService.findById(userId);
		if (userOpt.isEmpty())
			return "redirect:/auth/login";
		var user = userOpt.get();

		address.setUser(user);
		address.setUpdateAt(LocalDateTime.now());

		if (Boolean.TRUE.equals(address.isDefault())) {
			addressService.unsetDefaultForUser(user);
		}

		addressService.save(address);
		return "redirect:/user/profile";
	}

	@PostMapping("/change-password")
	public String changePassword(HttpSession session, Principal principal, @RequestParam String currentPassword,
			@RequestParam String newPassword, @RequestParam String confirmPassword) {
		String userId = resolveCurrentUserId(principal, session);
		if (userId == null)
			return "redirect:/auth/login";

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

	private String resolveCurrentUserId(Principal principal, HttpSession session) {
		if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
			return principal.getName(); // JwtAuthFilter set principal = userId
		}
		Object sid = session.getAttribute("CURRENT_USER_ID");
		return (sid != null) ? sid.toString() : null;
	}
}
