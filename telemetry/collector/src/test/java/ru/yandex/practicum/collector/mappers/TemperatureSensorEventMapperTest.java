package ru.yandex.practicum.collector.mappers;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.collector.mapper.sensor.TemperatureSensorEventMapper;
import ru.yandex.practicum.collector.model.sensor.TemperatureSensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class TemperatureSensorEventMapperTest {

    private final TemperatureSensorEventMapper mapper = new TemperatureSensorEventMapper();

    @Test
    void shouldDuplicateIdHubIdTimestampInPayload() {
        // given
        TemperatureSensorEvent event = new TemperatureSensorEvent();
        event.setId("sensor.temp.9");
        event.setHubId("hub-3");
        Instant timestamp = Instant.parse("2026-08-17T20:10:25.150Z");
        event.setTimestamp(timestamp);
        event.setTemperatureC(21);
        event.setTemperatureF(70);

        // when
        SensorEventAvro result = mapper.map(event);

        // then верхний уровень
        assertThat(result.getId()).isEqualTo("sensor.temp.9");
        assertThat(result.getHubId()).isEqualTo("hub-3");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);

        // then вложенный payload (id/hubId/timestamp продублированы)
        assertThat(result.getPayload()).isInstanceOf(TemperatureSensorAvro.class);
        TemperatureSensorAvro payload = (TemperatureSensorAvro) result.getPayload();
        assertThat(payload.getId()).isEqualTo("sensor.temp.9");
        assertThat(payload.getHubId()).isEqualTo("hub-3");
        assertThat(payload.getTimestamp()).isEqualTo(timestamp);
        assertThat(payload.getTemperatureC()).isEqualTo(21);
        assertThat(payload.getTemperatureF()).isEqualTo(70);
    }
}
