package com.example.notesystem.controller;

import com.example.notesystem.entity.User;
import com.example.notesystem.repository.UserRepository;
import com.example.notesystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-path}")
    private String uploadPath;

    // 🚀 修改：注册接口增加对 email 的支持
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> form) {
        String username = form.get("username");
        String password = form.get("password");
        String email = form.get("email"); // 获取邮箱

        Map<String, Object> result = userService.register(username, password, email);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> form) {
        String username = form.get("username");
        String password = form.get("password");
        Map<String, Object> result = userService.login(username, password);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    // 🚀 新增：密码找回/重置接口
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> form) {
        String username = form.get("username");
        String email = form.get("email");
        String newPassword = form.get("newPassword");

        Map<String, Object> result = userService.resetPassword(username, email, newPassword);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) return ResponseEntity.status(HttpStatus.NOT_FOUND).body("用户不存在");

        User user = userOpt.get();
        if (body.containsKey("nickname")) user.setNickname(body.get("nickname"));
        if (body.containsKey("signature")) user.setSignature(body.get("signature"));

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "个人信息更新成功", "user", user));
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("文件不能为空");

        Long userId = ((Number) request.getAttribute("userId")).longValue();

        File folder = new File(uploadPath);
        if (!folder.exists()) folder.mkdirs();

        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + suffix;

        try {
            file.transferTo(new File(folder, fileName));

            User user = userRepository.findById(userId).orElseThrow();
            // 建议在 UserController 中注入配置
            @Value("${app.image-base-url}")
            private String imageBaseUrl;
            
            // 在 uploadAvatar 方法中拼接
            String avatarUrl = imageBaseUrl + fileName;
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of("message", "头像上传成功", "url", avatarUrl));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件保存失败");
        }
    }
}
