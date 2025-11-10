package ru.practicum.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@RequiredArgsConstructor
@ConfigurationProperties(prefix = "app.user-action.weights")
public class UserActionWeights {
    private final double view;
    private final double register;
    private final double like;
}
