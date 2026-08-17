package ru.yandex.practicum.collector.mapper.sensor;

import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

public interface SensorEventMapper {
    SensorEventType getType();

    SensorEventAvro map(SensorEvent event);
}
