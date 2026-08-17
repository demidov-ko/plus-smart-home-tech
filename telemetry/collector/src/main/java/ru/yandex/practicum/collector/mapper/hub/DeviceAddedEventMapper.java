package ru.yandex.practicum.collector.mapper.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.DeviceAddedEvent;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.hub.types.HubEventType;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
public class DeviceAddedEventMapper implements HubEventMapper {

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_ADDED;
    }

    @Override
    public HubEventAvro map(HubEvent event) {
        DeviceAddedEvent source = (DeviceAddedEvent) event;

        DeviceAddedEventAvro payload = DeviceAddedEventAvro.newBuilder()
                .setId(source.getId())
                .setType(DeviceTypeAvro.valueOf(source.getDeviceType().name()))
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
