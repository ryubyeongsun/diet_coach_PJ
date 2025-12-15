package com.dietcoach.project.mapper;

import com.dietcoach.project.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    int insertUser(User user);

    User findById(@Param("id") Long id);

    User findByEmail(@Param("email") String email);   // ⭐ 여기 이름이 XML id랑 100% 같아야 함
    
    int updateUserProfile(User user);

    int updateUserEnergy(User user);
}
