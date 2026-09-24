package ru.yandex.practicum.product.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.product.entity.Product;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {
    @EntityGraph(attributePaths = "category")
    List<Product> findByActiveTrue();

    @EntityGraph(attributePaths = "category")
    List<Product> findByCategoryIdAndActiveTrue(Long categoryId);

    List<Product> findByActiveTrueAndNameContainingIgnoreCase(String query);
}
