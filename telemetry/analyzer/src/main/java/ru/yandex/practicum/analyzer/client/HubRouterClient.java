package ru.yandex.practicum.analyzer.client;

import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionProto;
import ru.yandex.practicum.grpc.telemetry.event.DeviceActionRequest;
import ru.yandex.practicum.grpc.telemetry.hubrouter.HubRouterControllerGrpc;

import java.time.Instant;

@Slf4j
@Component
public class HubRouterClient {

    @GrpcClient("hub-router")
    // Blocking-stub — синхронный gRPC-клиент
    private HubRouterControllerGrpc.HubRouterControllerBlockingStub hubRouterStub;

    // отправка команды на выполнение действия устройству через Hub Router
    public void sendAction(String hubId, String scenarioName, DeviceActionProto action) {
        Instant now = Instant.now();

        // gRPC-запрос
        DeviceActionRequest request = DeviceActionRequest.newBuilder()
                .setHubId(hubId)
                .setScenarioName(scenarioName)
                .setAction(action)
                .setTimestamp(Timestamp.newBuilder()
                        .setSeconds(now.getEpochSecond())
                        .setNanos(now.getNano())
                        .build())
                .build();

        try {
            // синхронный вызов: блокирует поток до получения ответа от Hub Router
            Empty response = hubRouterStub.handleDeviceAction(request);
            log.debug("Отправлено действие {} для хаба {}, ответ: {}", action.getType(), hubId, response);
        } catch (Exception e) {
            log.error("Не удалось отправить действие {} для хаба {}: {}", action.getType(), hubId, e.getMessage(), e);
        }
    }
}