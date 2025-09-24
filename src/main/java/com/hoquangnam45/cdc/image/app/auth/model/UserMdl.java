package com.hoquangnam45.cdc.image.app.auth.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserMdl {
    private UUID id;
    private String username;
    private String email;
    private String phoneNumber;
    private String passwordHash;
    private String createdAt;
}
