package ru.yandex.practicum.collector.mapper.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
public class DeviceAddedEventMapper implements HubEventMapper {

    @Override
    public HubEventProto.PayloadCase getPayloadCase() {
        return HubEventProto.PayloadCase.DEVICE_ADDED;
    }

    @Override
    public HubEventAvro map(HubEventProto event) {
        DeviceAddedEventProto source = event.getDeviceAdded();

        DeviceAddedEventAvro payload = DeviceAddedEventAvro.newBuilder()
                .setId(source.getId())
                // source.getType() возвращает DeviceTypeProto и конвертируем его в Avro-enum константы
                .setType(DeviceTypeAvro.valueOf(source.getType().name()))
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }
}
