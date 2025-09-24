package com.hoquangnam45.cdc.image.app.auth.repository;

import com.hoquangnam45.cdc.image.app.auth.model.RefreshTokenMdl;
import com.hoquangnam45.cdc.image.app.auth.model.UserMdl;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface AuthRepository {
    UserMdl findUser(String username, String phoneNumber, String email);

    void saveRefreshToken(RefreshTokenMdl refreshTokenMdl);
}
