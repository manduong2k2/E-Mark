package com.Marketplace_Management.Auth.Services;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import com.Marketplace_Management.Auth.Entities.EmailVerifyToken;
import com.Marketplace_Management.Auth.Repositories.IEmailVerifyTokenRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {
    private final IEmailVerifyTokenRepository repo;
    private final PasswordEncoder encoder;
    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${spring.application.base-url}")
    private String baseUrl;

    @Value("${third-party.frontend.base-url}")
    private String frontendBaseUrl;

    @Transactional
    public void createToken(String email, String token) {
        EmailVerifyToken entity = repo.findByEmail(email);
        if (entity != null) {
            repo.delete(entity);
        }

        String hashed = encoder.encode(token);
        entity = new EmailVerifyToken();
        entity.setEmail(email);
        entity.setToken(hashed);
        entity.setExpiresAt(LocalDateTime.now().plusHours(24));
        repo.save(entity);
    }

    public boolean verify(String email, String token) {
        EmailVerifyToken entity = repo.findByEmail(email);
        if (entity == null) {
            return false;
        }

        if (entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            repo.delete(entity);
            return false;
        }

        if (!encoder.matches(token, entity.getToken())) {
            return false;
        }

        repo.delete(entity);
        return true;
    }

    @Async
    public void sendVerifyEmail(String toEmail, String token) throws MessagingException {
        String link = baseUrl + "/api/auth/verify-email?email=" + urlEncode(toEmail) + "&token=" + urlEncode(token);
        sendLinkEmail(toEmail, "Activate your account", "mails/activate_account.html", link);
    }

    @Async
    public void sendResetPasswordEmail(String toEmail, String token) throws MessagingException {
        // The web app's forgot-password page shows the "new password" form when it has email + token
        String link = frontendBaseUrl + "/forgot?email=" + urlEncode(toEmail) + "&token=" + urlEncode(token);
        sendLinkEmail(toEmail, "Reset your password", "mails/reset_password.html", link);
    }

    /** Renders a mail template that has a link variable and sends it as HTML. */
    private void sendLinkEmail(String toEmail, String subject, String template, String link) throws MessagingException {
        Context context = new Context();
        context.setVariable("link", link);

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(toEmail);
        helper.setSubject(subject);
        helper.setText(templateEngine.process(template, context), true);

        mailSender.send(mimeMessage);
    }

    private static String urlEncode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
