package ru.yandex.practicum.analyzer.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "scenario_conditions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ScenarioCondition {
    @EmbeddedId
    private ScenarioConditionId id;

    @ManyToOne
    @MapsId("scenarioId") // значение поля scenarioId внутри составного ключа id берем из связанной сущности scenario
    @JoinColumn(name = "scenario_id")
    private Scenario scenario;

    @ManyToOne
    @MapsId("sensorId")
    @JoinColumn(name = "sensor_id")
    private Sensor sensor;

    @ManyToOne
    @MapsId("conditionId")
    @JoinColumn(name = "condition_id")
    private Condition condition;
}
