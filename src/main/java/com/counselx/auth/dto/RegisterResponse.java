package com.counselx.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegisterResponse {

    private Long userId;
    private String email;
    private String role;
    private String message;
}