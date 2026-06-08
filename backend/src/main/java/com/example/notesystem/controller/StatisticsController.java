package com.example.notesystem.controller;

import com.example.notesystem.entity.Category;
import com.example.notesystem.entity.Note;
import com.example.notesystem.entity.SearchLog;
import com.example.notesystem.repository.CategoryRepository;
import com.example.notesystem.repository.NoteRepository;
import com.example.notesystem.repository.SearchLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/statistics")
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

        long totalNotes = activeNotes.size();
        long totalStarred = activeNotes.stream().filter(n -> Integer.valueOf(1).equals(n.getIsStarred())).count();
        long totalCategories = categories.size();

        Map<Long, String> categoryNameMap = categories.stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
        Map<String, Long> categoryCount = new LinkedHashMap<>();
        long uncategorizedCount = 0;
        for (Note note : activeNotes) {
            if (note.getCategoryId() != null && categoryNameMap.containsKey(note.getCategoryId())) {
                String name = categoryNameMap.get(note.getCategoryId());
                categoryCount.put(name, categoryCount.getOrDefault(name, 0L) + 1);
            } else {
                uncategorizedCount++;
            }
        }
        if (uncategorizedCount > 0) {
            categoryCount.put("未分类", uncategorizedCount);
        }

        List<Map<String, Object>> pieData = new ArrayList<>();
        categoryCount.forEach((name, count) -> {
            Map<String, Object> item = new HashMap<>();
            item.put("name", name);
            item.put("value", count);
            pieData.add(item);
        });

        SimpleDateFormat sdf = new SimpleDateFormat("MM-dd");
        Map<String, Integer> dateCountMap = new LinkedHashMap<>();
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);
        for (int i = 0; i < 7; i++) {
            dateCountMap.put(sdf.format(cal.getTime()), 0);
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        for (Note note : activeNotes) {
            Date createdAt = note.getCreatedAt();
            if (createdAt != null) {
                String dateKey = sdf.format(createdAt);
                if (dateCountMap.containsKey(dateKey)) {
                    dateCountMap.put(dateKey, dateCountMap.get(dateKey) + 1);
                }
            }
        }

        List<SearchLog> logs = searchLogRepository.findByUserId(userId);
        Map<String, Long> keywordCounts = logs.stream()
                .filter(log -> log.getKeyword() != null && !log.getKeyword().isBlank())
                .collect(Collectors.groupingBy(SearchLog::getKeyword, Collectors.counting()));
        List<Map<String, Object>> hotKeywords = keywordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("name", entry.getKey());
                    item.put("value", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());

        int[] hourlyStats = new int[24];
        Calendar hourCalendar = Calendar.getInstance();
        for (Note note : activeNotes) {
            Date createdAt = note.getCreatedAt();
            if (createdAt != null) {
                hourCalendar.setTime(createdAt);
                hourlyStats[hourCalendar.get(Calendar.HOUR_OF_DAY)]++;
            }
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
