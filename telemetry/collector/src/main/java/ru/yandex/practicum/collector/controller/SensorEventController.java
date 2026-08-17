package ru.yandex.practicum.collector.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.service.SensorEventService;

@RestController
@RequestMapping("/events/sensors")
@RequiredArgsConstructor
public class SensorEventController {

    private final SensorEventService service;

    @PostMapping
    public void collectSensorEvent(@Valid @RequestBody SensorEvent event) {
        service.collect(event);
    }
}
