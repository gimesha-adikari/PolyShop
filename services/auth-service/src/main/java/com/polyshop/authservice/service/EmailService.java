package com.polyshop.authservice.service;


public interface EmailService {
    void sendEmail(String to, String subject, String htmlBody);
}
