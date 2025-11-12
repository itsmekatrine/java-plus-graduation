package ru.practicum;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import ru.practicum.kafka.EventSimilarityListener;
import ru.practicum.kafka.UserActionListener;

@Component
@RequiredArgsConstructor
public class AnalyzerStarter implements CommandLineRunner {
    private final UserActionListener userActionListener;
    private final EventSimilarityListener similarityListener;

    @Override
    public void run(String... args) {
        Thread userActionThread = new Thread(userActionListener);
        userActionThread.setName("UserActionListenerThread");
        userActionThread.start();
        similarityListener.run();
    }
}
