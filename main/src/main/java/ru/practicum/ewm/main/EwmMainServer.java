package ru.practicum.ewm.main;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"ru.practicum.ewm.stats.client",
        "ru.practicum.ewm.main.controllers",
        "ru.practicum.ewm.main.dto",
        "ru.practicum.ewm.main.enums",
        "ru.practicum.ewm.main.errorHandler",
        "ru.practicum.ewm.main.exceptions",
        "ru.practicum.ewm.main.mapper",
        "ru.practicum.ewm.main.model",
        "ru.practicum.ewm.main.repository",
        "ru.practicum.ewm.main.services",
        "ru.practicum.ewm.stats.dto"
})
public class EwmMainServer {
    public static void main(String[] args) {
        SpringApplication.run(EwmMainServer.class, args);
    }
}
