package ru.yandex.practicum.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

// @EnableConfigServer включает функциональность сервера конфигурации
// После этого приложение сможет отдавать конфигурацию клиентским сервисам по HTTP
@EnableConfigServer
@SpringBootApplication
public class ConfigServer {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServer.class, args);
    }
}

// Ручная проверка параметров через HTTP-запрос
// http://localhost:8888/aggregator/default для теста
// GET http://localhost:8888/analyzer/default
// GET http://localhost:8888/collector/default
// В ответе должен быть JSON с найденной конфигурацией