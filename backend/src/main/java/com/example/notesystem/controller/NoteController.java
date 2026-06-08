package com.example.notesystem.controller;

import com.example.notesystem.dto.ExtractTagsRequest;
import com.example.notesystem.dto.NoteRequest;
import com.example.notesystem.entity.Note;
import com.example.notesystem.entity.SearchLog;
import com.example.notesystem.entity.Tag;
import com.example.notesystem.exception.ApiException;
import com.example.notesystem.repository.NoteRepository;
import com.example.notesystem.repository.SearchLogRepository;
import com.example.notesystem.repository.TagRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/notes")
public class NoteController {

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private SearchLogRepository searchLogRepository;

    @GetMapping
    public ResponseEntity<?> getNotes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer isStarred,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String tagName,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "9") Integer size) {

        Long userId = ((Number) request.getAttribute("userId")).longValue();
        if (keyword != null && !keyword.trim().isEmpty()) {
            SearchLog log = new SearchLog();
            log.setUserId(userId);
            log.setKeyword(keyword.trim());
            searchLogRepository.save(log);
        }

        if (categoryId != null && categoryId <= 0) categoryId = null;
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageRequest = PageRequest.of(safePage - 1, safeSize);

        Page<Note> notePage = noteRepository.findPageByFilters(
                userId, status, blankToNull(keyword), categoryId, isStarred, startDate, endOfDay(endDate), blankToNull(tagName), pageRequest);

        return ResponseEntity.ok(Map.of(
                "message", "获取成功",
                "data", notePage.getContent(),
                "records", notePage.getContent(),
                "total", notePage.getTotalElements(),
                "page", safePage,
                "size", safeSize,
                "totalPages", notePage.getTotalPages()
        ));
    }

    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<?> permanentDeleteNote(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = requireOwnNote(userId, id);
        noteRepository.deleteById(note.getId());
        return ResponseEntity.ok(Map.of("message", "笔记已永久删除"));
    }

    @GetMapping("/tags")
    public ResponseEntity<?> getUserTags(HttpServletRequest request) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        return ResponseEntity.ok(tagRepository.findByUserId(userId));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportSingleNote(HttpServletRequest request, @PathVariable Long id, @RequestParam(defaultValue = "md") String type) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = requireOwnNote(userId, id);
        String content = "md".equalsIgnoreCase(type) ? note.getContent() : note.getContentText();
        String fileName = note.getTitle() + ("md".equalsIgnoreCase(type) ? ".md" : ".txt");
        return createDownloadResponse((content != null ? content : "").getBytes(StandardCharsets.UTF_8), fileName);
    }

    @GetMapping("/export/category")
    public ResponseEntity<byte[]> exportCategoryNotes(HttpServletRequest request, @RequestParam Long categoryId, @RequestParam(defaultValue = "md") String type) throws IOException {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        List<Note> notes = noteRepository.findByFilters(userId, 1, null, categoryId, null, null, null, null);
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Note note : notes) {
                String content = "md".equalsIgnoreCase(type) ? note.getContent() : note.getContentText();
                zos.putNextEntry(new ZipEntry(note.getTitle() + "_" + note.getId() + ("md".equalsIgnoreCase(type) ? ".md" : ".txt")));
                zos.write((content != null ? content : "").getBytes(StandardCharsets.UTF_8));
                zos.closeEntry();
            }
            zos.finish();
            return createDownloadResponse(baos.toByteArray(), "category_export_" + categoryId + ".zip");
        }
    }

    @PostMapping("/extract-tags")
    public ResponseEntity<?> extractTags(@RequestBody ExtractTagsRequest body) {
        String text = body.getText();
        if (text == null || text.trim().isEmpty()) return ResponseEntity.ok(new ArrayList<>());
        String cleanText = text.replaceAll("[^a-zA-Z0-9\\u4e00-\\u9fa5]", " ").toLowerCase();
        Set<String> stopWords = Set.of("的", "了", "是", "在", "我", "有", "和", "就", "不", "人", "都", "一", "一个", "上", "也", "很", "到", "说", "要", "去", "你", "会", "着", "没有", "看", "好", "自己", "这");
        Map<String, Integer> wordCount = new HashMap<>();
        String[] parts = cleanText.split("\\s+");
        for (String part : parts) {
            if (part.matches("[a-z0-9]+")) {
                if (part.length() > 2) wordCount.put(part, wordCount.getOrDefault(part, 0) + 1);
            } else {
                for (int i = 0; i < part.length() - 1; i++) {
                    String word2 = part.substring(i, Math.min(i + 2, part.length()));
                    if (!stopWords.contains(word2)) wordCount.put(word2, wordCount.getOrDefault(word2, 0) + 1);
                }
            }
        }
        List<String> topTags = wordCount.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        return ResponseEntity.ok(topTags);
    }

    @GetMapping("/recent")
    public ResponseEntity<?> getRecentNotes(HttpServletRequest request) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        return ResponseEntity.ok(noteRepository.findTop8ByUserIdAndStatusOrderByLastAccessedAtDesc(userId, 1));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getNoteDetail(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = requireOwnNote(userId, id);
        note.setLastAccessedAt(new Date());
        noteRepository.save(note);
        return ResponseEntity.ok(Map.of("note", note, "tags", tagRepository.findTagNamesByNoteId(note.getId())));
    }

    @PostMapping
    public ResponseEntity<?> createNote(HttpServletRequest request, @Valid @RequestBody NoteRequest payload) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = new Note();
        note.setUserId(userId);
        applyNotePayload(note, payload);
        Note saved = noteRepository.save(note);
        saveTagsForNote(userId, saved.getId(), payload.getTags());
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "新建成功", "noteId", saved.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(HttpServletRequest request, @PathVariable Long id, @Valid @RequestBody NoteRequest payload) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = requireOwnNote(userId, id);
        applyNotePayload(note, payload);
        noteRepository.save(note);
        saveTagsForNote(userId, note.getId(), payload.getTags());
        return ResponseEntity.ok(Map.of("message", "更新成功"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDeleteNote(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = requireOwnNote(userId, id);
        note.setStatus(0);
        note.setDeletedAt(new Date());
        noteRepository.save(note);
        return ResponseEntity.ok(Map.of("message", "已移入回收站"));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreNote(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = requireOwnNote(userId, id);
        note.setStatus(1);
        note.setDeletedAt(null);
        noteRepository.save(note);
        return ResponseEntity.ok(Map.of("message", "笔记已恢复"));
    }

    @PutMapping("/{id}/star")
    public ResponseEntity<?> toggleStar(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = requireOwnNote(userId, id);
        note.setIsStarred(note.getIsStarred() == 1 ? 0 : 1);
        noteRepository.save(note);
        return ResponseEntity.ok(Map.of("message", "操作成功", "is_starred", note.getIsStarred()));
    }

    private void applyNotePayload(Note note, NoteRequest payload) {
        note.setTitle(payload.getTitle());
        note.setContent(payload.getContent());
        note.setContentText(payload.getContentText());
        note.setCategoryId(payload.getCategoryId());
        if (payload.getIsStarred() != null) {
            note.setIsStarred(payload.getIsStarred());
        }
    }

    private void saveTagsForNote(Long userId, Long noteId, List<String> tagNames) {
        tagRepository.deleteNoteTags(noteId);
        if (tagNames == null) return;
        for (String name : tagNames) {
            if (name == null || name.trim().isEmpty()) continue;
            Tag tag = tagRepository.findByUserIdAndName(userId, name.trim()).orElseGet(() -> {
                Tag newTag = new Tag();
                newTag.setUserId(userId);
                newTag.setName(name.trim());
                return tagRepository.save(newTag);
            });
            tagRepository.addNoteTag(noteId, tag.getId());
        }
    }

    private Note requireOwnNote(Long userId, Long id) {
        return noteRepository.findById(id)
                .filter(note -> note.getUserId().equals(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "笔记不存在或无权访问"));
    }

    private ResponseEntity<byte[]> createDownloadResponse(byte[] content, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String encodedName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        headers.setContentDispositionFormData("attachment", encodedName);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    private String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    private Date endOfDay(Date value) {
        if (value == null) return null;
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(value);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTime();
    }
}
