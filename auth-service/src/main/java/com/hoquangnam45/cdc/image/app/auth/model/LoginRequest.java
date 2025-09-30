package com.hoquangnam45.cdc.image.app.auth.model;

public record LoginRequest(String username, String password, String phoneNumber, String email) {
}
