package com.counselx.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class LoginResponse {

    private String accessToken;
    private String tokenType;
    private long expiresIn;
    private String refreshToken;
    private Long userId;
    private String email;
    private List<String> roles;
}
