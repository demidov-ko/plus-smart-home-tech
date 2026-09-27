package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getAll();

    CategoryDto getById(Long id);

    Category getEntityById(Long id);

    CategoryDto create(CreateCategoryRequest request);
}
