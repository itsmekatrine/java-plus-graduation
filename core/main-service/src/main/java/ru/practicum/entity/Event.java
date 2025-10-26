package ru.practicum.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CurrentTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "event")
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    Long id;

    @Size(max = 120)
    @NotNull
    @Column(name = "title", nullable = false, length = 120)
    String title;

    @Size(max = 2000)
    @NotNull
    @Column(name = "annotation", nullable = false, length = 2000)
    String annotation;

    @Size(max = 7000)
    @Column(name = "description", length = 7000)
    String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 32)
    EventState state;

    @NotNull
    @Column(name = "event_date", nullable = false)
    LocalDateTime eventDate;

    @Column(name = "created_on")
    @CurrentTimestamp
    LocalDateTime createdOn;

    @Column(name = "published_on")
    LocalDateTime publishedOn;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.RESTRICT)
    @JoinColumn(name = "category_id", nullable = false)
    Category category;

    @NotNull
    @Column(name = "initiator_id", nullable = false)
    Long initiatorId;

    @Column(name = "paid")
    Boolean paid;

    @Column(name = "request_moderation")
    Boolean requestModeration;

    @Column(name = "participant_limit")
    Integer participantLimit;

    @NotNull
    @Column(name = "lat", nullable = false)
    Double lat;

    @NotNull
    @Column(name = "lon", nullable = false)
    Double lon;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "comment_pre_moderation", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "forbidden_word")
    Set<String> forbiddenWords;

}