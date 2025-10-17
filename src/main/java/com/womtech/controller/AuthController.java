package com.womtech.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/auth")
public class AuthController {

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login"; 
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam String email,
            @RequestParam String password,
            Model model) {

        if ("admin@womtech.com".equals(email) && "admin123".equals(password)) {
            model.addAttribute("success", "Đăng nhập thành công!");
            return "redirect:/";
        } else {
            model.addAttribute("error", "Sai tài khoản hoặc mật khẩu!");
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String showRegisterPage() {
        return "auth/register"; 
    }
}
