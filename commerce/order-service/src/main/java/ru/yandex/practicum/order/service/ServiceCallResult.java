package ru.yandex.practicum.order.service;

// Sealed interface - интерфейс с закрытым набором реализаций
public sealed interface ServiceCallResult<T>
        permits ServiceCallResult.Success,
        ServiceCallResult.Failure,
        ServiceCallResult.Degraded {

    // сервис ответил успешно, в value лежит результат
    record Success<T>(T value) implements ServiceCallResult<T> {}

    // бизнес-отказ (товар не найден, неактивен, остатка недостаточно). Заказ не создаётся, сценарий нужно остановить
    record Failure<T>(String message) implements ServiceCallResult<T> {}

    // техническая недоступность, можно перейти к резервному сценарию. Заказ сохраняется как PENDING_CONFIRMATION
    record Degraded<T>(String reason) implements ServiceCallResult<T> {}
}
