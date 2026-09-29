package org.example.registrationservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.registrationservice.exceptions.GlobalExceptionHandler;
import org.example.registrationservice.models.constants.RegistrationStatus;
import org.example.registrationservice.models.dto.requests.CreateRegistrationDetailRequest;
import org.example.registrationservice.models.dto.requests.CreateRegistrationRequest;
import org.example.registrationservice.models.dto.responses.RegistrationDetailResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.example.registrationservice.models.services.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RegistrationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RegistrationService registrationService;

    @InjectMocks
    private RegistrationController registrationController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(registrationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void createRegistrationReturns201Created() throws Exception {
        CreateRegistrationDetailRequest item = new CreateRegistrationDetailRequest(1L, 2);
        CreateRegistrationRequest request = new CreateRegistrationRequest(
                "Nguyen Van A",
                "nguyenvana@example.com",
                List.of(item)
        );

        RegistrationDetailResponse detailResponse = new RegistrationDetailResponse(10L, 1L, 2, 100.0, 200.0);
        RegistrationResponse response = new RegistrationResponse(
                1L,
                "Nguyen Van A",
                "nguyenvana@example.com",
                200.0,
                RegistrationStatus.PENDING,
                List.of(detailResponse)
        );

        when(registrationService.createRegistration(any(CreateRegistrationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.participantName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.participantEmail").value("nguyenvana@example.com"))
                .andExpect(jsonPath("$.totalAmount").value(200.0))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.details[0].lineTotal").value(200.0));
    }

    @Test
    void createRegistrationWithInvalidDataReturns400BadRequest() throws Exception {
        CreateRegistrationRequest invalidRequest = new CreateRegistrationRequest(
                "",
                "invalid-email",
                List.of()
        );

        mockMvc.perform(post("/api/v1/registrations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }
}
