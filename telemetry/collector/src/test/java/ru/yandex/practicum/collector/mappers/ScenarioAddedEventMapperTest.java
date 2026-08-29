//package ru.yandex.practicum.collector.mappers;
//
//import org.junit.jupiter.api.Test;
//import ru.yandex.practicum.collector.mapper.hub.ScenarioAddedEventMapper;
//import ru.yandex.practicum.collector.model.hub.DeviceAction;
//import ru.yandex.practicum.collector.model.hub.ScenarioAddedEvent;
//import ru.yandex.practicum.collector.model.hub.ScenarioCondition;
//import ru.yandex.practicum.collector.model.hub.types.ActionType;
//import ru.yandex.practicum.collector.model.hub.types.ConditionOperation;
//import ru.yandex.practicum.collector.model.hub.types.ConditionType;
//import ru.yandex.practicum.kafka.telemetry.event.ActionTypeAvro;
//import ru.yandex.practicum.kafka.telemetry.event.ConditionOperationAvro;
//import ru.yandex.practicum.kafka.telemetry.event.ConditionTypeAvro;
//import ru.yandex.practicum.kafka.telemetry.event.DeviceActionAvro;
//import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
//import ru.yandex.practicum.kafka.telemetry.event.ScenarioAddedEventAvro;
//import ru.yandex.practicum.kafka.telemetry.event.ScenarioConditionAvro;
//
//import java.time.Instant;
//import java.util.List;
//
//import static org.assertj.core.api.Assertions.assertThat;
//
//class ScenarioAddedEventMapperTest {
//
//    private final ScenarioAddedEventMapper mapper = new ScenarioAddedEventMapper();
//
//    @Test
//    void shouldMapConditionsWithIntAndBooleanValues() {
//        // given
//        ScenarioCondition tempCondition = new ScenarioCondition();
//        tempCondition.setSensorId("sensor.temp.1");
//        tempCondition.setType(ConditionType.TEMPERATURE);
//        tempCondition.setOperation(ConditionOperation.GREATER_THAN);
//        tempCondition.setValue(25); // Integer
//
//        ScenarioCondition switchCondition = new ScenarioCondition();
//        switchCondition.setSensorId("sensor.switch.1");
//        switchCondition.setType(ConditionType.SWITCH);
//        switchCondition.setOperation(ConditionOperation.EQUALS);
//        switchCondition.setValue(true); // Boolean
//
//        DeviceAction action = new DeviceAction();
//        action.setSensorId("sensor.switch.2");
//        action.setType(ActionType.ACTIVATE);
//        action.setValue(null); // null
//
//        ScenarioAddedEvent event = new ScenarioAddedEvent();
//        event.setHubId("hub-1");
//        event.setTimestamp(Instant.parse("2026-08-17T20:10:25.150Z"));
//        event.setName("Обогрев спальни");
//        event.setConditions(List.of(tempCondition, switchCondition));
//        event.setActions(List.of(action));
//
//        // when
//        HubEventAvro result = mapper.map(event);
//
//        // then
//        ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) result.getPayload();
//        assertThat(payload.getName()).isEqualTo("Обогрев спальни");
//        assertThat(payload.getConditions()).hasSize(2);
//        assertThat(payload.getActions()).hasSize(1);
//
//        ScenarioConditionAvro mappedTempCondition = payload.getConditions().get(0);
//        assertThat(mappedTempCondition.getSensorId()).isEqualTo("sensor.temp.1");
//        assertThat(mappedTempCondition.getType()).isEqualTo(ConditionTypeAvro.TEMPERATURE);
//        assertThat(mappedTempCondition.getOperation()).isEqualTo(ConditionOperationAvro.GREATER_THAN);
//        assertThat(mappedTempCondition.getValue()).isEqualTo(25); // именно Integer
//
//        ScenarioConditionAvro mappedSwitchCondition = payload.getConditions().get(1);
//        assertThat(mappedSwitchCondition.getValue()).isEqualTo(true); // именно Boolean
//
//        DeviceActionAvro mappedAction = payload.getActions().get(0);
//        assertThat(mappedAction.getSensorId()).isEqualTo("sensor.switch.2");
//        assertThat(mappedAction.getType()).isEqualTo(ActionTypeAvro.ACTIVATE);
//        assertThat(mappedAction.getValue()).isNull(); // не должно падать на null
//    }
//
//    @Test
//    void shouldMapActionWithIntValue() {
//        DeviceAction action = new DeviceAction();
//        action.setSensorId("sensor.dimmer.1");
//        action.setType(ActionType.SET_VALUE);
//        action.setValue(80);
//        ScenarioCondition condition = new ScenarioCondition();
//        condition.setSensorId("sensor.motion.1");
//        condition.setType(ConditionType.MOTION);
//        condition.setOperation(ConditionOperation.EQUALS);
//        condition.setValue(true);
//
//        ScenarioAddedEvent event = new ScenarioAddedEvent();
//        event.setHubId("hub-1");
//        event.setTimestamp(Instant.now());
//        event.setName("Свет по движению");
//        event.setConditions(List.of(condition));
//        event.setActions(List.of(action));
//
//        HubEventAvro result = mapper.map(event);
//        ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) result.getPayload();
//
//        assertThat(payload.getActions().get(0).getValue()).isEqualTo(80);
//    }
//}
