package ru.yandex.practicum.analyzer.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.ScenarioAction;
import ru.yandex.practicum.grpc.telemetry.event.ActionTypeProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;

@Component
public class ActionProtoMapper {

    public DeviceActionProto map(ScenarioAction scenarioAction) {
        DeviceActionProto.Builder builder = DeviceActionProto.newBuilder()
                .setSensorId(scenarioAction.getSensor().getId())
                .setType(ActionTypeProto.valueOf(scenarioAction.getAction().getType().name()));

        Integer value = scenarioAction.getAction().getValue();
        if (value != null) {
            builder.setValue(value);
        }

        return builder.build();
    }
}