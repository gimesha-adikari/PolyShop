package com.polyshop.authservice.service.impl;

import com.polyshop.authservice.service.EmailService;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class SmtpEmailService implements EmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final Logger log = LoggerFactory.getLogger(getClass());

    public SmtpEmailService(JavaMailSender mailSender, org.springframework.core.env.Environment env) {
        this.mailSender = mailSender;
        this.from = env.getProperty("spring.mail.from", "no-reply@polyshop.example");
    }

    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(msg);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("email send failed to {}: {}", to, e.getMessage(), e);
            throw new RuntimeException("email_failed");
        }
    }

    public String renderTemplate(String templatePath, Map<String,String> variables) {
        try {
            ClassPathResource r = new ClassPathResource(templatePath);
            String raw = StreamUtils.copyToString(r.getInputStream(), StandardCharsets.UTF_8);
            return new StringSubstitutor(variables).replace(raw);
        } catch (Exception e) {
            log.warn("failed to render template {}: {}", templatePath, e.getMessage());
            return variables.getOrDefault("body", "");
        }
    }
}
