package ru.yandex.practicum.product.mapper;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;

@Component
public class ProductMapper {
    public ProductDto toDto(Product product) {
        if (product == null) {
            return null;
        }

        CategoryDto categoryDto = null;
        if (product.getCategory() != null) {
            categoryDto = new CategoryDto(
                    product.getCategory().getId(),
                    product.getCategory().getName(),
                    product.getCategory().getDescription()
            );
        }

        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                categoryDto,
                product.getImageUrl(),
                product.getActive()
        );
    }

    public Product toEntity(CreateProductRequest request, Category category) {
        if (request == null) {
            return null;
        }
        return Product.builder()
                .name(request.name())
                .description(request.description())
                .price(request.price())
                .category(category)
                .imageUrl(request.imageUrl())
                .active(true) // По умолчанию при создании товар активен
                .build();
    }
}
