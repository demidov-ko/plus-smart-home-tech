package ru.yandex.practicum.collector.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.MotionSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class MotionSensorEventMapper implements SensorEventMapper {

    @Override
    public SensorEventProto.PayloadCase getPayloadCase() {
        return SensorEventProto.PayloadCase.MOTION_SENSOR;
    }

    @Override
    public SensorEventAvro map(SensorEventProto event) {
        MotionSensorProto source = event.getMotionSensor();

        MotionSensorAvro payload = MotionSensorAvro.newBuilder()
                .setLinkQuality(source.getLinkQuality())
                .setMotion(source.getMotion())
                .setVoltage(source.getVoltage())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }
}
