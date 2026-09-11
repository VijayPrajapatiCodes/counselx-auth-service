package com.counselx.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    public void sendEmailOtp(String toEmail, String otp, String firstName) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("CounselX - Email Verification OTP");
        message.setText(
                "Hello " + firstName + ",\n\n" +
                "Your CounselX email verification OTP is: " + otp + "\n\n" +
                "This OTP is valid for 10 minutes.\n" +
                "Do not share this OTP with anyone.\n\n" +
                "If you did not create a CounselX account, you can ignore this email.\n\n" +
                "Regards,\n" +
                "CounselX Team"
        );

        mailSender.send(message);
    }
}
