package com.womtech.controller;

import com.womtech.service.CategoryService;
import com.womtech.service.ProductService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping("/")
    public String home(Model model, Principal principal, HttpSession session, HttpServletRequest request) {
        // Kiểm tra authentication
        boolean isAuthenticated = false;
        String currentUserId = null;
        String currentUsername = null;

        // Ưu tiên JWT authentication (Principal)
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            isAuthenticated = true;
            currentUserId = principal.getName();
            // Có thể lấy username từ JWT token nếu cần
        } else {
            // Fallback về session nếu không có JWT
            Object sessionUserId = session.getAttribute("CURRENT_USER_ID");
            if (sessionUserId != null) {
                isAuthenticated = true;
                currentUserId = sessionUserId.toString();
                currentUsername = (String) session.getAttribute("CURRENT_USERNAME");
            }
        }

        // Load featured products (lấy 8 sản phẩm đầu tiên)
        model.addAttribute("featuredProducts", productService.getAllProducts().stream().limit(8).toList());
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("currentUsername", currentUsername);

        model.addAttribute("featuredCategories", categoryService.findAll());
        
        return "index";
    }
}
