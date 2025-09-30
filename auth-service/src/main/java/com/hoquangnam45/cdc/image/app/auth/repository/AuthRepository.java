package com.hoquangnam45.cdc.image.app.auth.repository;

import com.hoquangnam45.cdc.image.app.auth.model.RefreshTokenMdl;
import com.hoquangnam45.cdc.image.app.common.model.UserMdl;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Mapper
@Repository
public interface AuthRepository {
    UserMdl findUser(@Param("username") String username, @Param("phoneNumber") String phoneNumber, @Param("email") String email);
    UserMdl getUser(@Param("id") UUID id);
    void saveUser(UserMdl userMdl);
    void saveRefreshToken(RefreshTokenMdl refreshTokenMdl);
    void deleteRefreshToken(@Param("hashedRefreshToken") String hashedRefreshToken);
    RefreshTokenMdl findRefreshToken(@Param("hashedRefreshToken") String hashedRefreshToken);
    int deleteExpiredRefreshTokens(@Param("now") Instant now);
}
