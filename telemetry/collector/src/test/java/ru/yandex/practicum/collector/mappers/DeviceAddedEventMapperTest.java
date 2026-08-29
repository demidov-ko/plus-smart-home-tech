//package ru.yandex.practicum.collector.mappers;
//
//import org.junit.jupiter.api.Test;
//import ru.yandex.practicum.collector.mapper.hub.DeviceAddedEventMapper;
//import ru.yandex.practicum.collector.model.hub.DeviceAddedEvent;
//import ru.yandex.practicum.collector.model.hub.types.DeviceType;
//import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
//import ru.yandex.practicum.kafka.telemetry.event.DeviceTypeAvro;
//import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
//
//import java.time.Instant;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class DeviceAddedEventMapperTest {
//
//    private final DeviceAddedEventMapper mapper = new DeviceAddedEventMapper();
//
//    @Test
//    void shouldMapFieldsCorrectly() {
//        // given
//        DeviceAddedEvent event = new DeviceAddedEvent();
//        event.setHubId("hub-1");
//        Instant timestamp = Instant.parse("2026-08-17T20:10:25.150Z");
//        event.setTimestamp(timestamp);
//        event.setId("sensor.motion.1");
//        event.setDeviceType(DeviceType.MOTION_SENSOR);
//
//        // when
//        HubEventAvro result = mapper.map(event);
//
//        // then
//        assertThat(result.getHubId()).isEqualTo("hub-1");
//        assertThat(result.getTimestamp()).isEqualTo(timestamp);
//
//        assertThat(result.getPayload()).isInstanceOf(DeviceAddedEventAvro.class);
//        DeviceAddedEventAvro payload = (DeviceAddedEventAvro) result.getPayload();
//        assertThat(payload.getId()).isEqualTo("sensor.motion.1");
//        assertThat(payload.getType()).isEqualTo(DeviceTypeAvro.MOTION_SENSOR);
//    }
//}
