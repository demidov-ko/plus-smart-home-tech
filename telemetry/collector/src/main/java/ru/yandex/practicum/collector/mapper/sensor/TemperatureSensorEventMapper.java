package ru.yandex.practicum.collector.mapper.sensor;

import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.collector.model.sensor.TemperatureSensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

public class TemperatureSensorEventMapper implements SensorEventMapper {

    @Override
    public SensorEventType getType() {
        return SensorEventType.TEMPERATURE_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro map(SensorEvent event) {
        TemperatureSensorEvent source = (TemperatureSensorEvent) event;

        TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
                .setId(source.getId())
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setTemperatureC(source.getTemperatureC())
                .setTemperatureF(source.getTemperatureF())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(source.getId())
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
