package ru.yandex.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.entity.ScenarioAction;
import ru.yandex.practicum.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.analyzer.entity.Sensor;
import ru.yandex.practicum.analyzer.repository.*;
import ru.yandex.practicum.kafka.telemetry.event.DeviceRemovedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceRemovedEventHandler implements HubEventHandler {

    private final SensorRepository sensorRepository;
    private final ScenarioConditionRepository scenarioConditionRepository;
    private final ScenarioActionRepository scenarioActionRepository;
    private final ScenarioRepository scenarioRepository;
    private final ConditionRepository conditionRepository;
    private final ActionRepository actionRepository;

    @Override
    public Class<DeviceRemovedEventAvro> getPayloadType() {
        return DeviceRemovedEventAvro.class;
    }

    @Override
    @Transactional
    public void handle(HubEventAvro event) {
        DeviceRemovedEventAvro payload = (DeviceRemovedEventAvro) event.getPayload();

        sensorRepository.findByIdAndHubId(payload.getId(), event.getHubId())
                .ifPresent(this::removeSensorWithRelatedData);
    }

    private void removeSensorWithRelatedData(Sensor sensor) {
        List<ScenarioCondition> conditions = scenarioConditionRepository.findAllBySensorId(sensor.getId());
        List<ScenarioAction> actions = scenarioActionRepository.findAllBySensorId(sensor.getId());

        // сценарии, которые могут остаться "осиротевшими" после удаления связей этого датчика
        List<Scenario> affectedScenarios = Stream.concat(
                        conditions.stream().map(ScenarioCondition::getScenario),
                        actions.stream().map(ScenarioAction::getScenario))
                .distinct()
                .toList();

        // удаляем сами условия/действия,
        // чтобы таблицы conditions/actions не накапливали мёртвые строки, на которые больше никто не ссылается
        conditionRepository.deleteAll(conditions.stream()
                .map(ScenarioCondition::getCondition).toList());
        actionRepository.deleteAll(actions.stream()
                .map(ScenarioAction::getAction).toList());

        // удаляем промежуточне связи, которые могут ссылаться на удаленны id
        scenarioConditionRepository.deleteAllBySensorId(sensor.getId());
        scenarioActionRepository.deleteAllBySensorId(sensor.getId());

        sensorRepository.delete(sensor);

        // сценарии, у которых после этого не осталось ни условий, ни действий - удаляем как "мусор"
        for (Scenario scenario : affectedScenarios) {
            // проверяем, остались ли у сценария какие-либо условия/действия с другими датчиками
            boolean stillHasConditions = !scenarioConditionRepository.findAllByScenarioId(scenario.getId()).isEmpty();
            boolean stillHasActions = !scenarioActionRepository.findAllByScenarioId(scenario.getId()).isEmpty();
            if (!stillHasConditions && !stillHasActions) {
                log.info("Сценарий [{}] хаба {} остался без условий/действий после удаления датчика {} — удаляю",
                        scenario.getName(), scenario.getHubId(), sensor.getId());
                scenarioRepository.delete(scenario);
            }
        }
    }
}