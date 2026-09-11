package com.counselx.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class MeResponse {

    private Long userId;
    private String email;
    private String mobile;
    private String firstName;
    private String lastName;
    private String status;
    private boolean emailVerified;
    private boolean mobileVerified;
    private List<String> roles;
}
