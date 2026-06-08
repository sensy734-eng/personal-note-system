package com.example.notesystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class NoteRequest {
    @NotBlank(message = "笔记标题不能为空")
    @Size(max = 150, message = "笔记标题不能超过150个字符")
    private String title;

    private String content;
    private String contentText;
    private Long categoryId;
    private Integer isStarred;
    private List<String> tags;
}
