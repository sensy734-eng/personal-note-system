package com.example.notesystem.service;

import com.example.notesystem.entity.User;
import com.example.notesystem.repository.UserRepository;
import com.example.notesystem.utils.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtil jwtUtil;

    // 注册逻辑：增加 email 处理
    public Map<String, Object> register(String username, String password, String email) {
        Map<String, Object> response = new HashMap<>();

        if (userRepository.findByUsername(username).isPresent()) {
            response.put("success", false);
            response.put("message", "该用户名已被注册");
            return response;
        }

        // 密码加密
        String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // 保存用户
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email); // 关键修改
        newUser.setPassword(hashedPassword);
        userRepository.save(newUser);

        response.put("success", true);
        response.put("message", "注册成功");
        return response;
    }

    // 登录逻辑 (保持不变)
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOptional = userRepository.findByUsername(username);

        if (userOptional.isEmpty()) {
            response.put("success", false);
            response.put("message", "用户不存在");
            return response;
        }

        User user = userOptional.get();
        if (!BCrypt.checkpw(password, user.getPassword())) {
            response.put("success", false);
            response.put("message", "密码错误");
            return response;
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        response.put("success", true);
        response.put("message", "登录成功");
        response.put("token", token);

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        response.put("user", userInfo);

        return response;
    }

    // 🚀 新增：密码找回/重置逻辑
    public Map<String, Object> resetPassword(String username, String email, String newPassword) {
        Map<String, Object> response = new HashMap<>();
        Optional<User> userOptional = userRepository.findByUsername(username);

        // 核心校验：用户名存在且注册时填写的邮箱匹配
        if (userOptional.isPresent() && userOptional.get().getEmail().equalsIgnoreCase(email)) {
            User user = userOptional.get();
            // 重置密码
            user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
            userRepository.save(user);

            response.put("success", true);
            response.put("message", "密码修改成功，请重新登录");
        } else {
            response.put("success", false);
            response.put("message", "用户名或注册邮箱校验不匹配");
        }
        return response;
    }
}