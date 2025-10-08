package com.womtech.controller;

import com.womtech.dto.request.auth.LoginRequest;
import com.womtech.dto.request.auth.RegisterRequest;
import com.womtech.dto.response.auth.LoginResponse;
import com.womtech.dto.response.auth.RegisterResponse;
import com.womtech.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    // ========== LOGIN ==========
    @GetMapping("/login")
    public String showLogin() {
        return "auth/login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam("username") String username,
                          @RequestParam("password") String password,
                          HttpSession session,
                          RedirectAttributes ra,
                          Model model) {

        // Validate cơ bản theo form
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ tên đăng nhập và mật khẩu.");
            return "auth/login";
        }

        // Uỷ quyền xử lý cho Service
        LoginResponse res = userService.login(
                LoginRequest.builder()
                        .username(username)
                        .password(password)
                        .build()
        );

        if (!"Login successful!".equalsIgnoreCase(res.getMessage())) {
            model.addAttribute("error", res.getMessage());
            return "auth/login";
        }

        // Thành công → lưu session và chuyển hướng
        session.setAttribute("CURRENT_USER_ID", res.getUserID());
        session.setAttribute("CURRENT_USERNAME", res.getUsername());

        ra.addFlashAttribute("success", "Đăng nhập thành công!");
        String redirect = (res.getRedirectUrl() != null && !res.getRedirectUrl().isBlank())
                ? res.getRedirectUrl() : "/";
        return "redirect:" + redirect;
    }

    // ========== REGISTER ==========
    @GetMapping("/register")
    public String showRegister() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@RequestParam("email") String email,
                             @RequestParam("username") String username,
                             @RequestParam("password") String password,
                             @RequestParam("confirmPassword") String confirmPassword,
                             RedirectAttributes ra,
                             Model model) {

        // Validate nhanh theo form rút gọn
        if (!StringUtils.hasText(email) || !StringUtils.hasText(username)
                || !StringUtils.hasText(password) || !StringUtils.hasText(confirmPassword)) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ Email, Username, Mật khẩu và Xác nhận mật khẩu.");
            return "auth/register";
        }
        if (username.length() < 4) {
            model.addAttribute("error", "Tên đăng nhập phải có ít nhất 4 ký tự.");
            return "auth/register";
        }
        if (password.length() < 6) {
            model.addAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            return "auth/register";
        }
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Xác nhận mật khẩu không khớp.");
            return "auth/register";
        }
        if (userService.existsByEmail(email)) {
            model.addAttribute("error", "Email đã được đăng ký.");
            return "auth/register";
        }
        if (userService.existsByUsername(username)) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại.");
            return "auth/register";
        }

        // Gọi Service xử lý đăng ký
        RegisterResponse res = userService.register(
                RegisterRequest.builder()
                        .email(email)
                        .username(username)
                        .password(password) // Service sẽ mã hoá bằng PasswordUtil
                        .build()
        );

        if (!"Register successful!".equalsIgnoreCase(res.getMessage())) {
            model.addAttribute("error", res.getMessage());
            return "auth/register";
        }

        ra.addFlashAttribute("success", "Đăng ký thành công! Bạn có thể đăng nhập ngay.");
        return "redirect:/auth/login";
    }

    // ========== LOGOUT ==========
    @PostMapping("/logout")
    public String doLogout(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("success", "Bạn đã đăng xuất.");
        return "redirect:/auth/login";
    }

    // (Tuỳ chọn) GET logout cho tiện test
    @GetMapping("/logout")
    public String doLogoutGet(HttpSession session, RedirectAttributes ra) {
        session.invalidate();
        ra.addFlashAttribute("success", "Bạn đã đăng xuất.");
        return "redirect:/auth/login";
    }
}
