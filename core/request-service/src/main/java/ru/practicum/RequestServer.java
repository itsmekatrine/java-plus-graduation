package ru.practicum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import ru.practicum.controller.ErrorHandler;

@SpringBootApplication(scanBasePackageClasses = {
        RequestServer.class,
        ErrorHandler.class
})
@EnableFeignClients
public class RequestServer {
    public static void main(String[] args) {
        SpringApplication.run(RequestServer.class, args);
    }
}