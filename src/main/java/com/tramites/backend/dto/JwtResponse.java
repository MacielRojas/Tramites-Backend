package com.tramites.backend.dto;

import lombok.Getter;

import java.util.Set;

@Getter
public class JwtResponse {

    private final String token;
    private final String type = "Bearer";
    private final String id;
    private final String username;
    private final String email;
    private final Set<String> roles;

    public JwtResponse(String token, String id, String username, String email, Set<String> roles) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.roles = roles;
    }
}
