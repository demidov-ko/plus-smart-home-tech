package ru.yandex.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.repository.ScenarioActionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioConditionRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
@RequiredArgsConstructor
public class DeviceRemovedEventHandler implements HubEventHandler {

    private final SensorRepository sensorRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;

    @Override
    public Class<DeviceRemovedEventAvro> getPayloadType() {
        return DeviceRemovedEventAvro.class;
    }

    @Override
    public void handle(HubEventAvro event) {
        DeviceRemovedEventAvro payload = (DeviceRemovedEventAvro) event.getPayload();

        sensorRepository.findByIdAndHubId(payload.getId(), event.getHubId())
                .ifPresent(sensor -> {
                    scenarioConditionRepository.deleteAllBySensorId(sensor.getId());
                    scenarioActionRepository.deleteAllBySensorId(sensor.getId());
                    sensorRepository.delete(sensor);
                });
    }
}