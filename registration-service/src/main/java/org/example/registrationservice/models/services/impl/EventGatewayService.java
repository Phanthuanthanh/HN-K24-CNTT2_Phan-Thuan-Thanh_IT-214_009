package org.example.registrationservice.models.services.impl;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.example.registrationservice.clients.EventClient;
import org.example.registrationservice.exceptions.EventNotFoundException;
import org.example.registrationservice.exceptions.EventServiceException;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EventGatewayService {
    private final EventClient eventClient;

    @CircuitBreaker(name = "eventService", fallbackMethod = "getEventByIdFallback")
    public EventResponse getEventById(Long eventId) {
        EventResponse response = eventClient.getEventById(eventId);
        if (response == null || response.id() == null) {
            throw new EventNotFoundException(eventId);
        }
        return response;
    }

    public EventResponse getEventByIdFallback(Long eventId, Throwable throwable) {
        if (throwable instanceof EventNotFoundException) {
            throw (EventNotFoundException) throwable;
        }
        if (throwable instanceof FeignException.NotFound || (throwable instanceof FeignException fe && fe.status() == 404)) {
            throw new EventNotFoundException(eventId);
        }
        Throwable cause = throwable.getCause();
        while (cause != null) {
            if (cause instanceof EventNotFoundException) {
                throw (EventNotFoundException) cause;
            }
            if (cause instanceof FeignException.NotFound || (cause instanceof FeignException fe && fe.status() == 404)) {
                throw new EventNotFoundException(eventId);
            }
            cause = cause.getCause();
        }
        throw new EventServiceException("Event service is unavailable", throwable);
    }
}
