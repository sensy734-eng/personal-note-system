package com.example.notesystem.service;

import com.example.notesystem.entity.User;
import com.example.notesystem.exception.ApiException;
import com.example.notesystem.repository.UserRepository;
import com.example.notesystem.utils.JwtUtil;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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

    public Map<String, Object> register(String username, String password, String email) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "该用户名已被注册");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
        newUser.setRole("USER");
        userRepository.save(newUser);

        return Map.of("success", true, "message", "注册成功");
    }

    public Map<String, Object> login(String username, String password) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户不存在");
        }

        User user = userOptional.get();
        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "密码错误");
        }

        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "登录成功");
        response.put("token", token);
        response.put("user", toUserInfo(user));
        return response;
    }

    public Map<String, Object> resetPassword(String username, String email, String newPassword) {
        Optional<User> userOptional = userRepository.findByUsername(username);
        if (userOptional.isEmpty() || !userOptional.get().getEmail().equalsIgnoreCase(email)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "用户名或注册邮箱校验不匹配");
        }

        User user = userOptional.get();
        user.setPassword(BCrypt.hashpw(newPassword, BCrypt.gensalt()));
        userRepository.save(user);
        return Map.of("success", true, "message", "密码修改成功，请重新登录");
    }

    public Map<String, Object> toUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("username", user.getUsername());
        userInfo.put("email", user.getEmail());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatar", user.getAvatar());
        userInfo.put("signature", user.getSignature());
        userInfo.put("role", user.getRole());
        return userInfo;
    }
}
