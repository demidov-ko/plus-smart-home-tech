package ru.yandex.practicum.collector.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.sensor.MotionSensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class MotionSensorEventMapper implements SensorEventMapper {

    @Override
    public SensorEventType getType() {
        return SensorEventType.MOTION_SENSOR_EVENT;
    }

    @Override
    public SensorEventAvro map(SensorEvent event) {
        MotionSensorEvent source = (MotionSensorEvent) event;

        MotionSensorAvro payload = MotionSensorAvro.newBuilder()
                .setLinkQuality(source.getLinkQuality())
                .setMotion(source.isMotion())
                .setVoltage(source.getVoltage())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(source.getId())
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
