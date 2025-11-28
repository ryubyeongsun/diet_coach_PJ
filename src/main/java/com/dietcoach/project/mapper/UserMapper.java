package com.dietcoach.project.mapper;

import com.dietcoach.project.domain.User;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis mapper for user persistence.
 */
@Mapper
public interface UserMapper {

    int insertUser(User user);

    User findById(Long id);

    User findByEmail(String email);
}
