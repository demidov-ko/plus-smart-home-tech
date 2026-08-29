package ru.yandex.practicum.collector.mapper.sensor;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.ClimateSensorProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

@Component
public class ClimateSensorEventMapper implements SensorEventMapper {

    @Override
    public SensorEventProto.PayloadCase getPayloadCase() {
        return SensorEventProto.PayloadCase.CLIMATE_SENSOR;
    }

    @Override
    // Используем SensorEventProto из Protobuf для всего, а конкретные данные достаем геттером под конкретный oneof
    public SensorEventAvro map(SensorEventProto event) {
        ClimateSensorProto source = event.getClimateSensor();

        ClimateSensorAvro payload = ClimateSensorAvro.newBuilder()
                .setTemperatureC(source.getTemperatureC())
                .setHumidity(source.getHumidity())
                .setCo2Level(source.getCo2Level())
                .build();

        return SensorEventAvro.newBuilder()
                .setId(event.getId())
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    // необходим, т.к. Protobuf отдельная структура с полями seconds/nanos, а setTimestamp ожидает Instant
//    private Instant toInstant(Timestamp timestamp) {
//        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
//    }
}
