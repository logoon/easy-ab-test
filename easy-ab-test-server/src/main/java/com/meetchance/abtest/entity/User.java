package com.meetchance.abtest.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String email;
    private String role = "USER";
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
