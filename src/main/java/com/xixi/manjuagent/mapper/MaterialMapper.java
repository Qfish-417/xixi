package com.xixi.manjuagent.mapper;

import com.xixi.manjuagent.entity.Material;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MaterialMapper {

    @Insert("INSERT INTO materials (user_id, group_id, name, type, uuid, original_filename, url, thumbnail_url, tags, description, original_prompt, optimized_prompt, model_name, style, created_at, updated_at) " +
            "VALUES (#{userId}, #{groupId}, #{name}, #{type}, #{uuid}, #{originalFilename}, #{url}, #{thumbnailUrl}, #{tags}, #{description}, #{originalPrompt}, #{optimizedPrompt}, #{modelName}, #{style}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Material material);

    @Select("SELECT * FROM materials WHERE id = #{id}")
    Material selectById(Long id);

    @Select("SELECT * FROM materials WHERE user_id = #{userId} ORDER BY created_at DESC")
    List<Material> selectByUserId(Long userId);

    @Select("SELECT * FROM materials WHERE user_id = #{userId} AND type = #{type} ORDER BY created_at DESC")
    List<Material> selectByUserIdAndType(Long userId, String type);

    @Select("SELECT * FROM materials WHERE group_id = #{groupId} ORDER BY created_at DESC")
    List<Material> selectByGroupId(Long groupId);

    @Update("UPDATE materials SET name = #{name}, tags = #{tags}, description = #{description}, original_filename = #{originalFilename}, updated_at = NOW() WHERE id = #{id}")
    int update(Material material);
    
    @Select("SELECT * FROM materials WHERE uuid = #{uuid}")
    Material selectByUuid(String uuid);

    @Delete("DELETE FROM materials WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT * FROM materials WHERE user_id = #{userId} AND name LIKE CONCAT('%', #{keyword}, '%') ORDER BY created_at DESC")
    List<Material> searchByKeyword(Long userId, String keyword);
}