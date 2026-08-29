package ru.yandex.practicum.collector.mapper.hub;

import com.google.protobuf.Timestamp;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Instant;

public interface HubEventMapper {
    HubEventProto.PayloadCase getPayloadCase();

    HubEventAvro map(HubEventProto event);

    default Instant toInstant(Timestamp timestamp) {
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
    }
}
