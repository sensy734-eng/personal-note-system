package com.example.notesystem.repository;

import com.example.notesystem.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    // 根据用户 ID 查询该用户的所有分类，并按创建时间排序
    List<Category> findByUserIdOrderByCreatedAtAsc(Long userId);
}