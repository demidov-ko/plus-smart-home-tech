package ru.yandex.practicum.order.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.order.entity.Order;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @EntityGraph(attributePaths = "items")
    List<Order> findByCustomerEmail(String email);

    @EntityGraph(attributePaths = "items")  // решает проблему N+1
    List<Order> findAll();
}
