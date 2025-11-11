package ru.practicum;

import com.google.protobuf.Descriptors;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendationsControllerGrpc;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AnalyzerClient {

    @GrpcClient("analyzer")
    private RecommendationsControllerGrpc.RecommendationsControllerBlockingStub client;

    public Map<Long, Double> getRatings(Collection<Long> eventIds) {
        if (eventIds == null || eventIds.isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            log.info("Fetching ratings (via interactions) for events: {}", eventIds);

            InteractionsCountRequestProto req = InteractionsCountRequestProto.newBuilder()
                    .addAllEventId(eventIds)
                    .build();

            Iterator<RecommendedEventProto> it =
                    client.withDeadlineAfter(800, TimeUnit.MILLISECONDS).getInteractionsCount(req);

            Map<Long, Double> result = new HashMap<>();
            while (it.hasNext()) {
                RecommendedEventProto rec = it.next();
                double score = extractNumericScore(rec);
                if (Double.isFinite(score)) {
                    result.put(rec.getEventId(), score);
                }
            }
            return result;
        } catch (io.grpc.StatusRuntimeException ex) {
            log.warn("Analyzer unavailable ({}). Continue without ratings. ids={}", ex.getStatus(), eventIds);
            return Collections.emptyMap();
        } catch (Exception ex) {
            log.warn("Analyzer error. Continue without ratings. ids={}", eventIds, ex);
            return Collections.emptyMap();
        }
    }

    private double extractNumericScore(RecommendedEventProto rec) {
        Map<Descriptors.FieldDescriptor, Object> fields = rec.getAllFields();
        for (Map.Entry<Descriptors.FieldDescriptor, Object> entry : fields.entrySet()) {
            String name = entry.getKey().getName();
            if ("score".equals(name) || "rating".equals(name)
                    || "cnt".equals(name) || "count".equals(name) || "interactions".equals(name)) {

                Object v = entry.getValue();
                if (v instanceof Number) {
                    return ((Number) v).doubleValue();
                }
                if (v instanceof CharSequence) {
                    try {
                        return Double.parseDouble(v.toString());
                    } catch (NumberFormatException ignore) {
                    }
                }
            }
        }
        return 0.0;
    }
}