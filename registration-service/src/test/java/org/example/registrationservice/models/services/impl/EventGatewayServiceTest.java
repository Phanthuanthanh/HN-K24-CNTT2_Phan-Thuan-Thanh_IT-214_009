package org.example.registrationservice.models.services.impl;

import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.example.registrationservice.clients.EventClient;
import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.exceptions.EventServiceException;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Method;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventGatewayServiceTest {

    @Mock
    private EventClient eventClient;

    @InjectMocks
    private EventGatewayService eventGatewayService;

    @Test
    void getEventByIdSuccess() {
        EventResponse mockResponse = new EventResponse(1L, "Concert", 150.0, 100);
        when(eventClient.getEventById(1L)).thenReturn(mockResponse);

        EventResponse result = eventGatewayService.getEventById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());
        assertEquals("Concert", result.eventName());
        assertEquals(150.0, result.ticketPrice());
        assertEquals(100, result.availableTickets());
        verify(eventClient, times(1)).getEventById(1L);
    }

    @Test
    void getEventByIdNullResponseThrowsEventNotFoundException() {
        when(eventClient.getEventById(2L)).thenReturn(null);

        assertThrows(EventNotFoundException.class, () -> eventGatewayService.getEventById(2L));
    }

    @Test
    void fallbackWithEventNotFoundExceptionRethrows() {
        EventNotFoundException ex = new EventNotFoundException(5L);

        assertThrows(EventNotFoundException.class, () -> eventGatewayService.getEventByIdFallback(5L, ex));
    }

    @Test
    void fallbackWithFeign404ThrowsEventNotFoundException() {
        Request request = Request.create(Request.HttpMethod.GET, "/api/v1/events/5", Collections.emptyMap(), null, new RequestTemplate());
        FeignException.NotFound notFound = new FeignException.NotFound("Not Found", request, null, null);

        assertThrows(EventNotFoundException.class, () -> eventGatewayService.getEventByIdFallback(5L, notFound));
    }

    @Test
    void fallbackWithServiceErrorThrowsEventServiceException() {
        RuntimeException runtimeException = new RuntimeException("Connection timed out");

        assertThrows(EventServiceException.class, () -> eventGatewayService.getEventByIdFallback(5L, runtimeException));
    }

    @Test
    void hasCircuitBreakerAnnotationWithCorrectNameAndFallback() throws NoSuchMethodException {
        Method method = EventGatewayService.class.getMethod("getEventById", Long.class);
        CircuitBreaker circuitBreaker = method.getAnnotation(CircuitBreaker.class);

        assertNotNull(circuitBreaker);
        assertEquals("eventService", circuitBreaker.name());
        assertEquals("getEventByIdFallback", circuitBreaker.fallbackMethod());
    }
}
