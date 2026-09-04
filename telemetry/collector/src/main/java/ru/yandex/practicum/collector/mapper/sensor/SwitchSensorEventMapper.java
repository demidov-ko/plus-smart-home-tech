package ru.yandex.practicum.collector.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SwitchSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;

@Component
public class SwitchSensorEventMapper implements SensorEventMapper {

    @Override
    public SensorEventProto.PayloadCase getPayloadCase() {
        return SensorEventProto.PayloadCase.SWITCH_SENSOR;
    }

    @Override
    public SensorEventAvro map(SensorEventProto event) {
        SwitchSensorProto source = event.getSwitchSensor();

        SwitchSensorAvro payload = SwitchSensorAvro.newBuilder()
                .setState(source.getState())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }
}
