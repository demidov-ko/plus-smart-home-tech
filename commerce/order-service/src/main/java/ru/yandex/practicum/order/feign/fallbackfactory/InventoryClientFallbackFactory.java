package ru.yandex.practicum.order.feign.fallbackfactory;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.dto.ReleaseRequest;
import ru.yandex.practicum.order.feign.dto.ReserveRequest;
import ru.yandex.practicum.order.feign.dto.ReserveResponse;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {

            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                // Бизнес-ошибка (4xx) - пробрасываем оригинал (не уходят в fallback-сценарий)
                if (cause instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                    throw fe;
                }
                log.warn(
                        "inventory-service недоступен при резервировании товара id={}",
                        request.productId(),
                        cause
                );
                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }

            @Override
            public ReserveResponse releaseStock(ReleaseRequest request) {
                // Компенсация - best-effort: логируем, не бросаем дальше
                log.warn(
                        "inventory-service недоступен при снятии резерва товара id={}. Причина: {}",
                        request.productId(),
                        cause.getMessage()
                );
                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }
        };
    }
}
