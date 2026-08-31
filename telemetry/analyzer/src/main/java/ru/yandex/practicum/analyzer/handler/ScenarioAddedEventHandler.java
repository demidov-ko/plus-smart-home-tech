package ru.yandex.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.*;
import ru.yandex.practicum.analyzer.repository.ActionRepository;
import ru.yandex.practicum.analyzer.repository.ConditionRepository;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.*;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ScenarioAddedEventHandler implements HubEventHandler {

    private final ScenarioRepository scenarioRepository;
    private final SensorRepository sensorRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Override
    public Class<ScenarioAddedEventAvro> getPayloadType() {
        return ScenarioAddedEventAvro.class;
    }

    @Override
    public void handle(HubEventAvro event) {
        ScenarioAddedEventAvro payload = (ScenarioAddedEventAvro) event.getPayload();
        String hubId = event.getHubId();

        // если сценарий с таким именем уже был, удаляем старую версию
        // (каскад снесёт связанные ScenarioCondition/ScenarioAction)
        scenarioRepository.findByHubIdAndName(hubId, payload.getName())
                .ifPresent(scenarioRepository::delete);

        Scenario scenario = new Scenario();
        scenario.setHubId(hubId);
        scenario.setName(payload.getName());
        scenario = scenarioRepository.save(scenario);

        for (ScenarioConditionAvro conditionAvro : payload.getConditions()) {
            Sensor sensor = sensorRepository.findByIdAndHubId(conditionAvro.getSensorId(), hubId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Датчик " + conditionAvro.getSensorId() + " не зарегистрирован в хабе " + hubId));

            Condition condition = new Condition();
            condition.setType(ConditionType.valueOf(conditionAvro.getType().name()));
            condition.setOperation(ConditionOperation.valueOf(conditionAvro.getOperation().name()));
            condition.setValue(toIntegerValue(conditionAvro.getValue()));
            condition = conditionRepository.save(condition);

            ScenarioCondition scenarioCondition = new ScenarioCondition();
            scenarioCondition.setId(new ScenarioConditionId(scenario.getId(), sensor.getId(), condition.getId()));
            scenarioCondition.setScenario(scenario);
            scenarioCondition.setSensor(sensor);
            scenarioCondition.setCondition(condition);
            scenario.getConditions().add(scenarioCondition);
        }

        for (DeviceActionAvro actionAvro : payload.getActions()) {
            Sensor sensor = sensorRepository.findByIdAndHubId(actionAvro.getSensorId(), hubId)
                    .orElseThrow(() -> new IllegalStateException(
                            "Датчик " + actionAvro.getSensorId() + " не зарегистрирован в хабе " + hubId));

            Action action = new Action();
            action.setType(ActionType.valueOf(actionAvro.getType().name()));
            action.setValue(actionAvro.getValue());
            action = actionRepository.save(action);

            ScenarioAction scenarioAction = new ScenarioAction();
            scenarioAction.setId(new ScenarioActionId(scenario.getId(), sensor.getId(), action.getId()));
            scenarioAction.setScenario(scenario);
            scenarioAction.setSensor(sensor);
            scenarioAction.setAction(action);
            scenario.getActions().add(scenarioAction);
        }

        scenarioRepository.save(scenario);
    }

    // value в ScenarioConditionAvro - union {null, int, boolean}, приводим к Integer для колонки БД
    private Integer toIntegerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Boolean bool) {
            return bool ? 1 : 0;
        }
        return (Integer) value;
    }
}