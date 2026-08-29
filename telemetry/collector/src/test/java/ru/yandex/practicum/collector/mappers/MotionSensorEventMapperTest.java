//package ru.yandex.practicum.collector.mappers;
//
//import org.junit.jupiter.api.Test;
//import ru.yandex.practicum.collector.mapper.sensor.MotionSensorEventMapper;
//import ru.yandex.practicum.collector.model.sensor.MotionSensorEvent;
//import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
//import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
//
//import java.time.Instant;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class MotionSensorEventMapperTest {
//
//    private final MotionSensorEventMapper mapper = new MotionSensorEventMapper();
//
//    @Test
//    void shouldMapFieldsCorrectly() {
//        MotionSensorEvent event = new MotionSensorEvent();
//        event.setId("sensor.motion.5");
//        event.setHubId("hub-1");
//        Instant timestamp = Instant.parse("2026-08-17T20:10:25.150Z");
//        event.setTimestamp(timestamp);
//        event.setLinkQuality(90);
//        event.setMotion(true);
//        event.setVoltage(220);
//
//        SensorEventAvro result = mapper.map(event);
//
//        assertThat(result.getId()).isEqualTo("sensor.motion.5");
//        assertThat(result.getHubId()).isEqualTo("hub-1");
//        assertThat(result.getTimestamp()).isEqualTo(timestamp);
//
//        MotionSensorAvro payload = (MotionSensorAvro) result.getPayload();
//        assertThat(payload.getLinkQuality()).isEqualTo(90);
//        assertThat(payload.getMotion()).isTrue();
//        assertThat(payload.getVoltage()).isEqualTo(220);
//    }
//
//    @Test
//    void shouldMapFalseMotionCorrectly() {
//        MotionSensorEvent event = new MotionSensorEvent();
//        event.setId("sensor.motion.6");
//        event.setHubId("hub-1");
//        event.setTimestamp(Instant.now());
//        event.setLinkQuality(90);
//        event.setMotion(false);
//        event.setVoltage(220);
//
//        SensorEventAvro result = mapper.map(event);
//        MotionSensorAvro payload = (MotionSensorAvro) result.getPayload();
//
//        assertThat(payload.getMotion()).isFalse();
//    }
//}
