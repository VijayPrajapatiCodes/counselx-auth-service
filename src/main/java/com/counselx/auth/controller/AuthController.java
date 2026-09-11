package com.counselx.auth.controller;

import com.counselx.auth.dto.*;
import com.counselx.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerStudent(
            @Valid @RequestBody RegisterRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerStudent(request));
    }


    @PostMapping("/send-email-otp")
    public ResponseEntity<MessageResponse> sendEmailOtp(
            @Valid @RequestBody SendEmailOtpRequest request) {

        return ResponseEntity.ok(authService.sendEmailOtp(request));
    }

    @PostMapping("/verify-email")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {

        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request) {

        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshRequest request) {

        return ResponseEntity.ok(
                authService.refresh(request.getRefreshToken())
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        authService.logout(userId);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponse> me(Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(authService.me(userId));
    }
}
