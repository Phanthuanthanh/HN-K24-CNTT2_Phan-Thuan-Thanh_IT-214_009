package org.example.registrationservice.models.services.impl;

import lombok.RequiredArgsConstructor;
import org.example.registrationservice.models.constants.RegistrationStatus;
import org.example.registrationservice.models.dto.requests.CreateRegistrationDetailRequest;
import org.example.registrationservice.models.dto.requests.CreateRegistrationRequest;
import org.example.registrationservice.models.dto.responses.EventResponse;
import org.example.registrationservice.models.dto.responses.RegistrationDetailResponse;
import org.example.registrationservice.models.dto.responses.RegistrationResponse;
import org.example.registrationservice.models.entities.Registration;
import org.example.registrationservice.models.entities.RegistrationDetail;
import org.example.registrationservice.models.repositories.RegistrationDetailRepository;
import org.example.registrationservice.models.repositories.RegistrationRepository;
import org.example.registrationservice.models.services.RegistrationService;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {
    private final RegistrationRepository registrationRepository;
    private final RegistrationDetailRepository registrationDetailRepository;
    private final EventGatewayService eventGatewayService;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    @Transactional
    public RegistrationResponse createRegistration(CreateRegistrationRequest request) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new IllegalArgumentException("Items cannot be empty");
        }

        record ItemData(CreateRegistrationDetailRequest item, EventResponse event, double lineTotal) {}
        List<ItemData> itemDataList = new ArrayList<>();
        double totalAmount = 0.0;

        for (CreateRegistrationDetailRequest item : request.items()) {
            if (item.quantity() == null || item.quantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            EventResponse event = eventGatewayService.getEventById(item.eventId());
            if (event.availableTickets() == null || item.quantity() > event.availableTickets()) {
                throw new IllegalArgumentException("Not enough available tickets for event ID: " + item.eventId());
            }
            double ticketPrice = event.ticketPrice() != null ? event.ticketPrice() : 0.0;
            double lineTotal = ticketPrice * item.quantity();
            totalAmount += lineTotal;
            itemDataList.add(new ItemData(item, event, lineTotal));
        }

        Registration registration = Registration.builder()
                .participantName(request.participantName())
                .participantEmail(request.participantEmail())
                .totalAmount(totalAmount)
                .status(RegistrationStatus.PENDING)
                .build();
        Registration savedRegistration = registrationRepository.save(registration);

        List<RegistrationDetail> details = new ArrayList<>();
        for (ItemData itemData : itemDataList) {
            RegistrationDetail detail = RegistrationDetail.builder()
                    .registration(savedRegistration)
                    .eventId(itemData.item().eventId())
                    .quantity(itemData.item().quantity())
                    .ticketPrice(itemData.event().ticketPrice())
                    .lineTotal(itemData.lineTotal())
                    .build();
            details.add(detail);
        }
        List<RegistrationDetail> savedDetails = registrationDetailRepository.saveAll(details);

        kafkaTemplate.send("registration-created", request.participantEmail());

        List<RegistrationDetailResponse> detailResponses = savedDetails.stream()
                .map(d -> new RegistrationDetailResponse(
                        d.getId(),
                        d.getEventId(),
                        d.getQuantity(),
                        d.getTicketPrice(),
                        d.getLineTotal()
                ))
                .toList();

        return new RegistrationResponse(
                savedRegistration.getId(),
                savedRegistration.getParticipantName(),
                savedRegistration.getParticipantEmail(),
                savedRegistration.getTotalAmount(),
                savedRegistration.getStatus(),
                detailResponses
        );
    }
}
