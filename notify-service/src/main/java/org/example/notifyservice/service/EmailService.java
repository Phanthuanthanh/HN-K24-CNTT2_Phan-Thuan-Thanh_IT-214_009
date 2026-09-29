package org.example.notifyservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String from;

    public EmailService(JavaMailSender mailSender, @Value("${notification.mail.from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void sendOrderCreatedEmail(String recipient) {
        if (recipient == null || recipient.trim().isEmpty()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.trim().isEmpty()) {
            message.setFrom(from);
        }
        message.setTo(recipient.trim());
        message.setSubject("Registration Confirmation");
        message.setText("Registration created successfully for " + recipient.trim());
        mailSender.send(message);
    }
}
