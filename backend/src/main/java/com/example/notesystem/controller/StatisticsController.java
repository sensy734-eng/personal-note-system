package com.example.notesystem.controller;

import com.example.notesystem.entity.Category;
import com.example.notesystem.entity.Note;
import com.example.notesystem.repository.CategoryRepository;
import com.example.notesystem.repository.NoteRepository;
import com.example.notesystem.repository.SearchLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
@CrossOrigin(origins = "*")
public class StatisticsController {

    @Autowired
    private NoteRepository noteRepository;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private SearchLogRepository searchLogRepository;

    @GetMapping
    public ResponseEntity<?> getStatistics(HttpServletRequest request) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();

        List<Note> activeNotes = noteRepository.findByUserIdAndStatus(userId, 1);
        List<Category> categories = categoryRepository.findByUserIdOrderByCreatedAtAsc(userId);

        // 1. 基础面板
        long totalNotes = activeNotes.size();
        long totalStarred = activeNotes.stream().filter(n -> n.getIsStarred() == 1).count();
        long totalCategories = categories.size();

        // 2. 分类占比
        Map<Long, String> categoryNameMap = categories.stream().collect(Collectors.toMap(Category::getId, Category::getName));
        Map<String, Long> categoryCount = new HashMap<>();
        long uncategorizedCount = 0;
        for (Note note : activeNotes) {
            if (note.getCategoryId() != null && categoryNameMap.containsKey(note.getCategoryId())) {
                categoryCount.put(categoryNameMap.get(note.getCategoryId()), categoryCount.getOrDefault(categoryNameMap.get(note.getCategoryId()), 0L) + 1);
            } else { uncategorizedCount++; }
        }
        if (uncategorizedCount > 0) categoryCount.put("未分类", uncategorizedCount);
        List<Map<String, Object>> pieData = new ArrayList<>();
        categoryCount.forEach((name, count) -> { Map<String, Object> map = new HashMap<>(); map.put("name", name); map.put("value", count); pieData.add(map); });

        // 3. 近7天趋势
        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        Map<String, Integer> dateCountMap = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) { dateCountMap.put(sdf.format(cal.getTime()), 0); cal.add(Calendar.DAY_OF_YEAR, 1); }
        for (Note note : activeNotes) { if (note.getCreatedAt() != null) { String ds = sdf.format(note.getCreatedAt()); if (dateCountMap.containsKey(ds)) dateCountMap.put(ds, dateCountMap.get(ds) + 1); } }

        // 🚀 4. 新增：高频搜索词统计
        List<Map<String, Object>> hotKeywords = searchLogRepository.findTopKeywords(userId);

        // 🚀 5. 新增：活跃时段分布 (0-23点)
        List<Map<String, Object>> rawHourly = searchLogRepository.findHourlyDistribution(userId);
        int[] hourlyStats = new int[24];
        for (Map<String, Object> m : rawHourly) {
            int h = ((Number) m.get("hour")).intValue();
            int c = ((Number) m.get("count")).intValue();
            if (h >= 0 && h < 24) hourlyStats[h] = c;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("totalNotes", totalNotes);
        res.put("totalStarred", totalStarred);
        res.put("totalCategories", totalCategories);
        res.put("pieData", pieData);
        res.put("trendDates", new ArrayList<>(dateCountMap.keySet()));
        res.put("trendCounts", new ArrayList<>(dateCountMap.values()));
        res.put("hotKeywords", hotKeywords);
        res.put("hourlyStats", hourlyStats);

        return ResponseEntity.ok(res);
    }
}