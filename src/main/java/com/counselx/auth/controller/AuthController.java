package com.counselx.auth.controller;

import com.counselx.auth.dto.*;
import com.counselx.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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



    @PutMapping("/me")
    public ResponseEntity<MeResponse> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(authService.updateProfile(userId, request));
    }

    @PutMapping(value = "/profile-photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MeResponse> updateProfilePhoto(
            Authentication authentication,
            @RequestPart("file") MultipartFile file) throws Exception {

        Long userId = (Long) authentication.getPrincipal();
        return ResponseEntity.ok(
                authService.updateProfilePhoto(
                        userId,
                        file.getBytes(),
                        file.getContentType()
                )
        );
    }

    @GetMapping("/profile-photo")
    public ResponseEntity<byte[]> getProfilePhoto(Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        AuthService.ProfilePhoto photo = authService.getProfilePhoto(userId);

        MediaType mediaType = MediaType.parseMediaType(photo.contentType());

        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(org.springframework.http.CacheControl.noCache())
                .body(photo.bytes());
    }

    @DeleteMapping("/profile-photo")
    public ResponseEntity<Void> deleteProfilePhoto(Authentication authentication) {

        Long userId = (Long) authentication.getPrincipal();
        authService.deleteProfilePhoto(userId);
        return ResponseEntity.noContent().build();
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
