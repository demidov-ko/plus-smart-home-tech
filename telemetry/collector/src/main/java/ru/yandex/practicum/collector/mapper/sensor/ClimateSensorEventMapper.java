package ru.yandex.practicum.collector.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.ClimateSensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class ClimateSensorEventMapper implements SensorEventMapper {

    @Override
    public SensorEventType getType() {
        return SensorEventType.CLIMATE_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro map(SensorEvent event) {
        ClimateSensorEvent source = (ClimateSensorEvent) event;

        ClimateSensorAvro payload = ClimateSensorAvro.newBuilder()
                .setTemperatureC(source.getTemperatureC())
                .setHumidity(source.getHumidity())
                .setCo2Level(source.getCo2Level())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(source.getId())
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
