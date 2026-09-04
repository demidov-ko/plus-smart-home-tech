package ru.yandex.practicum.analyzer.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.analyzer.client.HubRouterClient;
import ru.yandex.practicum.analyzer.entity.Scenario;
import ru.yandex.practicum.analyzer.entity.ScenarioAction;
import ru.yandex.practicum.analyzer.mapper.ActionProtoMapper;
import ru.yandex.practicum.analyzer.repository.ScenarioRepository;
import ru.yandex.practicum.analyzer.service.ScenarioMatcher;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
// для каждого снапшота проверяет все сценарии соответствующего хаба: если условия
// сценария выполняются, то отправляет команды на устройства через Hub Router
public class SnapshotProcessor {

    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final ScenarioRepository scenarioRepository;
    private final ScenarioMatcher scenarioMatcher;
    private final ActionProtoMapper actionProtoMapper;
    private final HubRouterClient hubRouterClient;

    @Value("${kafka.topics.snapshots}")
    private String snapshotsTopic;

    public void start() {
        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            consumer.subscribe(List.of(snapshotsTopic));
            log.info("SnapshotProcessor начал чтение из топика: {}", snapshotsTopic);

            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    try {
                        handleSnapshot(record.value());
                    } catch (Exception e) {
                        log.error("Ошибка обработки снапшота (offset={}, hubId={}): {}",
                                record.offset(), record.key(), e.getMessage(), e);
                    }
                }
                // коммитим даже если часть записей упала
                consumer.commitSync();
            }

        } catch (WakeupException ignored) {
            // ожидаем при остановке
        } finally {
            try {
                consumer.commitSync();
            } finally {
                log.info("Закрываем консьюмер SnapshotProcessor");
                consumer.close();
            }
        }
    }

    private void handleSnapshot(SensorsSnapshotAvro snapshot) {
        List<Scenario> scenarios = scenarioRepository.findByHubId(snapshot.getHubId());
        log.info("Получен снапшот хаба {}, найдено сценариев: {}", snapshot.getHubId(), scenarios.size());

        for (Scenario scenario : scenarios) {
            // соответствие текущих показаний датчиков всем условиям сценария
            boolean matches = scenarioMatcher.matches(scenario, snapshot);
            log.info("Сценарий [{}] хаба {}: совпадение = {}", scenario.getName(), scenario.getHubId(), matches);
            if (matches) {
                executeScenario(scenario);
            }
        }
    }

    // выполняет все действия из сработавшего сценария
    private void executeScenario(Scenario scenario) {
        log.info("ВЫПОЛНЯЮ СЦЕНАРИЙ: hub={}, name={}, действий={}",
                scenario.getHubId(), scenario.getName(), scenario.getActions().size());

        for (ScenarioAction scenarioAction : scenario.getActions()) {
            // перевод сущности из БД в proto сообщение для gRPC
            DeviceActionProto action = actionProtoMapper.map(scenarioAction);

            log.debug("ОТПРАВЛЯЮ ДЕЙСТВИЕ: sensor={}, actionType={}, value={}",
                    action.getSensorId(), action.getType(), action.getValue());
            hubRouterClient.sendAction(scenario.getHubId(), scenario.getName(), action);
        }
    }
}