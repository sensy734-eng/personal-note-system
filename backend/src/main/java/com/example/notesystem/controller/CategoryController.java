package com.example.notesystem.controller;

import com.example.notesystem.dto.CategoryRequest;
import com.example.notesystem.entity.Category;
import com.example.notesystem.exception.ApiException;
import com.example.notesystem.repository.CategoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    @GetMapping
    public ResponseEntity<?> getCategories(HttpServletRequest request) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        List<Category> categories = categoryRepository.findByUserIdOrderByCreatedAtAsc(userId);
        return ResponseEntity.ok(categories);
    }

    @PostMapping
    public ResponseEntity<?> createCategory(HttpServletRequest request, @Valid @RequestBody CategoryRequest categoryForm) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();

        Category newCategory = new Category();
        newCategory.setUserId(userId);
        newCategory.setName(categoryForm.getName().trim());
        newCategory.setParentId(categoryForm.getParentId() != null ? categoryForm.getParentId() : 0L);

        categoryRepository.save(newCategory);
        return ResponseEntity.ok(Map.of("message", "分类创建成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody CategoryRequest categoryForm) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Category existing = categoryRepository.findById(id)
                .filter(category -> category.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "无权修改此分类"));

        existing.setName(categoryForm.getName().trim());
        categoryRepository.save(existing);

        return ResponseEntity.ok(Map.of("message", "分类修改成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Category existing = categoryRepository.findById(id)
                .filter(category -> category.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.FORBIDDEN, "无权删除此分类"));

        categoryRepository.deleteById(existing.getId());
        return ResponseEntity.ok(Map.of("message", "分类删除成功"));
    }
}
