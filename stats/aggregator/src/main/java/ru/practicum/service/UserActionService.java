package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class UserActionService {
    private final Map<Long, Map<Long, Double>> eventActions = new HashMap<>();
    private final Map<Long, Double> eventWeightSums = new HashMap<>();
    private final Map<Long, Map<Long, Double>> eventMinWeightSums = new HashMap<>();
    private final UserActionWeights weights;

    public Collection<EventSimilarityAvro> handle(UserActionAvro userAction) {
        long userId = userAction.getUserId();
        long eventId = userAction.getEventId();

        Map<Long, Double> userActions = eventActions.computeIfAbsent(eventId, v -> new HashMap<>());
        double currentWeight = userActions.getOrDefault(userId, 0.0);
        double newWeight = mapToWeight(userAction.getActionType());

        if (newWeight <= currentWeight) {
            return List.of();
        }

        updateEventWeights(userId, eventId, currentWeight, newWeight, userActions);
        return calculateSimilarities(userAction, eventId, userId, currentWeight, newWeight);
    }

    private List<EventSimilarityAvro> calculateSimilarities(UserActionAvro userAction,
                                                            long eventId,
                                                            long userId,
                                                            double currentWeight,
                                                            double newWeight) {
        return eventActions.keySet().stream()
                .filter(otherEventId -> !otherEventId.equals(eventId))
                .map(otherEventId -> getEntry(userId, otherEventId))
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> calculateEventSimilarity(
                        userAction,
                        eventId,
                        currentWeight,
                        newWeight,
                        entry.getKey(),
                        entry.getValue()))
                .toList();
    }


    private Map.Entry<Long, Double> getEntry(long userId, Long otherEventId) {
        return Map.entry(otherEventId,
                eventActions.get(otherEventId).getOrDefault(userId, 0.0));
    }

    private EventSimilarityAvro calculateEventSimilarity(UserActionAvro userAction,
                                                         long eventId,
                                                         double currentWeight,
                                                         double newWeight,
                                                         long otherEventId,
                                                         double otherWeight) {
        double updatedMinSum = updateMinWeightSum(
                eventId, otherEventId, currentWeight, newWeight, otherWeight
        );
        double similarity = calculateSimilarity(eventId, otherEventId, updatedMinSum);
        return createAvro(eventId, otherEventId, similarity, userAction.getTimestamp());
    }

    private void updateEventWeights(long userId,
                                    long eventId,
                                    double currentWeight,
                                    double newWeight,
                                    Map<Long, Double> userActions) {
        double currentSum = eventWeightSums.getOrDefault(eventId, 0.0);
        eventWeightSums.put(eventId, currentSum - currentWeight + newWeight);
        userActions.put(userId, newWeight);
    }

    private double updateMinWeightSum(long eventId,
                                      long otherEventId,
                                      double currentWeight,
                                      double newWeight,
                                      double otherWeight) {
        long minEventId = Math.min(eventId, otherEventId);
        long maxEventId = Math.max(eventId, otherEventId);

        Map<Long, Double> minSums = eventMinWeightSums.computeIfAbsent(minEventId, v -> new HashMap<>());
        double currentMinSum = minSums.getOrDefault(maxEventId, 0.0);

        double updatedMinSum = calculateUpdatedMinSum(
                currentMinSum, currentWeight, newWeight, otherWeight
        );

        minSums.put(maxEventId, updatedMinSum);
        return updatedMinSum;
    }

    private double calculateUpdatedMinSum(double currentMinSum,
                                          double currentWeight,
                                          double newWeight,
                                          double otherWeight) {
        double currentMin = Math.min(currentWeight, otherWeight);
        double newMin = Math.min(newWeight, otherWeight);
        return currentMinSum - currentMin + newMin;
    }

    private double calculateSimilarity(long eventId, long otherEventId, double minSum) {
        double sum1 = eventWeightSums.getOrDefault(eventId, 0.0);
        double sum2 = eventWeightSums.getOrDefault(otherEventId, 0.0);

        double sumProduct = sum1 * sum2;
        return sumProduct > 0 ? minSum / Math.sqrt(sumProduct) : 0.0;
    }

    private double mapToWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> weights.getView();
            case REGISTER -> weights.getRegister();
            case LIKE -> weights.getLike();
        };
    }

    private EventSimilarityAvro createAvro(long eventId, long otherEventId, double similarity, Instant timestamp) {
        long eventA = Math.min(eventId, otherEventId);
        long eventB = Math.max(eventId, otherEventId);

        return EventSimilarityAvro.newBuilder()
                .setEventA(eventA)
                .setEventB(eventB)
                .setScore(similarity)
                .setTimestamp(timestamp)
                .build();
    }
}