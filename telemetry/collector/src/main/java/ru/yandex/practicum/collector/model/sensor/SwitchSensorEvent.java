package ru.yandex.practicum.collector.model.sensor;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true)
public class SwitchSensorEvent extends SensorEvent {
    private boolean state;  // Текущее состояние переключателя. true - включен, false - выключен

    @Override
    public SensorEventType getType() {
        return SensorEventType.SWITCH_SENSOR_EVENT;
    }
}
