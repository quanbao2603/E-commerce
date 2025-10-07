package com.womtech.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class UserController {

    @GetMapping("")
    public String home(Model model) {
        model.addAttribute("message", "Xin chào, đây là trang người dùng!");
        return "user/index";
    }
}
