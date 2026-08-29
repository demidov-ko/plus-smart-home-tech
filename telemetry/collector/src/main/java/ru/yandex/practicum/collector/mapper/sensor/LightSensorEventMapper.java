package ru.yandex.practicum.collector.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.LightSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class LightSensorEventMapper implements SensorEventMapper {
    @Override
    public SensorEventProto.PayloadCase getPayloadCase() {
        return SensorEventProto.PayloadCase.LIGHT_SENSOR;
    }

    @Override
    public SensorEventAvro map(SensorEventProto event) {
        LightSensorProto source = event.getLightSensor();

        LightSensorAvro payload = LightSensorAvro.newBuilder()
                .setLinkQuality(source.getLinkQuality())
                .setLuminosity(source.getLuminosity())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }
}
