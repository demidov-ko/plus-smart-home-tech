package ru.yandex.practicum.order.feign.fallbackfactory;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.ProductClient;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            // Бизнес-ошибка (4xx) - не маскируем, пробрасываем оригинал (не уходят в fallback-сценарий)
            if (cause instanceof FeignException fe && fe.status() >= 400 && fe.status() < 500) {
                throw fe;
            }
            // Техническая деградация
            log.warn(
                    "product-service недоступен при запросе товара id={}",
                    productId,
                    cause
            );

            throw new ProductServiceUnavailableException(productId, cause);
        };
    }
}
