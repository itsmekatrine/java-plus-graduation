package ru.practicum.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.model.ActionType;
import ru.practicum.model.UserActionWeights;

@Component
@RequiredArgsConstructor
public class UserActionWeightMapper {
    private final UserActionWeights weights;

    public Double mapToWeight(ActionType actionType) {
        return switch (actionType) {
            case VIEW -> weights.getView();
            case REGISTER -> weights.getRegister();
            case LIKE -> weights.getLike();
        };
    }
}
