package com.polyshop.authservice.service.impl;

import com.polyshop.authservice.service.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
public class NoopEmailService implements EmailService {
    private final Logger log = LoggerFactory.getLogger(NoopEmailService.class);
    @Override
    public void sendEmail(String to, String subject, String htmlBody) {
        log.info("NOOP_EMAIL to={} subject={} body={}", to, subject, htmlBody);
    }
}
