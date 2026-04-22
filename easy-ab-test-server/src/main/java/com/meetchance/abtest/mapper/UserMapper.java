package com.meetchance.abtest.mapper;

import com.meetchance.abtest.entity.User;
import org.apache.ibatis.annotations.*;
import java.util.List;
import java.util.Optional;

@Mapper
public interface UserMapper {
    
    @Select("SELECT * FROM users WHERE id = #{id}")
    Optional<User> findById(Long id);
    
    @Select("SELECT * FROM users WHERE username = #{username}")
    Optional<User> findByUsername(String username);
    
    @Select("SELECT * FROM users WHERE email = #{email}")
    Optional<User> findByEmail(String email);
    
    @Select("SELECT * FROM users")
    List<User> findAll();
    
    @Insert("INSERT INTO users (username, password, email, role, created_at, updated_at) " +
            "VALUES (#{username}, #{password}, #{email}, #{role}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int save(User user);
    
    @Update("UPDATE users SET username = #{username}, password = #{password}, email = #{email}, " +
            "role = #{role}, updated_at = NOW() WHERE id = #{id}")
    int update(User user);
    
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);
    
    @Select("SELECT COUNT(*) FROM users WHERE username = #{username}")
    boolean existsByUsername(String username);
    
    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    boolean existsByEmail(String email);
}
