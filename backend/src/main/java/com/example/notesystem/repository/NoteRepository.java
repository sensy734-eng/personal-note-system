package com.example.notesystem.repository;

import com.example.notesystem.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    // 🚀 核心加强：支持多维度筛选（含标签检索）
    // 使用 Native Query 处理多表关联，支持按标签名、关键词、分类、星标、日期多重组合筛选
    @Query(value = "SELECT DISTINCT n.* FROM notes n " +
            "LEFT JOIN note_tags nt ON n.id = nt.note_id " +
            "LEFT JOIN tags t ON nt.tag_id = t.id " +
            "WHERE n.user_id = :userId AND n.status = :status " +
            "AND (:keyword IS NULL OR n.title LIKE %:keyword% OR n.content_text LIKE %:keyword%) " +
            "AND (:categoryId IS NULL OR n.category_id = :categoryId) " +
            "AND (:isStarred IS NULL OR n.is_starred = :isStarred) " +
            "AND (:startDate IS NULL OR n.updated_at >= :startDate) " +
            "AND (:endDate IS NULL OR n.updated_at <= :endDate) " +
            "AND (:tagName IS NULL OR t.name = :tagName) " +
            "ORDER BY n.is_starred DESC, n.updated_at DESC", nativeQuery = true)
    List<Note> findByFilters(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("isStarred") Integer isStarred,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("tagName") String tagName
    );

    List<Note> findTop8ByUserIdAndStatusOrderByLastAccessedAtDesc(Long userId, Integer status);

    List<Note> findByUserIdAndStatus(Long userId, Integer status);
}