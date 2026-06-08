package com.example.notesystem.controller;

import com.example.notesystem.dto.LoginRequest;
import com.example.notesystem.dto.ProfileUpdateRequest;
import com.example.notesystem.dto.RegisterRequest;
import com.example.notesystem.dto.ResetPasswordRequest;
import com.example.notesystem.entity.User;
import com.example.notesystem.exception.ApiException;
import com.example.notesystem.repository.UserRepository;
import com.example.notesystem.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Value("${file.upload-path}")
    private String uploadPath;

    @Value("${app.image-base-url}")
    private String imageBaseUrl;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest form) {
        Map<String, Object> result = userService.register(form.getUsername(), form.getPassword(), form.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest form) {
        return ResponseEntity.ok(userService.login(form.getUsername(), form.getPassword()));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest form) {
        return ResponseEntity.ok(userService.resetPassword(form.getUsername(), form.getEmail(), form.getNewPassword()));
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(HttpServletRequest request, @Valid @RequestBody ProfileUpdateRequest body) {
        Long userId = ((Number) request.getAttribute("userId")).longValue();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));

        if (body.getNickname() != null) user.setNickname(body.getNickname());
        if (body.getSignature() != null) user.setSignature(body.getSignature());

        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message", "个人信息更新成功", "user", userService.toUserInfo(user)));
    }

    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(HttpServletRequest request, @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new ApiException(HttpStatus.BAD_REQUEST, "文件不能为空");

        Long userId = ((Number) request.getAttribute("userId")).longValue();
        File folder = new File(uploadPath);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "上传目录创建失败");
        }

        String originalFilename = file.getOriginalFilename();
        String suffix = (originalFilename != null && originalFilename.contains("."))
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".jpg";
        String fileName = UUID.randomUUID() + suffix;

        try {
            file.transferTo(new File(folder, fileName));
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
            String avatarUrl = imageBaseUrl + fileName;
            user.setAvatar(avatarUrl);
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "头像上传成功", "url", avatarUrl));
        } catch (IOException e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "文件保存失败");
        }
    }
}
