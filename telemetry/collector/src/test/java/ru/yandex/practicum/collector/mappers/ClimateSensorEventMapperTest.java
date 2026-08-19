package ru.yandex.practicum.collector.mappers;

import org.junit.jupiter.api.Test;
import ru.yandex.practicum.collector.mapper.sensor.ClimateSensorEventMapper;
import ru.yandex.practicum.collector.model.sensor.ClimateSensorEvent;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ClimateSensorEventMapperTest {

    private final ClimateSensorEventMapper mapper = new ClimateSensorEventMapper();

    @Test
    void shouldMapFieldsCorrectly() {
        // given
        ClimateSensorEvent event = new ClimateSensorEvent();
        event.setId("sensor.climate.7");
        event.setHubId("hub-2");
        Instant timestamp = Instant.parse("2026-08-17T20:10:25.150Z");
        event.setTimestamp(timestamp);
        event.setTemperatureC(23);
        event.setHumidity(55);
        event.setCo2Level(450);

        // when
        SensorEventAvro result = mapper.map(event);

        // then
        assertThat(result.getId()).isEqualTo("sensor.climate.7");
        assertThat(result.getHubId()).isEqualTo("hub-2");
        assertThat(result.getTimestamp()).isEqualTo(timestamp);

        assertThat(result.getPayload()).isInstanceOf(ClimateSensorAvro.class);
        ClimateSensorAvro payload = (ClimateSensorAvro) result.getPayload();
        assertThat(payload.getTemperatureC()).isEqualTo(23);
        assertThat(payload.getHumidity()).isEqualTo(55);
        assertThat(payload.getCo2Level()).isEqualTo(450);
    }

    @Test
    void shouldReturnCorrectType() {
        assertThat(mapper.getType())
                .isEqualTo(ru.yandex.practicum.collector.model.sensor.SensorEventType.CLIMATE_SENSOR_EVENT);
    }
}
