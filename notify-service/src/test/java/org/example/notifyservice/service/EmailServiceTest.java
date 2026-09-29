package org.example.notifyservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    private EmailService emailService;

    @BeforeEach
    void setUp() {
        emailService = new EmailService(mailSender, "noreply@example.com");
    }

    @Test
    void sendOrderCreatedEmailValidRecipientSendsMessage() {
        emailService.sendOrderCreatedEmail("user@example.com");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());

        SimpleMailMessage sentMessage = captor.getValue();
        assertNotNull(sentMessage);
        assertNotNull(sentMessage.getTo());
        assertTrue(Objects.requireNonNull(sentMessage.getTo())[0].contains("user@example.com"));
        assertEquals("noreply@example.com", sentMessage.getFrom());
        assertNotNull(sentMessage.getSubject());
        assertNotNull(sentMessage.getText());
    }

    @Test
    void sendOrderCreatedEmailNullRecipientDoesNotSend() {
        emailService.sendOrderCreatedEmail(null);

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void sendOrderCreatedEmailEmptyRecipientDoesNotSend() {
        emailService.sendOrderCreatedEmail("   ");

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }
}
