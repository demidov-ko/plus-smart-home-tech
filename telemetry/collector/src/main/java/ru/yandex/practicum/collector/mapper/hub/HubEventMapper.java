package ru.yandex.practicum.collector.mapper.hub;

import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.hub.types.HubEventType;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

public interface HubEventMapper {
    HubEventType getType();

    HubEventAvro map(HubEvent event);
}
