package org.example.opencvdemo.helper;

import java.util.List;
import java.util.Set;

public class RegisterRequest {

    private String username;
    private String password;


    public RegisterRequest(String username, String password, List<String> roles) {
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
