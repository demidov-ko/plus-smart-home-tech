package ru.yandex.practicum.analyzer.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.entity.Sensor;
import ru.yandex.practicum.analyzer.repository.SensorRepository;
import ru.yandex.practicum.kafka.telemetry.event.DeviceAddedEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

@Component
@RequiredArgsConstructor
public class DeviceAddedEventHandler implements HubEventHandler {

    private final SensorRepository sensorRepository;

    @Override
    public Class<DeviceAddedEventAvro> getPayloadType() {
        return DeviceAddedEventAvro.class;
    }

    @Override
    public void handle(HubEventAvro event) {
        DeviceAddedEventAvro payload = (DeviceAddedEventAvro) event.getPayload();

        // если датчик уже зарегистрирован, то просто игнорируем повторное сообщение
        if (sensorRepository.existsByIdInAndHubId(java.util.List.of(payload.getId()), event.getHubId())) {
            return;
        }

        Sensor sensor = new Sensor();
        sensor.setId(payload.getId());
        sensor.setHubId(event.getHubId());
        sensorRepository.save(sensor);
    }
}
