package ru.yandex.practicum.aggregator.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorStateAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SnapshotService {
    // храним по одному снапшоту на каждый хаб по ключу hubId
    private final Map<String, SensorsSnapshotAvro> snapshots = new HashMap<>();

    public Optional<SensorsSnapshotAvro> updateState(SensorEventAvro event) {
        // Достаем снапшот для хаба, либо создаем новый
        SensorsSnapshotAvro snapshot = snapshots.computeIfAbsent(event.getHubId(),
                hubId -> SensorsSnapshotAvro.newBuilder()
                        .setHubId(hubId)
                        .setTimestamp(event.getTimestamp())
                        .setSensorsState(new HashMap<>())
                        .build());

        // Проверяем, есть ли уже данные по этому конкретному датчику
        Map<String, SensorStateAvro> sensorStateAvroMap = snapshot.getSensorsState();
        SensorStateAvro oldState = sensorStateAvroMap.get(event.getId());

        if (oldState != null) {
            // Если событие пришло раньше уже сохраненного состояния, то игнорируем
            if (oldState.getTimestamp().isAfter(event.getTimestamp())) {
                return Optional.empty();
            }
            // если данные не изменились, то тоже игнорируем
            if (oldState.getData().equals((event.getPayload()))) {
                return Optional.empty();
            }
        }
        // создаем новое состояние датчика на основе данных события
        SensorStateAvro newState = SensorStateAvro.newBuilder()
                .setTimestamp(event.getTimestamp())
                .setData(event.getPayload())
                .build();

        // кладем его в снапшот и обновляем таймстемп снапшота таймстемпом из события
        sensorStateAvroMap.put(event.getId(), newState);
        snapshot.setTimestamp(event.getTimestamp());

        return Optional.of(snapshot);
    }
}
