package ru.yandex.practicum.collector.model.sensor;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

// Событие климатического датчика
@Getter
@Setter
@ToString(callSuper = true)
public class ClimateSensorEvent extends SensorEvent {
    @NotNull
    private int temperatureC;

    @NotNull
    private int humidity;       // Влажность

    @NotNull
    private int co2Level;

    @Override
    public SensorEventType getType() {
        return SensorEventType.CLIMATE_SENSOR_EVENT;
    }
}
