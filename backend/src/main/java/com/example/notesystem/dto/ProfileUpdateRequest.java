package com.example.notesystem.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 50, message = "昵称不能超过50个字符")
    private String nickname;

    @Size(max = 255, message = "个性签名不能超过255个字符")
    private String signature;
}
