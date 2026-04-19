package com.example.notesystem.controller;

import com.example.notesystem.entity.Category;
import com.example.notesystem.repository.CategoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")

public class CategoryController {

    @Autowired
    private CategoryRepository categoryRepository;

    // 1. 获取当前用户的所有分类
    @GetMapping
    public ResponseEntity<?> getCategories(HttpServletRequest request) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        List<Category> categories = categoryRepository.findByUserIdOrderByCreatedAtAsc(userId);
        return ResponseEntity.ok(categories);
    }

    // 2. 新增分类
    @PostMapping
    public ResponseEntity<?> createCategory(HttpServletRequest request, @RequestBody Category categoryForm) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();

        if (categoryForm.getName() == null || categoryForm.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "分类名称不能为空"));
        }

        Category newCategory = new Category();
        newCategory.setUserId(userId);
        newCategory.setName(categoryForm.getName());
        newCategory.setParentId(categoryForm.getParentId() != null ? categoryForm.getParentId() : 0L);

        categoryRepository.save(newCategory);
        return ResponseEntity.ok(Map.of("message", "分类创建成功"));
    }

    // 3. 修改分类名称
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(HttpServletRequest request, @PathVariable Long id, @RequestBody Category categoryForm) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Optional<Category> opt = categoryRepository.findById(id);

        if (opt.isEmpty() || !opt.get().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "无权修改此分类"));
        }

        Category existing = opt.get();
        existing.setName(categoryForm.getName());
        categoryRepository.save(existing);

        return ResponseEntity.ok(Map.of("message", "分类修改成功"));
    }

    // 4. 删除分类
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Optional<Category> opt = categoryRepository.findById(id);

        if (opt.isEmpty() || !opt.get().getUserId().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "无权删除此分类"));
        }

        // 注意：由于数据库表设计了 ON DELETE SET NULL，删除分类后，该分类下的笔记不会被删，而是自动变成“无分类”状态
        categoryRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "分类删除成功"));
    }
}
