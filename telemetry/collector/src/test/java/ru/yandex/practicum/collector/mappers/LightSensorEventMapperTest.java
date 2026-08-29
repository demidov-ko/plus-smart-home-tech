//package ru.yandex.practicum.collector.mappers;
//
//import org.junit.jupiter.api.Test;
//import ru.yandex.practicum.collector.mapper.sensor.LightSensorEventMapper;
//import ru.yandex.practicum.collector.model.sensor.LightSensorEvent;
//import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
//import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
//
//import java.time.Instant;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class LightSensorEventMapperTest {
//
//    private final LightSensorEventMapper mapper = new LightSensorEventMapper();
//
//    @Test
//    void shouldMapFieldsCorrectly() {
//        LightSensorEvent event = new LightSensorEvent();
//        event.setId("sensor.light.3");
//        event.setHubId("hub-2");
//        Instant timestamp = Instant.parse("2026-08-17T20:10:25.150Z");
//        event.setTimestamp(timestamp);
//        event.setLinkQuality(75);
//        event.setLuminosity(59);
//
//        SensorEventAvro result = mapper.map(event);
//
//        assertThat(result.getId()).isEqualTo("sensor.light.3");
//        assertThat(result.getHubId()).isEqualTo("hub-2");
//        assertThat(result.getTimestamp()).isEqualTo(timestamp);
//
//        LightSensorAvro payload = (LightSensorAvro) result.getPayload();
//        assertThat(payload.getLinkQuality()).isEqualTo(75);
//        assertThat(payload.getLuminosity()).isEqualTo(59);
//    }
//}
