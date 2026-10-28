package com.busbooking.category;

import com.busbooking.bus.BusRepository;
import com.busbooking.category.dto.CategoryResponse;
import com.busbooking.category.dto.CreateCategoryRequest;
import com.busbooking.common.exception.BusCategoryNotFoundException;
import com.busbooking.common.exception.DuplicateResourceException;
import com.busbooking.common.exception.ResourceConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BusRepository busRepository;

    @InjectMocks
    private CategoryService categoryService;

    private BusCategory sampleCategory;

    @BeforeEach
    void setUp() {
        sampleCategory = new BusCategory("AC_DELUXE", "AC Deluxe Coach", "Comfortable air-conditioned coach");
        sampleCategory.setId(1L);
    }

    @Test
    void testGetAllActiveCategories() {
        when(categoryRepository.findByIsActiveTrue()).thenReturn(List.of(sampleCategory));

        List<CategoryResponse> results = categoryService.getAllActiveCategories();

        assertEquals(1, results.size());
        assertEquals("AC_DELUXE", results.get(0).code());
        assertTrue(results.get(0).isActive());
        verify(categoryRepository).findByIsActiveTrue();
    }

    @Test
    void testGetCategoryByIdSuccess() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));

        CategoryResponse result = categoryService.getCategoryById(1L);

        assertNotNull(result);
        assertEquals("AC_DELUXE", result.code());
        assertEquals("AC Deluxe Coach", result.name());
    }

    @Test
    void testGetCategoryByIdThrowsNotFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(BusCategoryNotFoundException.class, () -> categoryService.getCategoryById(99L));
    }

    @Test
    void testCreateCategorySuccess() {
        CreateCategoryRequest request = new CreateCategoryRequest("SLEEPER_AC", "AC Sleeper", "Berths with AC");
        when(categoryRepository.existsByCode("SLEEPER_AC")).thenReturn(false);
        when(categoryRepository.save(any(BusCategory.class))).thenAnswer(invocation -> {
            BusCategory c = invocation.getArgument(0);
            c.setId(2L);
            return c;
        });

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response);
        assertEquals("SLEEPER_AC", response.code());
        assertEquals("AC Sleeper", response.name());
        verify(categoryRepository).save(any(BusCategory.class));
    }

    @Test
    void testCreateCategoryDuplicateCodeThrowsException() {
        CreateCategoryRequest request = new CreateCategoryRequest("AC_DELUXE", "Duplicate Name", "Desc");
        when(categoryRepository.existsByCode("AC_DELUXE")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> categoryService.createCategory(request));
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void testDeleteUnreferencedCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(busRepository.existsByCategoryId(1L)).thenReturn(false);

        categoryService.deleteCategory(1L);

        verify(categoryRepository).delete(sampleCategory);
    }

    @Test
    void testDeleteReferencedCategoryDeactivatesAndThrowsConflict() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(busRepository.existsByCategoryId(1L)).thenReturn(true);

        ResourceConflictException ex = assertThrows(ResourceConflictException.class,
                () -> categoryService.deleteCategory(1L));

        assertTrue(ex.getMessage().contains("referenced by existing buses"));
        assertFalse(sampleCategory.isActive(), "Referenced category must be marked inactive");
        verify(categoryRepository).save(sampleCategory);
        verify(categoryRepository, never()).delete(sampleCategory);
    }

    @Test
    void testDeactivateCategory() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(sampleCategory));
        when(categoryRepository.save(any(BusCategory.class))).thenReturn(sampleCategory);

        CategoryResponse response = categoryService.deactivateCategory(1L);

        assertFalse(response.isActive());
        verify(categoryRepository).save(sampleCategory);
    }
}
