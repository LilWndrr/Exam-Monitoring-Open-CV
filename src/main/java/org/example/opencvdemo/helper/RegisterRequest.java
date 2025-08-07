package org.example.opencvdemo.helper;

import lombok.Builder;
import org.example.opencvdemo.entity.Role;

import java.util.ArrayList;
import java.util.Set;

@Builder
public class RegisterRequest {

    private String username;
    private String password;


    public RegisterRequest(String username, String password, ArrayList<Role> roles) {
        this.username = username;
        this.password = password;

    }
    public RegisterRequest(String username, String password) {
        this.username = username;
        this.password = password;

    }

    public RegisterRequest() {
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


}
