package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import ru.practicum.controller.ErrorHandler;

@SpringBootApplication(scanBasePackageClasses = {
        UserServer.class,
        ErrorHandler.class
})
public class UserServer {
    public static void main(String[] args) {
        SpringApplication.run(UserServer.class, args);
    }
}