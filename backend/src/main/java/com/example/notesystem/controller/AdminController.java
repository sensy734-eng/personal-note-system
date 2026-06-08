package com.example.notesystem.controller;

import com.example.notesystem.exception.ApiException;
import com.example.notesystem.repository.CategoryRepository;
import com.example.notesystem.repository.NoteRepository;
import com.example.notesystem.repository.UserRepository;
import com.example.notesystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private NoteRepository noteRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserService userService;

    @GetMapping("/overview")
    public ResponseEntity<?> overview(HttpServletRequest request) {
        requireAdmin(request);
        long totalUsers = userRepository.count();
        long totalNotes = noteRepository.count();
        long activeNotes = noteRepository.countByStatus(1);
        long totalCategories = categoryRepository.count();
        return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalNotes", totalNotes,
                "activeNotes", activeNotes,
                "totalCategories", totalCategories
        ));
    }

    @GetMapping("/users")
    public ResponseEntity<?> users(HttpServletRequest request) {
        requireAdmin(request);
        return ResponseEntity.ok(userRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(userService::toUserInfo)
                .collect(Collectors.toList()));
    }

    private void requireAdmin(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equals(role)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "仅管理员可访问该功能");
        }
    }
}
