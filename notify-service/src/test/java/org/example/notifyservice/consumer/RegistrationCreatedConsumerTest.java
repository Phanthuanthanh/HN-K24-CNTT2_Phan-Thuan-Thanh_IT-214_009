package org.example.notifyservice.consumer;

import org.example.notifyservice.service.EmailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.annotation.KafkaListener;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationCreatedConsumerTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private RegistrationCreatedConsumer consumer;

    @Test
    void consumeValidEmailCallsEmailService() {
        consumer.consume("test@example.com");

        verify(emailService, times(1)).sendOrderCreatedEmail("test@example.com");
    }

    @Test
    void consumeJsonQuotedEmailStripsQuotes() {
        consumer.consume("\"test@example.com\"");

        verify(emailService, times(1)).sendOrderCreatedEmail("test@example.com");
    }

    @Test
    void consumeNullEmailDoesNotCallService() {
        consumer.consume(null);

        verify(emailService, never()).sendOrderCreatedEmail(anyString());
    }

    @Test
    void consumeEmptyEmailDoesNotCallService() {
        consumer.consume("   ");

        verify(emailService, never()).sendOrderCreatedEmail(anyString());
    }

    @Test
    void consumeHasKafkaListenerAnnotation() throws NoSuchMethodException {
        Method method = RegistrationCreatedConsumer.class.getMethod("consume", String.class);
        KafkaListener annotation = method.getAnnotation(KafkaListener.class);

        assertNotNull(annotation);
        assertEquals(1, annotation.topics().length);
        assertEquals("${notification.kafka.order-created-topic:registration-created}", annotation.topics()[0]);
    }
}
