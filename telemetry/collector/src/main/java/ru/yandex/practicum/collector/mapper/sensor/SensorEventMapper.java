package ru.yandex.practicum.collector.mapper.sensor;

import com.google.protobuf.Timestamp;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Instant;

// Интерфейс объединяет все мапперы для SensorEventProto-событий
// можно будет внедрить все мапперы в виде списка в компонент,
// который будет распределять получаемые события по их обработчикам
public interface SensorEventMapper {
    // PayloadCase - это перечисление, которое появляется у oneof полей, генерируемое Protobuf
    SensorEventProto.PayloadCase getPayloadCase();

    SensorEventAvro map(SensorEventProto event);

    // необходим, т.к. Protobuf отдельная структура с полями seconds/nanos, а setTimestamp ожидает Instant
    default Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
