package ru.yandex.practicum.collector.mapper.sensor;

import ru.yandex.practicum.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.collector.model.sensor.SensorEventType;
import ru.yandex.practicum.collector.model.sensor.TemperatureSensorEvent;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.grpc.telemetry.event.TemperatureSensorProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

public class TemperatureSensorEventMapper implements SensorEventMapper {

    @Override
    public SensorEventProto.PayloadCase getPayloadCase() {
        return SensorEventProto.PayloadCase.TEMPERATURE_SENSOR;
    }

    @Override
    public SensorEventAvro map(SensorEventProto event) {
        TemperatureSensorProto source = event.getTemperatureSensor();

        // id, hubId и timestamp мы по прежнему должны взять с верхнего уровня SensorEventProto и продублировать их
        TemperatureSensorAvro payload = TemperatureSensorAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setTemperatureC(source.getTemperatureC())
                .setTemperatureF(source.getTemperatureF())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }
}
