package ru.practicum.controller;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.practicum.ewm.stats.proto.*;
import ru.practicum.handler.RecommendationsHandler;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class AnalyzerController extends RecommendationsControllerGrpc.RecommendationsControllerImplBase {
    private final RecommendationsHandler handler;

    @Override
    public void getRecommendationsForUser(UserPredictionsRequestProto request,
                                          StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Received get recommendations for user request: {}", request);
            handler.getRecommendationsForUser(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
            log.error("Error while handling get recommendations for user request", e);
        }
    }

    @Override
    public void getSimilarEvents(SimilarEventsRequestProto request,
                                 StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Received get similar events request: {}", request);
            handler.getSimilarEvents(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
            log.error("Error while handling get similar events request", e);
        }
    }

    @Override
    public void getInteractionsCount(InteractionsCountRequestProto request,
                                     StreamObserver<RecommendedEventProto> responseObserver) {
        try {
            log.info("Received get interactions count request: {}", request);
            handler.getInteractionsCount(request).forEach(responseObserver::onNext);
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
            log.error("Error while handling get interactions count request", e);
        }
    }
}
