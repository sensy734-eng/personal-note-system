package com.example.notesystem.controller;

import com.example.notesystem.entity.Note;
import com.example.notesystem.entity.Tag;
import com.example.notesystem.entity.SearchLog;
import com.example.notesystem.repository.NoteRepository;
import com.example.notesystem.repository.TagRepository;
import com.example.notesystem.repository.SearchLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
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
@CrossOrigin(origins = "*")
public class NoteController {

    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private SearchLogRepository searchLogRepository;

    // ==========================================
    // 1. 列表检索 (增强：自动记录搜索日志)
    // ==========================================
    @GetMapping
    public ResponseEntity<?> getNotes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "1") Integer status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Integer isStarred,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date endDate,
            @RequestParam(required = false) String tagName) {

        Long userId = ((Number) request.getAttribute("userId")).longValue();

        // 🚀 补充细节：记录搜索关键词
        if (keyword != null && !keyword.trim().isEmpty()) {
            SearchLog log = new SearchLog();
            log.setUserId(userId);
            log.setKeyword(keyword.trim());
            searchLogRepository.save(log);
        }

        if (categoryId != null && categoryId <= 0) categoryId = null;
        List<Note> notes = noteRepository.findByFilters(userId, status, keyword, categoryId, isStarred, startDate, endDate, tagName);
        return ResponseEntity.ok(Map.of("message", "获取成功", "data", notes));
    }

    // ==========================================
    // 2. 彻底删除 (3.4 模块补充细节点)
    // ==========================================
    @DeleteMapping("/{id}/permanent")
    public ResponseEntity<?> permanentDeleteNote(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Optional<Note> opt = noteRepository.findById(id);

        if (opt.isPresent() && opt.get().getUserId().equals(userId)) {
            noteRepository.deleteById(id); // 物理删除
            return ResponseEntity.ok(Map.of("message", "笔记已永久删除"));
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
    }

    // ==========================================
    // 3. 原有功能全量保留 (导出、标签、最近访问等)
    // ==========================================
    @GetMapping("/tags")
    public ResponseEntity<?> getUserTags(HttpServletRequest request) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        return ResponseEntity.ok(tagRepository.findByUserId(userId));
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportSingleNote(HttpServletRequest request, @PathVariable Long id, @RequestParam(defaultValue = "md") String type) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note note = noteRepository.findById(id).orElseThrow();
        if (!note.getUserId().equals(userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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

    private ResponseEntity<byte[]> createDownloadResponse(byte[] content, String fileName) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        String encodedName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        headers.setContentDispositionFormData("attachment", encodedName);
        return new ResponseEntity<>(content, headers, HttpStatus.OK);
    }

    @PostMapping("/extract-tags")
    public ResponseEntity<?> extractTags(@RequestBody Map<String, String> body) {
        String text = body.get("text");
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
        List<String> topTags = wordCount.entrySet().stream().sorted((a, b) -> b.getValue().compareTo(a.getValue())).limit(3).map(Map.Entry::getKey).collect(Collectors.toList());
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
        Optional<Note> opt = noteRepository.findById(id);
        if (opt.isPresent() && opt.get().getUserId().equals(userId)) {
            Note note = opt.get();
            note.setLastAccessedAt(new Date());
            noteRepository.save(note);
            return ResponseEntity.ok(Map.of("note", note, "tags", tagRepository.findTagNamesByNoteId(note.getId())));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    @PostMapping
    public ResponseEntity<?> createNote(HttpServletRequest request, @RequestBody Map<String, Object> payload) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note n = new Note();
        n.setUserId(userId);
        n.setTitle((String) payload.get("title"));
        n.setContent((String) payload.get("content"));
        n.setContentText((String) payload.get("contentText"));
        if (payload.get("categoryId") != null) n.setCategoryId(Long.valueOf(payload.get("categoryId").toString()));
        Note saved = noteRepository.save(n);
        saveTagsForNote(userId, saved.getId(), (List<String>) payload.get("tags"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("message", "新建成功", "noteId", saved.getId()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateNote(HttpServletRequest request, @PathVariable Long id, @RequestBody Map<String, Object> payload) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note n = noteRepository.findById(id).orElseThrow();
        if (!n.getUserId().equals(userId)) return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        n.setTitle((String) payload.get("title"));
        n.setContent((String) payload.get("content"));
        n.setContentText((String) payload.get("contentText"));
        n.setCategoryId(payload.get("categoryId") != null ? Long.valueOf(payload.get("categoryId").toString()) : null);
        if (payload.get("isStarred") != null) n.setIsStarred(Integer.valueOf(payload.get("isStarred").toString()));
        noteRepository.save(n);
        saveTagsForNote(userId, n.getId(), (List<String>) payload.get("tags"));
        return ResponseEntity.ok(Map.of("message", "更新成功"));
    }

    private void saveTagsForNote(Long userId, Long noteId, List<String> tagNames) {
        tagRepository.deleteNoteTags(noteId);
        if (tagNames == null) return;
        for (String name : tagNames) {
            if (name.trim().isEmpty()) continue;
            Tag t = tagRepository.findByUserIdAndName(userId, name.trim()).orElseGet(() -> {
                Tag nt = new Tag(); nt.setUserId(userId); nt.setName(name.trim());
                return tagRepository.save(nt);
            });
            tagRepository.addNoteTag(noteId, t.getId());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> softDeleteNote(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        noteRepository.findById(id).filter(n -> n.getUserId().equals(userId)).ifPresent(n -> { n.setStatus(0); noteRepository.save(n); });
        return ResponseEntity.ok(Map.of("message", "已移入回收站"));
    }

    @PutMapping("/{id}/restore")
    public ResponseEntity<?> restoreNote(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        noteRepository.findById(id).filter(n -> n.getUserId().equals(userId)).ifPresent(n -> { n.setStatus(1); noteRepository.save(n); });
        return ResponseEntity.ok(Map.of("message", "笔记已恢复"));
    }

    @PutMapping("/{id}/star")
    public ResponseEntity<?> toggleStar(HttpServletRequest request, @PathVariable Long id) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Note n = noteRepository.findById(id).filter(note -> note.getUserId().equals(userId)).orElseThrow();
        n.setIsStarred(n.getIsStarred() == 1 ? 0 : 1);
        noteRepository.save(n);
        return ResponseEntity.ok(Map.of("message", "操作成功", "is_starred", n.getIsStarred()));
    }
}