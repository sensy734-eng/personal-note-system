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

    Optional<Tag> findByUserIdAndName(Long userId, String name);

    List<Tag> findByUserId(Long userId);

    @Query(value = "SELECT t.name FROM tags t INNER JOIN note_tags nt ON t.id = nt.tag_id WHERE nt.note_id = ?1", nativeQuery = true)
    List<String> findTagNamesByNoteId(Long noteId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM note_tags WHERE note_id = ?1", nativeQuery = true)
    void deleteNoteTags(Long noteId);

    @Modifying
    @Transactional
    @Query(value = "MERGE INTO note_tags (note_id, tag_id) KEY (note_id, tag_id) VALUES (?1, ?2)", nativeQuery = true)
    void addNoteTag(Long noteId, Long tagId);
}
