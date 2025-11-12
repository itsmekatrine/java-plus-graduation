package ru.practicum.handler;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.mapper.UserActionWeightMapper;
import ru.practicum.model.EventSimilarity;
import ru.practicum.model.UserAction;
import ru.practicum.repository.EventSimilarityRepository;
import ru.practicum.repository.UserActionRepository;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class RecommendationsHandler {
    private final UserActionRepository userActionRepository;
    private final EventSimilarityRepository similarityRepository;
    private final UserActionWeightMapper mapper;

    public List<RecommendedEventProto> getRecommendationsForUser(UserPredictionsRequestProto request) {
        Long userId = request.getUserId();
        int limit = request.getMaxResults();
        PageRequest pageRequest = PageRequest.of(0, limit);

        Set<Long> viewedEventIds = userActionRepository.findAllByUserId(userId, pageRequest).stream()
                .map(UserAction::getEventId)
                .collect(Collectors.toSet());

        if (viewedEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        Set<Long> candidateEventIds = findCandidateRecommendations(userId, viewedEventIds, limit);

        return generateRecommendations(candidateEventIds, userId, limit);
    }

    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        Long eventId = request.getEventId();
        Long userId = request.getUserId();
        int limit = request.getMaxResults();
        PageRequest pageRequest = PageRequest.of(0, limit);

        List<EventSimilarity> aSimilarities = similarityRepository.findAllByEventA(eventId, pageRequest);
        List<EventSimilarity> bSimilarities = similarityRepository.findAllByEventB(eventId, pageRequest);

        List<RecommendedEventProto> recommendations = new ArrayList<>();

        addFilteredRecommendations(recommendations, aSimilarities,true, userId);
        addFilteredRecommendations(recommendations, bSimilarities,false, userId);

        recommendations.sort(Comparator.comparing(RecommendedEventProto::getScore).reversed());

        return recommendations.size() > request.getMaxResults()
                ? recommendations.subList(0, request.getMaxResults())
                : recommendations;
    }

    public List<RecommendedEventProto> getInteractionsCount(InteractionsCountRequestProto request) {
        Set<Long> eventIds = new HashSet<>(request.getEventIdList());

        Map<Long, Double> eventScores = new HashMap<>();

        userActionRepository.findAllByEventIdIn(eventIds)
                .forEach(action -> {
                    long eventId = action.getEventId();
                    double weight = mapper.mapToWeight(action.getActionType());
                    eventScores.merge(eventId, weight, Double::sum);
                });

        return eventScores.entrySet().stream()
                .map(entry -> createRecommendation(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Set<Long> findCandidateRecommendations(Long userId, Set<Long> viewedEventIds, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);

        List<EventSimilarity> aSimilarities = similarityRepository.findAllByEventAIn(viewedEventIds, pageRequest);
        List<EventSimilarity> bSimilarities = similarityRepository.findAllByEventBIn(viewedEventIds, pageRequest);

        Set<Long> recommendations = new HashSet<>();

        addNewEventsFromSimilarities(aSimilarities, true, userId, recommendations);
        addNewEventsFromSimilarities(bSimilarities, false, userId, recommendations);

        return recommendations;
    }

    private void addNewEventsFromSimilarities(List<EventSimilarity> similarities,
                                              boolean isEventB,
                                              Long userId,
                                              Set<Long> result) {
        Set<Long> candidateIds = prepareCandidates(similarities, isEventB);

        if (candidateIds.isEmpty()) {
            return;
        }

        Set<Long> existingUsersEventIds = userActionRepository.findEventIdsByUserIdAndEventIdIn(userId, candidateIds);

        candidateIds.removeAll(existingUsersEventIds);
        result.addAll(candidateIds);
    }

    private List<RecommendedEventProto> generateRecommendations(Set<Long> candidateEventIds,
                                                                Long userId,
                                                                int limit) {
        if (candidateEventIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<EventSimilarity> allSimilarities = getEventSimilaritiesForCandidates(candidateEventIds);
        Map<Long, List<EventSimilarity>> similaritiesByCandidate = groupSimilaritiesByCandidateId(allSimilarities);
        Set<Long> allRelatedEventIds = collectRelatedEventsBySimilarities(allSimilarities);
        Map<Long, UserAction> userActions = getUserActionsForRelatedEvents(userId, allRelatedEventIds);
        Map<Long, Double> eventScores = calculateScoresForCandidates(candidateEventIds, similaritiesByCandidate, userActions);

        return eventScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .map(entry -> createRecommendation(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Map<Long, Double> calculateScoresForCandidates(Set<Long> candidateEventIds,
                                                           Map<Long, List<EventSimilarity>> similaritiesByCandidate,
                                                           Map<Long, UserAction> userActions) {
        Map<Long, Double> eventScores = new HashMap<>();
        for (Long candidateId : candidateEventIds) {
            List<EventSimilarity> candidateSimilarities = similaritiesByCandidate
                    .getOrDefault(candidateId, Collections.emptyList());

            double sumWeightedRatings = 0.0;
            double sumSimilarityScores = 0.0;

            for (EventSimilarity es : candidateSimilarities) {
                Long relatedEventId = es.getEventA().equals(candidateId) ? es.getEventB() : es.getEventA();

                if (userActions.containsKey(relatedEventId)) {
                    double userRating = mapper.mapToWeight(userActions.get(relatedEventId).getActionType());
                    sumWeightedRatings += userRating * es.getScore();
                    sumSimilarityScores += es.getScore();
                }
            }

            double score = sumSimilarityScores > 0 ? sumWeightedRatings / sumSimilarityScores : 0.0;
            eventScores.put(candidateId, score);
        }
        return eventScores;
    }

    private Map<Long, UserAction> getUserActionsForRelatedEvents(Long userId, Set<Long> allRelatedEventIds) {
        return userActionRepository.findAllByEventIdInAndUserId(allRelatedEventIds, userId).stream()
                .collect(Collectors.toMap(UserAction::getEventId, Function.identity()));
    }

    private Set<Long> collectRelatedEventsBySimilarities(List<EventSimilarity> allSimilarities) {
        return allSimilarities.stream()
                .flatMap(es -> Stream.of(es.getEventA(), es.getEventB()))
                .collect(Collectors.toSet());
    }

    private Map<Long, List<EventSimilarity>> groupSimilaritiesByCandidateId(List<EventSimilarity> allSimilarities) {
        Map<Long, List<EventSimilarity>> similaritiesByCandidate = new HashMap<>();
        for (EventSimilarity es : allSimilarities) {
            similaritiesByCandidate.computeIfAbsent(es.getEventA(), v -> new ArrayList<>()).add(es);
            similaritiesByCandidate.computeIfAbsent(es.getEventB(), v -> new ArrayList<>()).add(es);
        }
        return similaritiesByCandidate;
    }

    private List<EventSimilarity> getEventSimilaritiesForCandidates(Set<Long> candidateEventIds) {
        List<EventSimilarity> allSimilarities = new ArrayList<>();
        allSimilarities.addAll(similarityRepository.findAllByEventAIn(candidateEventIds));
        allSimilarities.addAll(similarityRepository.findAllByEventBIn(candidateEventIds));
        return allSimilarities;
    }

    private void addFilteredRecommendations(List<RecommendedEventProto> recommendations,
                                            List<EventSimilarity> similarities,
                                            boolean isEventB,
                                            Long userId) {
        Set<Long> candidateIds = prepareCandidates(similarities, isEventB);

        if (candidateIds.isEmpty()) {
            return;
        }

        Set<Long> existingUsersEventIds = userActionRepository.findEventIdsByUserIdAndEventIdIn(userId, candidateIds);

        for (EventSimilarity es : similarities) {
            Long candidateEventId = isEventB ? es.getEventB() : es.getEventA();
            if (!existingUsersEventIds.contains(candidateEventId)) {
                recommendations.add(createRecommendation(candidateEventId, es.getScore()));
            }
        }
    }

    private Set<Long> prepareCandidates(List<EventSimilarity> similarities, boolean isEventB) {
        Set<Long> candidateIds = new HashSet<>();
        for (EventSimilarity es : similarities) {
            candidateIds.add(isEventB ? es.getEventB() : es.getEventA());
        }
        return candidateIds;
    }

    private RecommendedEventProto createRecommendation(Long eventId, Double score) {
        return RecommendedEventProto.newBuilder()
                .setEventId(eventId)
                .setScore(score)
                .build();
    }
}