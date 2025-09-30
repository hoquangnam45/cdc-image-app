package com.hoquangnam45.cdc.image.app.auth.repository;

import com.hoquangnam45.cdc.image.app.auth.model.RefreshTokenMdl;
import com.hoquangnam45.cdc.image.app.common.model.UserMdl;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Mapper
@Repository
public interface AuthRepository {
    UserMdl findUser(String username, String phoneNumber, String email);
    UserMdl getUser(UUID id);
    void saveUser(UserMdl userMdl);
    void saveRefreshToken(RefreshTokenMdl refreshTokenMdl);
    void deleteRefreshToken(String hashedRefreshToken);
    RefreshTokenMdl findRefreshToken(String hashedRefreshToken);
    int deleteExpiredRefreshTokens(Instant now);
}
