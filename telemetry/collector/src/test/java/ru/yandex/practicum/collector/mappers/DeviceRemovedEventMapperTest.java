package ru.yandex.practicum.collector.mappers;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.collector.mapper.hub.DeviceRemovedEventMapper;
import ru.yandex.practicum.collector.model.hub.DeviceRemovedEvent;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class DeviceRemovedEventMapperTest {

    private final DeviceRemovedEventMapper mapper = new DeviceRemovedEventMapper();

    @Test
    void shouldMapFieldsCorrectly() {
        DeviceRemovedEvent event = new DeviceRemovedEvent();
        event.setHubId("hub-1");
        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        event.setTimestamp(timestamp);
        event.setId("sensor.motion.1");

        HubEventAvro result = mapper.map(event);

        assertThat(result.getHubId()).isEqualTo("hub-1");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);

        DeviceRemovedEventAvro payload = (DeviceRemovedEventAvro) result.getPayload();
        assertThat(payload.getId()).isEqualTo("sensor.motion.1");
    }
}
