package com.womtech.controller;

import com.womtech.dto.request.RegisterRequest;
import com.womtech.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/register") 
@RequiredArgsConstructor
public class RegisterController {

    private final UserService userService;

    @GetMapping
    public String showRegisterForm(Model model) {
        model.addAttribute("registerRequest", new RegisterRequest());
        return "auth/register";
    }

    @PostMapping
    public String handleRegister(
            @Valid @ModelAttribute("registerRequest") RegisterRequest request,
            BindingResult result,
            Model model
    ) {
        
        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            userService.register(request);
            model.addAttribute("successMsg", "Đăng ký tài khoản thành công!");
            return "auth/login"; 
        } catch (IllegalArgumentException ex) {
            model.addAttribute("errorMsg", ex.getMessage());
            return "auth/register";
        } catch (Exception ex) {
            model.addAttribute("errorMsg", "Đã xảy ra lỗi không mong muốn!");
            return "auth/register";
        }
    }
}
