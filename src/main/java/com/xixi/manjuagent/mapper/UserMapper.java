package com.xixi.manjuagent.mapper;

import com.xixi.manjuagent.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Insert("INSERT INTO users (email, password, nickname, avatar, status, created_at, updated_at) " +
            "VALUES (#{email}, #{password}, #{nickname}, #{avatar}, #{status}, #{createdAt}, #{updatedAt})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT * FROM users WHERE id = #{id}")
    User findById(Long id);

    @Select("SELECT * FROM users WHERE email = #{email}")
    User findByEmail(String email);

    @Select("SELECT * FROM users")
    List<User> findAll();

    @Update("UPDATE users SET " +
            "nickname = #{nickname}, " +
            "avatar = #{avatar}, " +
            "status = #{status}, " +
            "updated_at = #{updatedAt}, " +
            "last_login_at = #{lastLoginAt} " +
            "WHERE id = #{id}")
    int update(User user);

    @Update("UPDATE users SET password = #{password}, updated_at = #{updatedAt} WHERE id = #{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password, @Param("updatedAt") java.time.LocalDateTime updatedAt);

    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM users WHERE email = #{email}")
    int countByEmail(String email);
}
