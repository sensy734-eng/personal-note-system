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
// 注意：如果你已经在 WebConfig 里配置了全局 CORS，这里的 @CrossOrigin 可以省略

public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-path}")
    private String uploadPath;

    // 🚀 修正：将注解移到类成员变量位置
    @Value("${app.image-base-url}")
    private String imageBaseUrl;

    /**
     * 用户注册接口
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> form) {
        String username = form.get("username");
        String password = form.get("password");
        String email = form.get("email");

        Map<String, Object> result = userService.register(username, password, email);

        if ((Boolean) result.get("success")) {
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(result);
        }
    }

    /**
     * 用户登录接口
     */
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

    /**
     * 密码找回/重置接口
     */
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

    /**
     * 更新个人信息接口
     */
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

    /**
     * 头像上传接口
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) return ResponseEntity.badRequest().body("文件不能为空");

        Long userId = ((Number) request.getAttribute("userId")).longValue();

        // 检查并创建上传目录
        File folder = new File(uploadPath);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        // 生成唯一文件名
        String originalFilename = file.getOriginalFilename();
        String suffix = (originalFilename != null && originalFilename.contains(".")) 
                ? originalFilename.substring(originalFilename.lastIndexOf(".")) 
                : ".jpg";
        String fileName = UUID.randomUUID().toString() + suffix;

        try {
            // 保存文件到本地/容器磁盘
            file.transferTo(new File(folder, fileName));

            // 更新用户信息中的头像地址
            User user = userRepository.findById(userId).orElseThrow();
            
            // 🚀 直接使用类成员变量 imageBaseUrl
            String avatarUrl = imageBaseUrl + fileName;
            user.setAvatar(avatarUrl);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                "message", "头像上传成功", 
                "url", avatarUrl
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("文件保存失败");
        }
    }
}
