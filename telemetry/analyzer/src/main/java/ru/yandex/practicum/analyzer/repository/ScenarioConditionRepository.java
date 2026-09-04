package ru.yandex.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.analyzer.entity.ScenarioConditionId;

import java.util.List;

public interface ScenarioConditionRepository extends JpaRepository<ScenarioCondition, ScenarioConditionId> {

    @Modifying
    @Transactional
    @Query("DELETE FROM ScenarioCondition sc WHERE sc.sensor.id = :sensorId")
    void deleteAllBySensorId(String sensorId);

    List<ScenarioCondition> findAllBySensorId(String sensorId);

    List<ScenarioCondition> findAllByScenarioId(Long scenarioId);
}
