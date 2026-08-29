package ru.yandex.practicum.collector.controller;

import com.google.protobuf.Empty;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;
import ru.yandex.practicum.collector.service.HubEventService;
import ru.yandex.practicum.collector.service.SensorEventService;
import ru.yandex.practicum.grpc.telemetry.collector.CollectorControllerGrpc;
import ru.yandex.practicum.grpc.telemetry.event.HubEventProto;
import ru.yandex.practicum.grpc.telemetry.event.SensorEventProto;

@GrpcService
@RequiredArgsConstructor
// CollectorControllerImplBase генерируется из collector_controller.proto
// и содержит оба rpc метода CollectorSensorEvent/CollectorHubEvent по аналогии с CollectorController
// это обычный бин с @GrpcService
public class EventController extends CollectorControllerGrpc.CollectorControllerImplBase {
    private final SensorEventService sensorEventService;
    private final HubEventService hubEventService;

    @Override
    public void collectSensorEvent(SensorEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            sensorEventService.collect(request);

            // responseObserver - способ вернуть ответ клиенту в gRPC
            // onNext() кладет результат, onCompleted() завершает вызов
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            // в случае исключения отправляем ошибку клиенту
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }

    @Override
    public void collectHubEvent(HubEventProto request, StreamObserver<Empty> responseObserver) {
        try {
            hubEventService.collect(request);
            responseObserver.onNext(Empty.getDefaultInstance());
            responseObserver.onCompleted();
        } catch (Exception e) {
            responseObserver.onError(new StatusRuntimeException(Status.fromThrowable(e)));
        }
    }
}
