package ru.practicum.specification;

import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import ru.practicum.entity.Event;
import ru.practicum.entity.EventState;
import ru.practicum.parameters.EventAdminSearchParam;
import ru.practicum.parameters.PublicSearchParam;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class EventSpecifications {
    public static Specification<Event> userIdIs(List<Long> userIds) {
        return (root, query, criteriaBuilder) -> {
            if (userIds == null || userIds.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return root.get("initiatorId").in(userIds);
        };
    }

    public static Specification<Event> categories(List<Long> categories) {
        return (root, query, criteriaBuilder) -> {
            if (categories == null || categories.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Path<Long> categoryIdPath = root.get("category").get("id");
            return categoryIdPath.in(categories);
        };
    }

    public static Specification<Event> states(List<EventState> states) {
        return (root, query, criteriaBuilder) -> {
            if (states == null || states.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            Path<EventState> statePath = root.get("state");
                return statePath.in(states);
        };
    }

    public static Specification<Event> startAfter(LocalDateTime rangeStart) {
        return (root, query, criteriaBuilder) ->
                rangeStart != null
                        ? criteriaBuilder.greaterThanOrEqualTo(root.get("eventDate"), rangeStart)
                        : criteriaBuilder.conjunction();
    }

    public static Specification<Event> startBefore(LocalDateTime rangeEnd) {
        return (root, query, criteriaBuilder) ->
                rangeEnd != null
                        ? criteriaBuilder.lessThanOrEqualTo(root.get("eventDate"), rangeEnd)
                        : criteriaBuilder.conjunction();
    }

    public static Specification<Event> isPaid(Boolean paid) {
        return (root, query, criteriaBuilder) -> {
            if (paid == null) {
                return criteriaBuilder.conjunction();
            } else {
                return criteriaBuilder.equal(root.get("paid"), paid);
            }
        };
    }

    public static Specification<Event> textInAnnotationOrDescription(String text) {
        return (root, query, criteriaBuilder) -> {
            if (text == null || text.isBlank()) {
                return criteriaBuilder.conjunction();
            }

            Predicate annotationPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("annotation")),
                    "%" + text.toLowerCase() + "%"
            );
            Predicate descriptionPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")),
                    "%" + text.toLowerCase() + "%"
            );

            return criteriaBuilder.or(annotationPredicate, descriptionPredicate);
        };
    }

    public static Specification<Event> eventAdminSearchParamSpec(EventAdminSearchParam params) {
        return Specification.where(EventSpecifications.userIdIs(params.getUsers()))
                .and(statesByAnyEnum(params.getStates()))
                .and(categories(params.getCategories()))
                .and(startBefore(params.getRangeEnd()))
                .and(startAfter(params.getRangeStart()));
    }

    public static Specification<Event> eventPublicSearchParamSpec(PublicSearchParam params) {
        LocalDateTime effectiveStart = params.getRangeStart();
        if (effectiveStart == null && params.getRangeEnd() == null) {
            effectiveStart = LocalDateTime.now();
        }

        return Specification.where(textInAnnotationOrDescription(params.getText()))
                .and(categories(params.getCategories()))
                .and(isPaid(params.getPaid()))
                .and(startBefore(params.getRangeEnd()))
                .and(startAfter(effectiveStart))
                .and(states(List.of(EventState.PUBLISHED)));
    }

    public static Specification<Event> statesByAnyEnum(Collection<? extends Enum<?>> states) {
        return (root, query, cb) -> {
            if (states == null || states.isEmpty()) return cb.conjunction();

            Path<EventState> statePath = root.get("state");

            List<EventState> entityStates = states.stream()
                    .map(Enum::name)
                    .map(name -> {
                        try {
                            return EventState.valueOf(name);
                        } catch (IllegalArgumentException ex) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (entityStates.isEmpty()) return cb.conjunction();
            return statePath.in(entityStates);
        };
    }
}
