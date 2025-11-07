package ru.practicum.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;
import ru.practicum.dto.request.RequestStatus;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder(toBuilder = true)
@Table(
        name = "participation_request",
        uniqueConstraints = @UniqueConstraint(name = "uq_request_event_requester", columnNames = {"event_id", "requester_id"})
)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ParticipationRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @NotNull
    @Column(name = "requester_id", nullable = false)
    Long requesterId;

    @NotNull
    @Column(name = "event_id", nullable = false)
    Long eventId;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    RequestStatus status;

    @Column(name = "created")
    LocalDateTime created;
}