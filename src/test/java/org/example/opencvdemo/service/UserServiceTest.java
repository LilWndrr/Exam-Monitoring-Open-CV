package org.example.opencvdemo.service;


import org.assertj.core.api.Assertions;
import org.example.opencvdemo.entity.Role;
import org.example.opencvdemo.entity.User;
import org.example.opencvdemo.exception.ApiRequestException;
import org.example.opencvdemo.helper.RegisterRequest;
import org.example.opencvdemo.repository.RoleRepo;
import org.example.opencvdemo.repository.UserRepo;
import org.example.opencvdemo.services.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepo userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RoleRepo roleRepo;
    @InjectMocks
    private UserService userService;

    User testUser;

    Role testRole;

    Set<Role> roles;

    @BeforeEach
    void setUp() {

        testRole = Role.builder().name("ROLE_USER").id(1L).build();
       roles = new HashSet<>();
       roles.add(testRole);
        testUser= User.builder().id(1L).roles(roles).username("TestUser").password("password").snapshots(new HashSet<>()).build();

    }

    @Test
    public void UserServiceTest_GetUserById_ReturnsExistingUser(){
        long id = 1L;

        when(userRepo.findById(id)).thenReturn(Optional.of(testUser));

        User user = userService.getUserById(id);

        Assertions.assertThat(user).isNotNull();
        Assertions.assertThat(user.getUsername()).isEqualTo("TestUser");
        verify(userRepo).findById(id);

    }

    @Test
    public void UserServiceTest_GetUserById_ReturnsNullIfUserNotFound(){
        long id = 3L;

        when(userRepo.findById(id)).thenReturn(Optional.empty());

        User user = userService.getUserById(id);

        Assertions.assertThat(user).isNull();
        verify(userRepo).findById(id);

    }

    @Test
    public void UserServiceTest_CreateUser_ReturnsSavedUser(){
        long id = 3L;
        String expectedUsername = "TestUser";
        String expectedPassword = "password";
        String expectedRole = "ROLE_USER";
        String encodedPassword = "encodedPassword123";
        RegisterRequest registerRequest = new RegisterRequest("TestUser","password", new ArrayList<>());

        when(userRepo.findByUsername(expectedUsername)).thenReturn(null);
        when(roleRepo.findByName(expectedRole)).thenReturn(testRole);
        when(passwordEncoder.encode(expectedPassword)).thenReturn(encodedPassword);
        when(userRepo.save(any(User.class))).thenReturn(testUser);


        User saveUser = userService.createUser(registerRequest,expectedRole);

        Assertions.assertThat(saveUser).isNotNull();
        verify(userRepo).findByUsername(expectedUsername);
        verify(roleRepo).findByName(expectedRole);
        verify(passwordEncoder).encode(expectedPassword);
        verify(userRepo).save(argThat(user -> {
            assertAll(
                    ()-> Assertions.assertThat(user.getPassword()).isEqualTo(encodedPassword),
                    ()-> Assertions.assertThat(user.getUsername()).isEqualTo(expectedUsername),
                    ()-> Assertions.assertThat(user.getRoles()).isEqualTo(roles)
            );
            return true;
        }));

    }

    @Test
    public void UserServiceTest_CreateUser_ReturnsAlreadyExistingUserException(){

        String existingUsername = "TestUser";

        String expectedRole = "ROLE_USER";

        RegisterRequest registerRequest = new RegisterRequest(existingUsername,"password", new ArrayList<>());

        when(userRepo.findByUsername(existingUsername)).thenReturn(testUser);

        ApiRequestException exception= assertThrows(ApiRequestException.class, () -> {
            userService.createUser(registerRequest,expectedRole);
        } );


        Assertions.assertThat(exception.getMessage()).isEqualTo("Username already exists");
        verify(userRepo).findByUsername(existingUsername);
        verify(roleRepo,never()).findByName(anyString());
        verify(passwordEncoder,never()).encode(anyString());
        verify(userRepo,never()).save(any(User.class));


    }
    @Test
    public void UserServiceTest_CreateUser_ReturnsRoleInvalidException(){

        String expectedUsername = "TestUser";

        String invalidRole = "ROL_USER";

        RegisterRequest registerRequest = new RegisterRequest("TestUser","password", new ArrayList<>());

        when(userRepo.findByUsername(expectedUsername)).thenReturn(null);
        when(roleRepo.findByName(invalidRole)).thenReturn(null);


        ApiRequestException exception= assertThrows(ApiRequestException.class, () -> {
            userService.createUser(registerRequest,invalidRole);
        } );



        Assertions.assertThat(exception.getMessage()).isEqualTo("Role is Invalid");
        verify(userRepo).findByUsername(expectedUsername);
        verify(passwordEncoder,never()).encode(anyString());
        verify(userRepo,never()).save(any(User.class));


    }


}
