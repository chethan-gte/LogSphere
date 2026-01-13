package com.example.demo.controller;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/login")
    public String showLoginForm(Model model, HttpSession session) {
        // If already logged in, redirect to dashboard
        if (session.getAttribute("user") != null) {
            return "redirect:/admin/dashboard";
        }
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String email, 
                       @RequestParam String password,
                       HttpSession session,
                       RedirectAttributes redirectAttributes) {
        
        Optional<User> userOptional = userRepository.findByEmail(email);
        
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            // Simple password check (in production, use password hashing like BCrypt)
            if (user.getPassword().equals(password)) {
                session.setAttribute("user", user);
                session.setAttribute("userName", user.getName());
                session.setAttribute("userRole", user.getRole());
                
                // Redirect based on role
                if ("ADMIN".equals(user.getRole())) {
                    return "redirect:/admin/dashboard";
                } else if ("HR".equals(user.getRole())) {
                    return "redirect:/hr/dashboard";
                } else if ("MANAGER".equals(user.getRole())) {
                    return "redirect:/manager/dashboard";
                } else {
                    return "redirect:/dashboard";
                }
            } else {
                redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
                return "redirect:/login";
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Invalid email or password!");
            return "redirect:/login";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }
}

