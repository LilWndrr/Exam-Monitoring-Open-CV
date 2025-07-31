package org.example.opencvdemo.services;

import org.example.opencvdemo.entity.Role;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.exception.ApiRequestException;
import org.example.opencvdemo.helper.RegisterRequest;
import org.example.opencvdemo.repository.RoleRepo;
import org.example.opencvdemo.repository.UserRepo;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RoleRepo roleRepo;

    public UserService(UserRepo userRepo, PasswordEncoder passwordEncoder, RoleRepo roleRepo) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.roleRepo = roleRepo;
    }

    public User createUser(RegisterRequest request, String role) {
        if(userRepo.findByUsername(request.getUsername()) != null) {
            throw new RuntimeException("Username is already exist");
        }
        Set<Role> roles = new HashSet<>();

            Role newRole = roleRepo.findByName(role);
            if(newRole == null) {
                throw new ApiRequestException("Role is Invalid");
            }
            roles.add(newRole);

        User newUser = new User();
        newUser.setUsername(request.getUsername());
        newUser.setPassword(passwordEncoder.encode(request.getPassword()));
        newUser.setRoles(roles);



        return userRepo.save(newUser);
    }


    public User getUserById(Long id) {
        return userRepo.findById(id).orElse(null);
    }
}
