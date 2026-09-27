package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductMapper;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;

    @Override
    public List<ProductDto> getAllActive() {
        return productRepository.findByActiveTrue().stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Override
    public ProductDto getById(Long id) {
        return productMapper.toDto(getEntityById(id));
    }

    @Override
    public List<ProductDto> getByCategory(Long categoryId) {
        categoryService.getEntityById(categoryId);

        return productRepository.findByCategoryIdAndActiveTrue(categoryId).stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Override
    public Product getEntityById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Товар с id=" + id + " не найден"));
    }

    @Override
    public List<ProductDto> search(String query) {
        if (query == null || query.isBlank()) {
            return getAllActive();
        }
        return productRepository.findByActiveTrueAndNameContainingIgnoreCase(query).stream()
                .map(productMapper::toDto)
                .toList();
    }

    @Override
    public ProductDto create(CreateProductRequest request) {
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryService.getEntityById(request.categoryId());
        }

        Product product = productMapper.toEntity(request, category);
        return productMapper.toDto(productRepository.save(product));
    }

    @Override
    public ProductDto patchUpdate(Long id, UpdateProductRequest request) {
        Product product = getEntityById(id);

        if (request.categoryId() != null) {
            Category newCategory = categoryService.getEntityById(request.categoryId());
            product.setCategory(newCategory);
        }

        if (request.name() != null) {
            product.setName(request.name());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.price() != null) {
            product.setPrice(request.price());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }
        if (request.active() != null) {
            product.setActive(request.active());
        }

        return productMapper.toDto(productRepository.save(product));
    }
}
