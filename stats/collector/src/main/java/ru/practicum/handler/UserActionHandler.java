package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.kafka.KafkaClient;

import java.time.Instant;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionHandler {
    private final KafkaClient kafkaClient;

    public void handle(UserActionProto proto) {
        UserActionAvro userAction = UserActionAvro.newBuilder()
                .setUserId(proto.getUserId())
                .setEventId(proto.getEventId())
                .setActionType(mapActionToAvro(proto.getActionType()))
                .setTimestamp(Instant.ofEpochSecond(
                        proto.getTimestamp().getSeconds(),
                        proto.getTimestamp().getNanos())
                ).build();

        send(userAction);

        log.info("Message sent to {}", kafkaClient.getUserActionTopic());
    }

    private ActionTypeAvro mapActionToAvro(ActionTypeProto actionType) {
        return switch (actionType) {
            case ACTION_VIEW -> ActionTypeAvro.VIEW;
            case ACTION_REGISTER -> ActionTypeAvro.REGISTER;
            case ACTION_LIKE -> ActionTypeAvro.LIKE;
            default -> throw new IllegalArgumentException("Unknown action type: " + actionType);
        };
    }

    private void send(UserActionAvro avro) {
        ProducerRecord<Long, SpecificRecordBase> producerRecord = new ProducerRecord<>(
                kafkaClient.getUserActionTopic(),
                null,
                avro.getTimestamp().toEpochMilli(),
                avro.getUserId(),
                avro);

        kafkaClient.getProducer().send(producerRecord);
    }
}
