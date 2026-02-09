package com.example.feature.users.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder // Thêm Builder vô sau này gọi new UserDisabledEvent cho dễ nhìn
public class UserDisabledEvent {
    private Long userId;     // Sửa String -> Long (để khớp với User entity)
    private String title;
    private String message;
    private int type;
}