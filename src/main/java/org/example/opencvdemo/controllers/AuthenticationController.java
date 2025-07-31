package org.example.opencvdemo.controllers;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.opencvdemo.helper.AuthenticationRequest;
import org.example.opencvdemo.helper.AuthenticationResponse;
import org.example.opencvdemo.helper.RegisterRequest;
import org.example.opencvdemo.services.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;


    public AuthenticationController(AuthenticationManager authenticationManager, UserService userService
    ) {
        this.authenticationManager = authenticationManager;

        this.userService = userService;
    }


    @PostMapping("/register")
    public ResponseEntity<String> register (@RequestBody RegisterRequest request) {
        userService.createUser(request,"ROLE_USER");
        return ResponseEntity.ok("User created successfully");
    }
    @PostMapping("/admin/create")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<String> create (@RequestBody RegisterRequest request) {
        userService.createUser(request,"ROLE_ADMIN");
        return ResponseEntity.ok("Admin created successfully");
    }


    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(@RequestBody AuthenticationRequest authenticationRequest,
                                                        HttpServletRequest request,
                                                        HttpServletResponse response) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(
                            authenticationRequest.getUsername(),
                            authenticationRequest.getPassword()
                    );

            Authentication authentication = authenticationManager.authenticate(authenticationToken);


            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);




            HttpSession session = request.getSession(true);
            session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);

            return ResponseEntity.ok( new AuthenticationResponse(session.getId(),SecurityContextHolder.getContext().getAuthentication().getAuthorities().toString()));

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new AuthenticationResponse());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        SecurityContextHolder.clearContext();
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.ok("Logout successful");
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getAuthStatus(HttpServletRequest request) {
        Map<String, Object> status = new HashMap<>();

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        HttpSession session = request.getSession(false);

        status.put("authenticated", auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken));
        status.put("principal", auth != null ? auth.getName() : "none");
        status.put("authorities", auth != null ? auth.getAuthorities().toString() : "none");
        status.put("sessionId", session != null ? session.getId() : "no session");
        status.put("sessionCreationTime", session != null ? new Date(session.getCreationTime()).toString() : "no session");

        return ResponseEntity.ok(status);
    }
}