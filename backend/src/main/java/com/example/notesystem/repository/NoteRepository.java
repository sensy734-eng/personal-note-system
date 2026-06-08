package com.example.notesystem.repository;

import com.example.notesystem.entity.Note;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface NoteRepository extends JpaRepository<Note, Long> {

    @Query("""
            SELECT n FROM Note n
            WHERE n.userId = :userId AND n.status = :status
            AND (:keyword IS NULL OR n.title LIKE CONCAT('%', :keyword, '%') OR n.contentText LIKE CONCAT('%', :keyword, '%'))
            AND (:categoryId IS NULL OR n.categoryId = :categoryId)
            AND (:isStarred IS NULL OR n.isStarred = :isStarred)
            AND (:startDate IS NULL OR n.updatedAt >= :startDate)
            AND (:endDate IS NULL OR n.updatedAt <= :endDate)
            AND (:tagName IS NULL OR :tagName IS NOT NULL)
            ORDER BY n.isStarred DESC, n.updatedAt DESC
            """)
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

    @Query("""
            SELECT n FROM Note n
            WHERE n.userId = :userId AND n.status = :status
            AND (:keyword IS NULL OR n.title LIKE CONCAT('%', :keyword, '%') OR n.contentText LIKE CONCAT('%', :keyword, '%'))
            AND (:categoryId IS NULL OR n.categoryId = :categoryId)
            AND (:isStarred IS NULL OR n.isStarred = :isStarred)
            AND (:startDate IS NULL OR n.updatedAt >= :startDate)
            AND (:endDate IS NULL OR n.updatedAt <= :endDate)
            AND (:tagName IS NULL OR :tagName IS NOT NULL)
            ORDER BY n.isStarred DESC, n.updatedAt DESC
            """)
    Page<Note> findPageByFilters(
            @Param("userId") Long userId,
            @Param("status") Integer status,
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("isStarred") Integer isStarred,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate,
            @Param("tagName") String tagName,
            Pageable pageable
    );

    List<Note> findTop8ByUserIdAndStatusOrderByLastAccessedAtDesc(Long userId, Integer status);

    List<Note> findByUserIdAndStatus(Long userId, Integer status);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, Integer status);

    long countByStatus(Integer status);
}
