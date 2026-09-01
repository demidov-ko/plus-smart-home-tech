package ru.yandex.practicum.analyzer.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class ScenarioMatcher {

    private final ScenarioConditionEvaluator conditionEvaluator;

    public boolean matches(Scenario scenario, SensorsSnapshotAvro snapshot) {
        Set<ScenarioCondition> conditions = scenario.getConditions();
        if (conditions.isEmpty()) {
            // пустой сценарий не сработает
            return false;
        }
        // сценарий срабатывает только если ВСЕ его условия выполнены одновременно
        return conditions.stream()
                .allMatch(condition -> conditionEvaluator.isSatisfied(condition, snapshot));
    }
}