package com.ardhana.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * Thin wrapper around Spring's JavaMailSender (SMTP) — currently used only to
 * email a school's login credentials once an admin creates their account.
 * Provider-agnostic: point spring.mail.* at any SMTP relay (Gmail app
 * password, SES, SendGrid, Mailgun, your own mail server, etc).
 */
@Slf4j
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${spring.mail.host}")
    private String smtpHost;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    void logConfigStatus() {
        boolean looksUnconfigured = smtpHost == null || smtpHost.isBlank() || smtpHost.contains("REPLACE_ME")
                || fromAddress == null || fromAddress.isBlank() || fromAddress.contains("REPLACE_ME");
        if (looksUnconfigured) {
            log.warn("SMTP is NOT configured with real credentials (host='{}', from='{}'). "
                    + "Emails (e.g. school login credentials) will fail to send until MAIL_HOST / "
                    + "MAIL_USERNAME / MAIL_PASSWORD / MAIL_FROM are set.", smtpHost, fromAddress);
        } else {
            log.info("SMTP configured: host={}, from={}", smtpHost, fromAddress);
        }
    }

    /**
     * Sends a school's login email + a freshly-generated plaintext password.
     * This is the ONLY point in the system where the plaintext password is
     * ever visible — it's never logged and never stored (only its bcrypt
     * hash is persisted).
     */
    public void sendSchoolLoginCredentials(String toEmail, String schoolName, String plaintextPassword) {
        String loginUrl = frontendUrl.replaceAll("/$", "") + "/auth";

        String body = "Hello " + schoolName + ",\n\n"
                + "An account has been created so you can view and pay your fees online.\n\n"
                + "Login: " + loginUrl + "\n"
                + "Email: " + toEmail + "\n"
                + "Password: " + plaintextPassword + "\n\n"
                + "For security, please sign in and keep these credentials somewhere safe. "
                + "If you did not expect this email, please contact us.\n\n"
                + "— Ardhana";

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(toEmail);
        message.setSubject("Your Ardhana login details");
        message.setText(body);

        try {
            mailSender.send(message);
            log.info("School login credentials emailed to {}", toEmail);
        } catch (Exception e) {
            // Deliberately does NOT log plaintextPassword or the exception's
            // full message if it happens to echo the message body back.
            log.error("Failed to email login credentials to {}: {}", toEmail, e.getClass().getSimpleName());
            throw new IllegalStateException("Unable to send login email: " + e.getMessage(), e);
        }
    }
}
