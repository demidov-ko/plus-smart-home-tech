package ru.yandex.practicum.order.service;

import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemData;

import java.util.List;

public interface OrderService {
    OrderDto getById(Long id);

    List<OrderDto> getAll();

    List<OrderDto> getByEmail(String email);

    OrderDto saveOrder(String customerName, String customerEmail, List<OrderItemData> items);

}
