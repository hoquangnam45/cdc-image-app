package com.hoquangnam45.cdc.image.app.common.model;

import java.util.UUID;

public record JwtUser(UUID id, String username, String email, String phoneNumber, boolean emailConfirmed, boolean phoneNumberConfirmed) {
}
