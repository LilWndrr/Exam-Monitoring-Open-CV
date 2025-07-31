package org.example.opencvdemo.config;

import org.example.opencvdemo.entity.Role;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.repository.RoleRepo;
import org.example.opencvdemo.repository.UserRepo;
import org.example.opencvdemo.services.CameraService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class DataSeed implements CommandLineRunner {

    private final UserRepo userRepository;

    private final RoleRepo roleRepository;

    private final CameraService cameraService;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeed(UserRepo userRepository, RoleRepo roleRepository, CameraService cameraService, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.cameraService = cameraService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
       /* Role userRole = new Role();
        userRole.setName("ROLE_USER");
        roleRepository.save(userRole);

        Role adminRole = new Role();
        adminRole.setName("ROLE_ADMIN");
        roleRepository.save(adminRole);

        User user = new User();
        user.setUsername("user");
        user.setPassword(passwordEncoder.encode("userpassword"));
        user.setRoles(Set.of(userRole));
        userRepository.save(user);

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("adminpassword"));
        admin.setRoles(Set.of(adminRole));
        userRepository.save(admin);
*/
        cameraService.train();


    }
}
