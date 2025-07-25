package org.example.opencvdemo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import org.example.opencvdemo.entity.User;

public interface UserRepo extends JpaRepository<User, Long> {
    User findByUsername(String username);

}
