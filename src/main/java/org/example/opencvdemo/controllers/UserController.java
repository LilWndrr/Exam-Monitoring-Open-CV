package org.example.opencvdemo.controllers;


import org.example.opencvdemo.entity.User;

import org.example.opencvdemo.helper.RegisterRequest;
import org.example.opencvdemo.services.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.function.EntityResponse;

@RestController
@RequestMapping("/user")
    public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }




    // Public endpoint, accessible to anyone
        @GetMapping("/public")
        public String publicAccess() {
            return "This is a public endpoint accessible to anyone.";
        }

        // Endpoint for authenticated users with ROLE_USER
        @GetMapping("/userDashboard")
        @PreAuthorize("hasAuthority('ROLE_USER')")
        public String userDashboard() {
            return "Welcome to the user dashboard!";
        }

        // Endpoint for authenticated users with ROLE_ADMIN
        @GetMapping("/adminDashboard")
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")
        public String adminDashboard() {
            return "Welcome to the admin dashboard!";
        }

        @GetMapping("/admin/getById")
        @PreAuthorize("hasAuthority('ROLE_ADMIN')")

        public ResponseEntity<User> getById (Long id) {
            return ResponseEntity.ok(userService.getUserById(id));
        }


}
