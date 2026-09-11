package com.counselx.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendEmailOtpRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email")
        String email
) {
}
