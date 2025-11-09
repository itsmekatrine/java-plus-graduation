package ru.practicum;

import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.*;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Slf4j
@Service
public class RecommendationsClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Stream<RecommendedEventProto> getSimilarEvents(long eventId, long userId, int maxResults) {
        try {
            log.info("Fetching similar events: eventId={}, userId={}, maxResults={}", eventId, userId, maxResults);
            SimilarEventsRequestProto request = SimilarEventsRequestProto.newBuilder()
                    .setEventId(eventId)
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = client.getSimilarEvents(request);

            return toStream(iterator);
        } catch (Exception e) {
            log.error("Failed to get similar events for event {}", eventId, e);
            throw new RuntimeException(e);
        }
    }

    public Stream<RecommendedEventProto> getRecommendationsForUser(long userId, int maxResults) {
        try {
            log.info("Fetching recommendations for user: userId={}, maxResults={}", userId, maxResults);
            UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                    .setUserId(userId)
                    .setMaxResults(maxResults)
                    .build();

            Iterator<RecommendedEventProto> iterator = client.getRecommendationsForUser(request);

            return toStream(iterator);
        } catch (Exception e) {
            log.error("Failed to get recommendations for user {}", userId, e);
            throw new RuntimeException(e);
        }
    }

    public Stream<RecommendedEventProto> getInteractionsCount(List<Long> eventIds) {
        try {
            log.info("Fetching interactions count for events");
            InteractionsCountRequestProto request = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Iterator<RecommendedEventProto> iterator = client.getInteractionsCount(request);

            return toStream(iterator);
        } catch (Exception e) {
            log.error("Failed to get interactions count for events {}", eventIds, e);
            throw new RuntimeException(e);
        }
    }

    private Stream<RecommendedEventProto> toStream(Iterator<RecommendedEventProto> iterator) {
        return StreamSupport.stream(
                Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED),
                false
        );
    }
}
