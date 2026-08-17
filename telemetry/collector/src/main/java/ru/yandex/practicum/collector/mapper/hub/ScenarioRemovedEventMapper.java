package ru.yandex.practicum.collector.mapper.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.collector.model.hub.HubEvent;
import ru.yandex.practicum.collector.model.hub.ScenarioRemovedEvent;
import ru.yandex.practicum.collector.model.hub.types.HubEventType;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;

@Component
public class ScenarioRemovedEventMapper implements HubEventMapper {

    @Override
    public HubEventType getType() {
        return HubEventType.SCENARIO_REMOVED;
    }

    @Override
    public HubEventAvro map(HubEvent event) {
        ScenarioRemovedEvent source = (ScenarioRemovedEvent) event;

        ScenarioRemovedEventAvro payload = ScenarioRemovedEventAvro.newBuilder()
                .setName(source.getName())
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(source.getHubId())
                .setTimestamp(source.getTimestamp())
                .setPayload(payload)
                .build();
    }
}
