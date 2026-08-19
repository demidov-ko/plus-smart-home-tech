package ru.yandex.practicum.collector.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.LightSensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class LightSensorEventMapper implements SensorEventMapper {
    @Override
    public SensorEventType getType() {
        return SensorEventType.LIGHT_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro map(SensorEvent event) {
        LightSensorEvent source = (LightSensorEvent) event;

        LightSensorAvro payload = LightSensorAvro.newBuilder()
                .setLinkQuality(source.getLinkQuality())
                .setLuminosity(source.getLuminosity())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(source.getId())
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
