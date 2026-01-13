package com.example.demo.config;

import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        // Create default admin user if it doesn't exist
        if (!userRepository.existsByEmail("admin@logsphere.com")) {
            User admin = new User();
            admin.setEmail("admin@logsphere.com");
            admin.setPassword("admin123");
            admin.setName("Admin User");
            admin.setRole("ADMIN");
            userRepository.save(admin);
            System.out.println("Default admin user created: admin@logsphere.com / admin123");
        }
        
        // Create default HR user if it doesn't exist
        if (!userRepository.existsByEmail("hr@logsphere.com")) {
            User hr = new User();
            hr.setEmail("hr@logsphere.com");
            hr.setPassword("hr123");
            hr.setName("HR User");
            hr.setRole("HR");
            userRepository.save(hr);
            System.out.println("Default HR user created: hr@logsphere.com / hr123");
        }
        
        // Create default Manager user if it doesn't exist
        if (!userRepository.existsByEmail("manager@logsphere.com")) {
            User manager = new User();
            manager.setEmail("manager@logsphere.com");
            manager.setPassword("manager123");
            manager.setName("Manager User");
            manager.setRole("MANAGER");
            userRepository.save(manager);
            System.out.println("Default Manager user created: manager@logsphere.com / manager123");
        }
    }
}

