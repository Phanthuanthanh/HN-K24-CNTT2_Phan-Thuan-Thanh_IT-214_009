package org.example.eventservice.models.services.impl;

import org.example.eventservice.exceptions.EventNotFoundException;
import org.example.eventservice.models.entities.Event;
import org.example.eventservice.models.repositories.EventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.annotation.Cacheable;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventServiceImpl eventService;

    @Test
    void getEventByIdSuccess() {
        Event event = Event.builder()
                .id(1L)
                .eventName("Tech Conference")
                .ticketPrice(100.0)
                .availableTickets(50)
                .build();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        Event result = eventService.getEventById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Tech Conference", result.getEventName());
        assertEquals(100.0, result.getTicketPrice());
        assertEquals(50, result.getAvailableTickets());
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    void getEventByIdNotFoundThrowsException() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(EventNotFoundException.class, () -> eventService.getEventById(999L));
        verify(eventRepository, times(1)).findById(999L);
    }

    @Test
    void getEventByIdHasCacheableAnnotation() throws NoSuchMethodException {
        Method method = EventServiceImpl.class.getMethod("getEventById", Long.class);
        Cacheable cacheable = method.getAnnotation(Cacheable.class);

        assertNotNull(cacheable);
        assertArrayEquals(new String[]{"events"}, cacheable.value());
        assertEquals("#id", cacheable.key());
        assertTrue(cacheable.sync());
    }
}
