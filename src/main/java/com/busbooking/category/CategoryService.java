package com.busbooking.category;

import com.busbooking.bus.BusRepository;
import com.busbooking.category.dto.CategoryResponse;
import com.busbooking.category.dto.CreateCategoryRequest;
import com.busbooking.common.exception.BusCategoryNotFoundException;
import com.busbooking.common.exception.DuplicateResourceException;
import com.busbooking.common.exception.ResourceConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final BusRepository busRepository;

    public CategoryService(CategoryRepository categoryRepository, BusRepository busRepository) {
        this.categoryRepository = categoryRepository;
        this.busRepository = busRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllActiveCategories() {
        return categoryRepository.findByIsActiveTrue()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        BusCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusCategoryNotFoundException(id));
        return CategoryResponse.fromEntity(category);
    }

    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String code = request.code().toUpperCase().trim();
        if (categoryRepository.existsByCode(code)) {
            throw new DuplicateResourceException("Category with code " + code + " already exists");
        }

        BusCategory category = new BusCategory(
                code,
                request.name().trim(),
                request.description() != null ? request.description().trim() : null
        );

        BusCategory saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        BusCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusCategoryNotFoundException(id));

        String newCode = request.code().toUpperCase().trim();
        if (!category.getCode().equals(newCode) && categoryRepository.existsByCode(newCode)) {
            throw new DuplicateResourceException("Category with code " + newCode + " already exists");
        }

        category.setCode(newCode);
        category.setName(request.name().trim());
        category.setDescription(request.description() != null ? request.description().trim() : null);

        BusCategory updated = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updated);
    }

    @Transactional(noRollbackFor = ResourceConflictException.class)
    public void deleteCategory(Long id) {
        BusCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusCategoryNotFoundException(id));

        if (busRepository.existsByCategoryId(id)) {
            category.setActive(false);
            categoryRepository.save(category);
            throw new ResourceConflictException(
                    "Category with id " + id + " cannot be removed because it is referenced by existing buses. It has been deactivated instead."
            );
        }

        categoryRepository.delete(category);
    }

    @Transactional
    public CategoryResponse deactivateCategory(Long id) {
        BusCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusCategoryNotFoundException(id));
        category.setActive(false);
        BusCategory saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }
}
