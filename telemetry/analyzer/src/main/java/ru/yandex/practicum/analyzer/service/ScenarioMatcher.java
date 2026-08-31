package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioMatcher {

    private final ScenarioConditionEvaluator conditionEvaluator;

    public boolean matches(Scenario scenario, SensorsSnapshotAvro snapshot) {
        // сценарий срабатывает только если ВСЕ его условия выполнены одновременно
        return scenario.getConditions().stream()
                .allMatch(condition -> conditionEvaluator.isSatisfied(condition, snapshot));
    }
}