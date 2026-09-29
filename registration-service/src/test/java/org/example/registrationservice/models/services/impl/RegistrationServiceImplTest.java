package org.example.registrationservice.models.services.impl;

import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.exceptions.EventServiceException;
import org.example.registrationservice.models.constants.RegistrationStatus;
import org.example.registrationservice.models.dto.requests.CreateRegistrationDetailRequest;
import org.example.registrationservice.models.dto.requests.CreateRegistrationRequest;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.example.registrationservice.models.entities.Registration;
import org.example.registrationservice.models.entities.RegistrationDetail;
import org.example.registrationservice.models.repositories.RegistrationDetailRepository;
import org.example.registrationservice.models.repositories.RegistrationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private RegistrationRepository registrationRepository;

    @Mock
    private RegistrationDetailRepository registrationDetailRepository;

    @Mock
    private EventGatewayService eventGatewayService;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void createRegistrationSuccess() {
        CreateRegistrationDetailRequest item1 = new CreateRegistrationDetailRequest(1L, 2);
        CreateRegistrationDetailRequest item2 = new CreateRegistrationDetailRequest(2L, 3);
        CreateRegistrationRequest request = new CreateRegistrationRequest(
                "Nguyen Van A",
                "nguyenvana@example.com",
                List.of(item1, item2)
        );

        EventResponse event1 = new EventResponse(1L, "Event 1", 50.0, 10);
        EventResponse event2 = new EventResponse(2L, "Event 2", 100.0, 5);

        when(eventGatewayService.getEventById(1L)).thenReturn(event1);
        when(eventGatewayService.getEventById(2L)).thenReturn(event2);

        Registration savedReg = Registration.builder()
                .id(100L)
                .participantName("Nguyen Van A")
                .participantEmail("nguyenvana@example.com")
                .totalAmount(400.0)
                .status(RegistrationStatus.PENDING)
                .build();
        when(registrationRepository.save(any(Registration.class))).thenReturn(savedReg);

        RegistrationDetail detail1 = RegistrationDetail.builder()
                .id(10L)
                .registration(savedReg)
                .eventId(1L)
                .quantity(2)
                .ticketPrice(50.0)
                .lineTotal(100.0)
                .build();
        RegistrationDetail detail2 = RegistrationDetail.builder()
                .id(11L)
                .registration(savedReg)
                .eventId(2L)
                .quantity(3)
                .ticketPrice(100.0)
                .lineTotal(300.0)
                .build();
        when(registrationDetailRepository.saveAll(anyList())).thenReturn(List.of(detail1, detail2));

        RegistrationResponse response = registrationService.createRegistration(request);

        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("Nguyen Van A", response.participantName());
        assertEquals("nguyenvana@example.com", response.participantEmail());
        assertEquals(400.0, response.totalAmount());
        assertEquals(RegistrationStatus.PENDING, response.status());
        assertEquals(2, response.details().size());
        assertEquals(100.0, response.details().get(0).lineTotal());
        assertEquals(300.0, response.details().get(1).lineTotal());

        verify(kafkaTemplate, times(1)).send("registration-created", "nguyenvana@example.com");
    }

    @Test
    void createRegistrationThrowsWhenAvailableTicketsInsufficient() {
        CreateRegistrationDetailRequest item = new CreateRegistrationDetailRequest(1L, 10);
        CreateRegistrationRequest request = new CreateRegistrationRequest(
                "Nguyen Van B",
                "b@example.com",
                List.of(item)
        );

        EventResponse event = new EventResponse(1L, "Event 1", 50.0, 5);
        when(eventGatewayService.getEventById(1L)).thenReturn(event);

        assertThrows(IllegalArgumentException.class, () -> registrationService.createRegistration(request));

        verify(registrationRepository, never()).save(any());
        verify(registrationDetailRepository, never()).saveAll(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void createRegistrationThrowsWhenQuantityIsZeroOrNegative() {
        CreateRegistrationDetailRequest item = new CreateRegistrationDetailRequest(1L, 0);
        CreateRegistrationRequest request = new CreateRegistrationRequest(
                "Nguyen Van C",
                "c@example.com",
                List.of(item)
        );

        assertThrows(IllegalArgumentException.class, () -> registrationService.createRegistration(request));

        verify(registrationRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void createRegistrationThrowsWhenEventNotFound() {
        CreateRegistrationDetailRequest item = new CreateRegistrationDetailRequest(999L, 1);
        CreateRegistrationRequest request = new CreateRegistrationRequest(
                "Nguyen Van D",
                "d@example.com",
                List.of(item)
        );

        when(eventGatewayService.getEventById(999L)).thenThrow(new EventNotFoundException(999L));

        assertThrows(EventNotFoundException.class, () -> registrationService.createRegistration(request));

        verify(registrationRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    void createRegistrationThrowsWhenEventServiceUnavailable() {
        CreateRegistrationDetailRequest item = new CreateRegistrationDetailRequest(1L, 1);
        CreateRegistrationRequest request = new CreateRegistrationRequest(
                "Nguyen Van E",
                "e@example.com",
                List.of(item)
        );

        when(eventGatewayService.getEventById(1L)).thenThrow(new EventServiceException("Down"));

        assertThrows(EventServiceException.class, () -> registrationService.createRegistration(request));

        verify(registrationRepository, never()).save(any());
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }
}
