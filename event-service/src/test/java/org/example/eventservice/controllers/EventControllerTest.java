package org.example.eventservice.controllers;

import org.example.eventservice.exceptions.EventNotFoundException;
import org.example.eventservice.exceptions.GlobalExceptionHandler;
import org.example.eventservice.models.entities.Event;
import org.example.eventservice.models.services.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    private MockMvc mockMvc;

    @Mock
    private EventService eventService;

    @InjectMocks
    private EventController eventController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getEventByIdReturns200WhenFound() throws Exception {
        Event event = Event.builder()
                .id(1L)
                .eventName("Music Festival")
                .ticketPrice(200.0)
                .availableTickets(100)
                .build();

        when(eventService.getEventById(1L)).thenReturn(event);

        mockMvc.perform(get("/api/v1/events/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.eventName").value("Music Festival"))
                .andExpect(jsonPath("$.ticketPrice").value(200.0))
                .andExpect(jsonPath("$.availableTickets").value(100));
    }

    @Test
    void getEventByIdReturns404WhenNotFound() throws Exception {
        when(eventService.getEventById(999L)).thenThrow(new EventNotFoundException(999L));

        mockMvc.perform(get("/api/v1/events/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
