//package ru.yandex.practicum.collector.mappers;
//
//import org.junit.jupiter.api.Test;
//import ru.yandex.practicum.collector.mapper.hub.ScenarioRemovedEventMapper;
//import ru.yandex.practicum.collector.model.hub.ScenarioRemovedEvent;
//import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
//import ru.yandex.practicum.kafka.telemetry.event.ScenarioRemovedEventAvro;
//
//import java.time.Instant;
//import java.time.temporal.ChronoUnit;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class ScenarioRemovedEventMapperTest {
//
//    private final ScenarioRemovedEventMapper mapper = new ScenarioRemovedEventMapper();
//
//    @Test
//    void shouldMapFieldsCorrectly() {
//        ScenarioRemovedEvent event = new ScenarioRemovedEvent();
//        event.setHubId("hub-1");
//        Instant timestamp = Instant.now().truncatedTo(ChronoUnit.MILLIS);
//        event.setTimestamp(timestamp);
//        event.setName("Обогрев спальни");
//
//        HubEventAvro result = mapper.map(event);
//
//        assertThat(result.getHubId()).isEqualTo("hub-1");
//        assertThat(result.getTimestamp()).isEqualTo(timestamp);
//
//        ScenarioRemovedEventAvro payload = (ScenarioRemovedEventAvro) result.getPayload();
//        assertThat(payload.getName()).isEqualTo("Обогрев спальни");
//    }
//}
