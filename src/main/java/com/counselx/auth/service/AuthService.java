package com.counselx.auth.service;

import com.counselx.auth.dto.*;
import com.counselx.auth.exception.TooManyRequestsException;
import com.counselx.auth.entity.RefreshToken;
import com.counselx.auth.entity.User;
import com.counselx.auth.entity.UserRole;
import com.counselx.auth.entity.otp_verifications;
import com.counselx.auth.entity.roles;
import com.counselx.auth.repository.OtpVerificationRepository;
import com.counselx.auth.repository.RefreshTokenRepository;
import com.counselx.auth.repository.RoleRepository;
import com.counselx.auth.repository.UserRepository;
import com.counselx.auth.repository.UserRoleRepository;
import com.counselx.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final OtpVerificationRepository otpVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final OtpRateLimiterService otpRateLimiterService;

    @Value("${auth.email-otp.expiration-minutes:10}")
    private long otpExpirationMinutes;

    @Value("${auth.email-otp.max-attempts:5}")
    private int maxOtpAttempts;

    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public RegisterResponse registerStudent(RegisterRequest request) {

        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobile().trim();

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }

        if (userRepository.existsByMobile(mobile)) {
            throw new IllegalArgumentException("Mobile already registered");
        }

        User user = User.builder()
                .email(email)
                .mobile(mobile)
                .password(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .status("PENDING")
                .emailVerified(false)
                .mobileVerified(false)
                .build();

        User savedUser = userRepository.save(user);

        roles studentRole = roleRepository.findByName("STUDENT")
                .orElseThrow(() ->
                        new IllegalStateException("STUDENT role not found"));

        userRoleRepository.save(
                UserRole.builder()
                        .user(savedUser)
                        .role(studentRole)
                        .build()
        );

        if (!otpRateLimiterService.tryAcquire(email)) {
            throw new IllegalStateException("Unable to send verification OTP. Please try again later");
        }

        sendNewEmailOtp(savedUser);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                "STUDENT",
                "Student registered successfully. Verification OTP sent to email."
        );
    }

    @Transactional
    public MessageResponse sendEmailOtp(SendEmailOtpRequest request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified");
        }

        if (!otpRateLimiterService.tryAcquire(email)) {
            throw new TooManyRequestsException(
                    "OTP request limit reached. Please wait before requesting another OTP"
            );
        }

        sendNewEmailOtp(user);

        return new MessageResponse("Email verification OTP sent successfully");
    }

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {

        String email = request.email().trim().toLowerCase();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.isEmailVerified()) {
            return new MessageResponse("Email is already verified");
        }

        otp_verifications otp = otpVerificationRepository
                .findTopByUser_IdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("OTP not found. Please request a new OTP"));

        if (otp.getExpiresAt().isBefore(LocalDateTime.now())) {
            otp.setUsed(true);
            otpVerificationRepository.save(otp);
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP");
        }

        if (otp.getAttempts() >= maxOtpAttempts) {
            otp.setUsed(true);
            otpVerificationRepository.save(otp);
            throw new IllegalArgumentException("Too many incorrect OTP attempts. Please request a new OTP");
        }

        if (!hashToken(request.otp()).equals(otp.getOtpHash())) {
            otp.setAttempts(otp.getAttempts() + 1);

            if (otp.getAttempts() >= maxOtpAttempts) {
                otp.setUsed(true);
            }

            otpVerificationRepository.save(otp);
            throw new IllegalArgumentException("Invalid OTP");
        }

        otp.setUsed(true);
        otpVerificationRepository.save(otp);

        user.setEmailVerified(true);
        userRepository.save(user);

        return new MessageResponse("Email verified successfully");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(
                        request.getEmail().trim().toLowerCase())
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or password"));

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword())) {

            throw new IllegalArgumentException("Invalid email or password");
        }

        if (!"ACTIVE".equals(user.getStatus())
                && !"PENDING".equals(user.getStatus())) {

            throw new IllegalArgumentException("Account is blocked");
        }

        return createLoginResponse(user);
    }

    @Transactional
    public LoginResponse refresh(String rawRefreshToken) {

        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(hashToken(rawRefreshToken))
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            stored.setRevoked(true);
            refreshTokenRepository.save(stored);
            throw new IllegalArgumentException("Refresh token has expired");
        }

        User user = stored.getUser();

        if (!"ACTIVE".equals(user.getStatus())
                && !"PENDING".equals(user.getStatus())) {

            throw new IllegalArgumentException("Account is blocked");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return createLoginResponse(user);
    }

    @Transactional
    public void logout(Long userId) {

        List<RefreshToken> tokens =
                refreshTokenRepository
                        .findAllByUser_IdAndRevokedFalse(userId);

        tokens.forEach(token -> token.setRevoked(true));
        refreshTokenRepository.saveAll(tokens);
        SecurityContextHolder.clearContext();
    }

    @Transactional(readOnly = true)
    public MeResponse me(Long userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<String> roles = getRoles(userId);

        return new MeResponse(
                user.getId(),
                user.getEmail(),
                user.getMobile(),
                user.getFirstName(),
                user.getLastName(),
                user.getStatus(),
                user.isEmailVerified(),
                user.isMobileVerified(),
                roles
        );
    }

    private void sendNewEmailOtp(User user) {

        otpVerificationRepository.findAllByUser_IdAndUsedFalse(user.getId())
                .forEach(existing -> existing.setUsed(true));

        String otp = generateOtp();

        otp_verifications entity = otp_verifications.builder()
                .user(user)
                .otpHash(hashToken(otp))
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .used(false)
                .attempts(0)
                .build();

        otpVerificationRepository.save(entity);
        emailService.sendEmailOtp(user.getEmail(), otp, user.getFirstName());
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }

    private LoginResponse createLoginResponse(User user) {

        List<String> roles = getRoles(user.getId());

        String accessToken = jwtService.generateToken(
                user.getId(), user.getEmail(), roles);

        String refreshToken = generateRefreshToken();

        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(refreshToken))
                .expiresAt(LocalDateTime.now().plusDays(7))
                .revoked(false)
                .build();

        refreshTokenRepository.save(entity);

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtService.getExpirationSeconds(),
                refreshToken,
                user.getId(),
                user.getEmail(),
                roles
        );
    }

    private List<String> getRoles(Long userId) {
        return userRoleRepository.findAllByUser_Id(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName())
                .toList();
    }

    private String generateRefreshToken() {
        byte[] bytes = new byte[64];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 algorithm not available", ex);
        }
    }
}
