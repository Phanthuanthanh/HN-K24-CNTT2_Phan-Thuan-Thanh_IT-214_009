package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class RegistrationCreatedConsumer {

    private final EmailService emailService;

    public RegistrationCreatedConsumer(EmailService emailService) {
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${notification.kafka.order-created-topic:registration-created}", groupId = "${spring.kafka.consumer.group-id:notify-service}")
    public void consume(String email) {
        if (email == null) {
            return;
        }
        String cleanEmail = email.trim();
        if (cleanEmail.startsWith("\"") && cleanEmail.endsWith("\"") && cleanEmail.length() >= 2) {
            cleanEmail = cleanEmail.substring(1, cleanEmail.length() - 1).trim();
        }
        if (!cleanEmail.isEmpty()) {
            emailService.sendOrderCreatedEmail(cleanEmail);
        }
    }

}
