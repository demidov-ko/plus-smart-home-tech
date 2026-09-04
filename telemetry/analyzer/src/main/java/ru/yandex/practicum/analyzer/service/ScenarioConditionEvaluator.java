package ru.yandex.practicum.analyzer.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.Condition;
import ru.yandex.practicum.analyzer.entity.ConditionOperation;
import ru.yandex.practicum.analyzer.entity.ConditionType;
import ru.yandex.practicum.analyzer.entity.ScenarioCondition;
import ru.yandex.practicum.kafka.telemetry.event.ClimateSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.LightSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.MotionSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.kafka.telemetry.event.SwitchSensorAvro;
import ru.yandex.practicum.kafka.telemetry.event.TemperatureSensorAvro;

@Slf4j
@Component
// оценщик условий сценариев
public class ScenarioConditionEvaluator {

    // проверяет, выполнено ли условие сценария для текущего снапшота данных
    public boolean isSatisfied(ScenarioCondition scenarioCondition, SensorsSnapshotAvro snapshot) {
        // получаем состояние конкретного датчика по его id из общего списка в снапшоте
        SensorStateAvro state = snapshot.getSensorsState().get(scenarioCondition.getSensor().getId());
        if (state == null) {
            log.debug("Нет данных по датчику {} в снапшоте", scenarioCondition.getSensor().getId());
            // если по датчику ещё не было данных в снапшоте, то условие не может быть выполнено
            return false;
        }

        Condition condition = scenarioCondition.getCondition();
        // получаем актуальное значение из данных датчика
        Integer actualValue = extractValue(condition.getType(), state.getData());
        if (actualValue == null) {
            // если тип условия не соответствует типу данных датчика, считаем условие невыполненным
            return false;
        }

        // сравниваем актуальное значение и ожидаемое
        return applyOperation(condition.getOperation(), actualValue, condition.getValue());
    }

    // маппит тип условия на конкретное поле в соответствующем Avro-классе
    private Integer extractValue(ConditionType type, Object payload) {
        return switch (type) {
            case MOTION -> payload instanceof MotionSensorAvro m ? boolToInt(m.getMotion()) : null;
            case LUMINOSITY -> payload instanceof LightSensorAvro l ? l.getLuminosity() : null;
            case SWITCH -> payload instanceof SwitchSensorAvro s ? boolToInt(s.getState()) : null;
            case TEMPERATURE -> extractTemperature(payload);
            case CO2LEVEL -> payload instanceof ClimateSensorAvro c ? c.getCo2Level() : null;
            case HUMIDITY -> payload instanceof ClimateSensorAvro c ? c.getHumidity() : null;
        };
    }

    private Integer extractTemperature(Object payload) {
        // температура может прийти и с климатического, и с обычного температурного датчика
        if (payload instanceof ClimateSensorAvro climate) {
            return climate.getTemperatureC();
        }
        if (payload instanceof TemperatureSensorAvro temperature) {
            return temperature.getTemperatureC();
        }
        return null;
    }

    // операция сравнения двух целых чисел
    private boolean applyOperation(ConditionOperation operation, int actual, Integer expected) {
        if (expected == null) {
            return false;
        }
        return switch (operation) {
            case EQUALS -> actual == expected;
            case GREATER_THAN -> actual > expected;
            case LOWER_THAN -> actual < expected;
        };
    }

    // конвертер булевых значений
    private int boolToInt(boolean value) {
        return value ? 1 : 0;
    }
}
