package com.example.notesystem.repository;

import com.example.notesystem.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    // 查找用户的某个标签
    Optional<Tag> findByUserIdAndName(Long userId, String name);

    // 获取用户的所有标签 (用于前端输入提示)
    List<Tag> findByUserId(Long userId);

    // 获取某篇笔记的所有标签名称
    @Query(value = "SELECT t.name FROM tags t INNER JOIN note_tags nt ON t.id = nt.tag_id WHERE nt.note_id = ?1", nativeQuery = true)
    List<String> findTagNamesByNoteId(Long noteId);

    // 清空某篇笔记的所有标签关联
    @Modifying
    @Transactional
    @Query(value = "DELETE FROM note_tags WHERE note_id = ?1", nativeQuery = true)
    void deleteNoteTags(Long noteId);

    // 绑定笔记与标签
    @Modifying
    @Transactional
    @Query(value = "INSERT IGNORE INTO note_tags (note_id, tag_id) VALUES (?1, ?2)", nativeQuery = true)
    void addNoteTag(Long noteId, Long tagId);
}