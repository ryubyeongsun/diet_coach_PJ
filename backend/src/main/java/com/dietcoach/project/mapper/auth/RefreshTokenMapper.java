// com.dietcoach.project.mapper.auth.RefreshTokenMapper
package com.dietcoach.project.mapper.auth;

import com.dietcoach.project.domain.auth.RefreshToken;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RefreshTokenMapper {
    RefreshToken findByTokenHash(@Param("tokenHash") String tokenHash);
    int insert(RefreshToken token);
    int revokeById(@Param("id") Long id);
}
