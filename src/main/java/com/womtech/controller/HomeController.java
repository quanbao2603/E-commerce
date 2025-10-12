package com.womtech.controller;

import com.womtech.service.CategoryService;
import com.womtech.service.ProductService;
import com.womtech.service.UserService;
import com.womtech.util.CookieUtil;
import com.womtech.security.JwtService;
import com.womtech.security.TokenRevokeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.security.Principal;

@Controller
@ControllerAdvice
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;
    private final UserService userService;
    private final JwtService jwtService;
    private final TokenRevokeService tokenRevokeService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("featuredProducts", productService.findAll().stream().limit(8).toList());
        model.addAttribute("featuredCategories", categoryService.findAll());
        return "index";
    }

    @ModelAttribute
    public void addAuthenticationInfo(Model model, Principal principal, HttpServletRequest request, HttpSession session) {
        boolean isAuthenticated = false;
        String currentUserId = null;
        String currentUsername = null;

        System.out.println("=== DEBUG AUTHENTICATION ===");
        System.out.println("Principal: " + (principal != null ? principal.getName() : "null"));
        
        var cookie = CookieUtil.get(request, "AT");
        System.out.println("AT Cookie: " + (cookie != null ? "exists" : "null"));
        if (cookie != null) {
            System.out.println("AT Valid: " + jwtService.isValidAccess(cookie.getValue()));
            boolean isRevoked = tokenRevokeService.isRevoked(cookie.getValue());
            System.out.println("AT Revoked: " + isRevoked);
            System.out.println("AT Username: " + jwtService.getUsername(cookie.getValue()));
            System.out.println("AT Token: " + cookie.getValue().substring(0, Math.min(20, cookie.getValue().length())) + "...");
        }
        
        Object sessionUserId = session.getAttribute("CURRENT_USER_ID");
        System.out.println("Session User ID: " + sessionUserId);
        System.out.println("=============================");

        // Ưu tiên JWT authentication (Principal)
        if (principal != null && principal.getName() != null && !principal.getName().isBlank()) {
            isAuthenticated = true;
            currentUserId = principal.getName();
            
            // Lấy username từ JWT token
            if (cookie != null) {
                currentUsername = jwtService.getUsername(cookie.getValue());
            }
            
            // Fallback: lấy từ database nếu không có trong token
            if (currentUsername == null) {
                userService.findById(currentUserId).ifPresent(user -> {
                    model.addAttribute("currentUsername", user.getUsername());
                });
            }
        } else if (cookie != null && jwtService.isValidAccess(cookie.getValue()) && !tokenRevokeService.isRevoked(cookie.getValue())) {
            // Nếu Principal null nhưng có valid JWT token và chưa bị revoke -> vẫn authenticated
            isAuthenticated = true;
            currentUserId = jwtService.getUserId(cookie.getValue());
            currentUsername = jwtService.getUsername(cookie.getValue());
        } else {
            // Nếu có cookie nhưng token bị revoke -> xóa cookies
            if (cookie != null && tokenRevokeService.isRevoked(cookie.getValue())) {
                clearAuthCookies(request);
            }
            
            // KHÔNG fallback về session để tránh hiển thị user sau khi logout
            // Chỉ dựa vào JWT authentication
        }

        System.out.println("Final result - isAuthenticated: " + isAuthenticated + ", username: " + currentUsername);

        // Load featured products (lấy 8 sản phẩm đầu tiên)
        model.addAttribute("featuredProducts", productService.getAllProducts().stream().limit(8).toList());
        model.addAttribute("isAuthenticated", isAuthenticated);
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("currentUsername", currentUsername);
    }

    private void clearAuthCookies(HttpServletRequest request) {
        // Xóa cookies bằng cách set response headers
        // Note: Trong @ModelAttribute, chúng ta không có HttpServletResponse
        // Nên sẽ xóa cookies ở client-side hoặc trong filter
        System.out.println("Token bị revoke - cần xóa cookies");
    }
}
