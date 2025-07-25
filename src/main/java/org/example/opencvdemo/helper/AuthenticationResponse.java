package org.example.opencvdemo.helper;

public class AuthenticationResponse {

    String sessionId;
    String role;

    public AuthenticationResponse(String sessionId, String role) {
        this.sessionId = sessionId;
        this.role = role;
    }

    public AuthenticationResponse() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
