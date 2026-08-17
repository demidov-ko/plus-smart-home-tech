package ru.yandex.practicum.collector.mapper.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.DeviceRemovedEvent;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.hub.types.HubEventType;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
public class DeviceRemovedEventMapper implements HubEventMapper {

    @Override
    public HubEventType getType() {
        return HubEventType.DEVICE_REMOVED;
    }

    @Override
    public HubEventAvro map(HubEvent event) {
        DeviceRemovedEvent source = (DeviceRemovedEvent) event;

        DeviceRemovedEventAvro payload = DeviceRemovedEventAvro.newBuilder()
                .setId(source.getId())
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
