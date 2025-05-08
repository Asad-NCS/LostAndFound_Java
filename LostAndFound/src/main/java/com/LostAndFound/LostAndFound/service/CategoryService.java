package com.LostAndFound.LostAndFound.service;

import com.LostAndFound.LostAndFound.dto.CategoryDTO;
import com.LostAndFound.LostAndFound.model.Category;
import com.LostAndFound.LostAndFound.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public CategoryDTO createCategory(CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        Category saved = categoryRepository.save(category);
        return new CategoryDTO(saved.getId(), saved.getName()); // Return populated DTO
    }

    public CategoryDTO getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .map(c -> new CategoryDTO(c.getId(), c.getName()))
                .orElse(null);
    }

    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(c -> new CategoryDTO(c.getId(), c.getName()))
                .collect(Collectors.toList());
    }

    public CategoryDTO updateCategory(Long id, CategoryDTO dto) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found"));
        category.setName(dto.getName());
        Category updated = categoryRepository.save(category);
        return new CategoryDTO(updated.getId(), updated.getName());
    }

    public void deleteCategory(Long id) {
        categoryRepository.deleteById(id);
    }
}