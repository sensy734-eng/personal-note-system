package com.example.notesystem.repository;
import com.example.notesystem.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    // 统计高频搜索词
    @Query(value = "SELECT keyword as name, COUNT(*) as value FROM search_logs WHERE user_id = ?1 GROUP BY keyword ORDER BY value DESC LIMIT 10", nativeQuery = true)
    List<Map<String, Object>> findTopKeywords(Long userId);

    // 统计活跃时段分布
    @Query(value = "SELECT HOUR(created_at) as hour, COUNT(*) as count FROM notes WHERE user_id = ?1 GROUP BY hour ORDER BY hour", nativeQuery = true)
    List<Map<String, Object>> findHourlyDistribution(Long userId);
}