package ru.practicum.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.mapper.UserActionWeightMapper;
import ru.practicum.model.UserAction;
import ru.practicum.model.ActionType;
import ru.practicum.repository.UserActionRepository;

import java.time.Instant;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class UserActionHandler {
    private final UserActionRepository userActionRepository;
    private final UserActionWeightMapper mapper;

    public void handle(UserActionAvro avro) {
        ActionType newActionType = ActionType.valueOf(avro.getActionType().name());
        Double newWeight = mapper.mapToWeight(newActionType);

        userActionRepository
                .findByUserIdAndEventId(avro.getUserId(), avro.getEventId())
                .ifPresentOrElse(
                        existingAction -> updateIfHigherWeight(existingAction, newActionType, newWeight, avro.getTimestamp()),
                        () -> saveNewAction(avro)
                );
    }

    private void updateIfHigherWeight(UserAction existingAction, ActionType newActionType, Double newWeight, Instant timestamp) {
        Double currentWeight = mapper.mapToWeight(existingAction.getActionType());

        if (newWeight > currentWeight) {
            existingAction.setActionType(newActionType);
            existingAction.setTimestamp(timestamp);
            userActionRepository.save(existingAction);
            log.debug("Upgrading action {} to higher type {}", existingAction.getId(), newActionType);
        }
    }

    private void saveNewAction(UserActionAvro avro) {
        UserAction userAction = new UserAction();
        userAction.setUserId(avro.getUserId());
        userAction.setEventId(avro.getEventId());
        userAction.setActionType(ActionType.valueOf(avro.getActionType().name()));
        userAction.setTimestamp(avro.getTimestamp());

        userActionRepository.save(userAction);
        log.debug("Created new user action: {}", userAction);
    }
}