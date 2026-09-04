package ru.yandex.practicum.collector.mapper.hub;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioAddedEventProto;
import ru.yandex.practicum.grpc.telemetry.event.ScenarioConditionProto;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;

@Component
public class ScenarioAddedEventMapper implements HubEventMapper {

    @Override
    public HubEventProto.PayloadCase getPayloadCase() {
        return HubEventProto.PayloadCase.SCENARIO_ADDED;
    }

    @Override
    public HubEventAvro map(HubEventProto event) {
        ScenarioAddedEventProto source = event.getScenarioAdded();

        // для repeated полей Protobuf генерирует геттер с суффиксом List по имени поля condition из .proto
        List<ScenarioConditionAvro> conditions = source.getConditionList().stream()
                .map(this::mapCondition)
                .toList();

        List<DeviceActionAvro> actions = source.getActionList().stream()
                .map(this::mapAction)
                .toList();

        ScenarioAddedEventAvro payload = ScenarioAddedEventAvro.newBuilder()
                .setName(source.getName())
                .setConditions(conditions)
                .setActions(actions)
                .build();

        return HubEventAvro.newBuilder()
                .setHubId(event.getHubId())
                .setTimestamp(toInstant(event.getTimestamp()))
                .setPayload(payload)
                .build();
    }

    private ScenarioConditionAvro mapCondition(ScenarioConditionProto condition) {
        // ScenarioConditionProto тоже oneof (bool_value/int_value), поэтому у него свой ValueCase-enum.
        // switch по нему достает нужное значение и приводит к Object lkz Avro union
        Object value = switch (condition.getValueCase()) {
            case BOOL_VALUE -> condition.getBoolValue();
            case INT_VALUE -> condition.getIntValue();
            case VALUE_NOT_SET -> null;
        };

        return ScenarioConditionAvro.newBuilder()
                .setSensorId(condition.getSensorId())
                .setType(ConditionTypeAvro.valueOf(condition.getType().name()))
                .setOperation(ConditionOperationAvro.valueOf(condition.getOperation().name()))
                .setValue(value)
                .build();
    }

    private DeviceActionAvro mapAction(DeviceActionProto action) {
        // value в DeviceActionProto объявлено optional int32, а не oneof,
        // поэтому проверяем через генерируемый метод hasValue(), который проверяет, было ли поле вообще установлено
        Integer value = action.hasValue() ? action.getValue() : null;

        return DeviceActionAvro.newBuilder()
                .setSensorId(action.getSensorId())
                .setType(ActionTypeAvro.valueOf(action.getType().name()))
                .setValue(value)
                .build();
    }
}
