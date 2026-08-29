//package ru.yandex.practicum.collector.mappers;
//
//import org.junit.jupiter.api.Test;
//import ru.yandex.practicum.collector.mapper.sensor.SwitchSensorEventMapper;
//import ru.yandex.practicum.collector.model.sensor.SwitchSensorEvent;
//import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
//import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
//
//import java.time.Instant;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class SwitchSensorEventMapperTest {
//
//    private final SwitchSensorEventMapper mapper = new SwitchSensorEventMapper();
//
//    @Test
//    void shouldMapTrueState() {
//        SwitchSensorEvent event = new SwitchSensorEvent();
//        event.setId("sensor.switch.4356");
//        event.setHubId("hub-1");
//        event.setTimestamp(Instant.parse("2026-08-17T20:10:25.150Z"));
//        event.setState(true);
//
//        SensorEventAvro result = mapper.map(event);
//
//        SwitchSensorAvro payload = (SwitchSensorAvro) result.getPayload();
//        assertThat(payload.getState()).isTrue();
//    }
//
//    @Test
//    void shouldMapFalseState() {
//        SwitchSensorEvent event = new SwitchSensorEvent();
//        event.setId("sensor.switch.4357");
//        event.setHubId("hub-1");
//        event.setTimestamp(Instant.now());
//        event.setState(false);
//
//        SensorEventAvro result = mapper.map(event);
//
//        SwitchSensorAvro payload = (SwitchSensorAvro) result.getPayload();
//        assertThat(payload.getState()).isFalse();
//    }
//}
