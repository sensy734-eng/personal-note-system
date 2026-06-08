package com.example.notesystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "note_tags")
@IdClass(NoteTagId.class)
public class NoteTag {
    @Id
    @Column(name = "note_id")
    private Long noteId;

    @Id
    @Column(name = "tag_id")
    private Long tagId;
}
