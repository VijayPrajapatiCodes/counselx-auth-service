package com.counselx.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 80, message = "First name is too long")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 80, message = "Last name is too long")
        String lastName,

        @NotBlank(message = "Mobile is required")
        @Pattern(regexp = "\\d{10}", message = "Mobile must be 10 digits")
        String mobile
) {}
