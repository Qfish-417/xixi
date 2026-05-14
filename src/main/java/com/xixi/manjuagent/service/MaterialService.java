package com.xixi.manjuagent.service;

import com.xixi.manjuagent.config.MinioConfig;
import com.xixi.manjuagent.entity.Material;
import com.xixi.manjuagent.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaterialService {

    private final MaterialMapper materialMapper;
    private final MinioService minioService;
    private final MinioConfig minioConfig;

    /**
     * 构建用户文件夹路径
     * 格式: {userId}/{year}/{month}/{uuid}{extension}
     */
    private String buildUserFolderPath(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return String.format("%d/%d/%d", userId, now.getYear(), now.getMonthValue());
    }

    /**
     * 生成UUID文件名
     */
    private String generateUUIDFileName(String originalFilename) {
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        return UUID.randomUUID().toString() + extension;
    }

    /**
     * 构建完整的存储路径
     * 格式: {userFolder}/{uuid}{extension}
     */
    private String buildStoragePath(Long userId, String uuidFilename) {
        return buildUserFolderPath(userId) + "/" + uuidFilename;
    }

    /**
     * 保存生成的图片（数据库存储元数据，MinIO存储实际文件）
     */
    @Transactional
    public Material saveGeneratedImage(Long userId, String originalPrompt, String optimizedPrompt, 
                                       String modelName, String style, InputStream imageStream, 
                                       long size, String contentType) {
        try {
            // 1. 生成UUID和文件名
            String uuid = UUID.randomUUID().toString();
            String uuidFilename = uuid + ".png";
            
            // 2. 构建存储路径（用户独立文件夹）
            String storagePath = buildStoragePath(userId, uuidFilename);
            
            // 3. 上传图片到MinIO
            minioService.uploadFile(imageStream, storagePath, contentType, size);
            
            // 4. 创建缩略图URL
            String thumbnailUrl = minioService.getImageUrl(storagePath);
            
            // 5. 保存元数据到数据库
            Material material = Material.builder()
                    .userId(userId)
                    .name("生成图片-" + System.currentTimeMillis())
                    .type("image")
                    .uuid(uuid)
                    .originalFilename("generated_image.png")
                    .url(storagePath)  // 存储完整路径
                    .thumbnailUrl(thumbnailUrl)
                    .originalPrompt(originalPrompt)
                    .optimizedPrompt(optimizedPrompt)
                    .modelName(modelName)
                    .style(style)
                    .build();
            
            materialMapper.insert(material);
            log.info("图片保存成功: userId={}, materialId={}, uuid={}", userId, material.getId(), uuid);
            
            // 6. 更新URL为可访问的URL
            material.setUrl(minioService.getImageUrl(storagePath));
            return material;
            
        } catch (Exception e) {
            log.error("保存图片失败: {}", e.getMessage());
            throw new RuntimeException("保存图片失败", e);
        }
    }

    /**
     * 上传用户图片
     */
    @Transactional
    public Material uploadUserImage(Long userId, MultipartFile file) {
        return uploadUserMedia(userId, file, "image");
    }

    /**
     * 上传用户视频
     */
    @Transactional
    public Material uploadUserVideo(Long userId, MultipartFile file) {
        return uploadUserMedia(userId, file, "video");
    }

    /**
     * 上传用户媒体文件（通用方法）
     */
    @Transactional
    public Material uploadUserMedia(Long userId, MultipartFile file, String type) {
        try {
            // 1. 生成UUID和保留原始文件名
            String uuid = UUID.randomUUID().toString();
            String originalFilename = file.getOriginalFilename();
            String uuidFilename = generateUUIDFileName(originalFilename);
            
            // 2. 构建存储路径（用户独立文件夹）
            String storagePath = buildStoragePath(userId, uuidFilename);
            
            // 3. 上传到MinIO
            if ("video".equals(type)) {
                minioService.uploadVideo(file.getInputStream(), storagePath, file.getContentType(), file.getSize());
            } else {
                minioService.uploadFile(file.getInputStream(), storagePath, file.getContentType(), file.getSize());
            }
            
            // 4. 创建缩略图URL
            String thumbnailUrl = "video".equals(type) 
                    ? minioService.getVideoUrl(storagePath)
                    : minioService.getImageUrl(storagePath);
            
            // 5. 保存元数据到数据库（包含UUID和原始文件名的映射）
            Material material = Material.builder()
                    .userId(userId)
                    .name(originalFilename != null ? originalFilename : "上传" + type + "-" + System.currentTimeMillis())
                    .type(type)
                    .uuid(uuid)
                    .originalFilename(originalFilename)
                    .url(storagePath)  // 存储完整路径
                    .thumbnailUrl(thumbnailUrl)
                    .build();
            
            materialMapper.insert(material);
            log.info("用户{}上传成功: userId={}, materialId={}, uuid={}, originalFilename={}", 
                    type, userId, material.getId(), uuid, originalFilename);
            
            // 6. 更新URL为可访问的URL
            if ("video".equals(type)) {
                material.setUrl(minioService.getVideoUrl(storagePath));
            } else {
                material.setUrl(minioService.getImageUrl(storagePath));
            }
            return material;
            
        } catch (Exception e) {
            log.error("上传{}失败: {}", type, e.getMessage());
            throw new RuntimeException("上传" + type + "失败", e);
        }
    }

    /**
     * 保存已生成的图片/视频URL到用户资产库
     */
    @Transactional
    public Material saveGeneratedImageUrl(Long userId, String mediaUrl, String prompt, 
                                           String modelName, String style, String type) {
        try {
            String uuid = UUID.randomUUID().toString();
            String extension = "png";
            String storageUrl = mediaUrl;
            
            // 判断类型并下载到MinIO
            if ("video".equals(type)) {
                extension = "mp4";
                // 下载视频并上传到MinIO
                try {
                    storageUrl = minioService.uploadFromUrl(mediaUrl, "mp4");
                } catch (Exception e) {
                    log.warn("视频上传到MinIO失败，使用原始URL: {}", e.getMessage());
                }
            } else {
                // 下载图片并上传到MinIO
                try {
                    storageUrl = minioService.uploadFromUrl(mediaUrl, "png");
                } catch (Exception e) {
                    log.warn("图片上传到MinIO失败，使用原始URL: {}", e.getMessage());
                }
            }
            
            Material material = Material.builder()
                    .userId(userId)
                    .name(type + "-" + System.currentTimeMillis())
                    .type(type != null ? type : "image")
                    .uuid(uuid)
                    .originalFilename("generated_" + type + "." + extension)
                    .url(storageUrl)
                    .originalPrompt(prompt)
                    .modelName(modelName)
                    .style(style)
                    .build();
            
            materialMapper.insert(material);
            log.info("{}URL保存到资产库: userId={}, materialId={}, uuid={}, url={}", 
                    type, userId, material.getId(), uuid, storageUrl);
            
            return material;
            
        } catch (Exception e) {
            log.error("保存{}URL到资产库失败: {}", type, e.getMessage());
            throw new RuntimeException("保存" + type + "URL到资产库失败", e);
        }
    }

    /**
     * 保存已生成的视频URL到用户资产库
     */
    @Transactional
    public Material saveGeneratedVideoUrl(Long userId, String videoUrl, String prompt, 
                                          String modelName, String style) {
        return saveGeneratedImageUrl(userId, videoUrl, prompt, modelName, style, "video");
    }

    /**
     * 获取图片的可访问URL
     */
    public String getImageUrl(String storagePath) {
        return minioService.getImageUrl(storagePath);
    }

    /**
     * 根据ID获取材料
     */
    public Material getMaterialById(Long id) {
        Material material = materialMapper.selectById(id);
        return enrichMaterialUrl(material);
    }

    /**
     * 根据UUID获取材料
     */
    public Material getMaterialByUuid(String uuid) {
        Material material = materialMapper.selectByUuid(uuid);
        return enrichMaterialUrl(material);
    }

    /**
     * 补充材料的URL（将存储路径转换为可访问URL）
     */
    private Material enrichMaterialUrl(Material material) {
        if (material != null) {
            String url = material.getUrl();
            if (!url.startsWith("http")) {
                if ("video".equals(material.getType())) {
                    material.setUrl(minioService.getVideoUrl(url));
                } else {
                    material.setUrl(minioService.getImageUrl(url));
                }
            }
        }
        return material;
    }

    /**
     * 获取用户的所有材料
     */
    public List<Material> getUserMaterials(Long userId) {
        List<Material> materials = materialMapper.selectByUserId(userId);
        materials.forEach(this::enrichMaterialUrl);
        return materials;
    }

    /**
     * 获取用户的指定类型材料
     */
    public List<Material> getUserMaterialsByType(Long userId, String type) {
        List<Material> materials = materialMapper.selectByUserIdAndType(userId, type);
        materials.forEach(this::enrichMaterialUrl);
        return materials;
    }

    /**
     * 删除材料（同时删除数据库记录和MinIO文件）
     */
    @Transactional
    public void deleteMaterial(Long id) {
        Material material = materialMapper.selectById(id);
        if (material != null) {
            // 1. 删除MinIO中的文件
            String storagePath = material.getUrl();
            if (!storagePath.startsWith("http")) {
                if ("video".equals(material.getType())) {
                    minioService.deleteVideo(storagePath);
                } else {
                    minioService.deleteImage(storagePath);
                }
            }
            
            // 2. 删除数据库记录
            materialMapper.deleteById(id);
            log.info("材料删除成功: id={}, uuid={}", id, material.getUuid());
        }
    }

    /**
     * 更新材料信息
     */
    @Transactional
    public void updateMaterial(Material material) {
        materialMapper.update(material);
        log.info("材料更新成功: id={}, uuid={}", material.getId(), material.getUuid());
    }

    /**
     * 搜索材料
     */
    public List<Material> searchMaterials(Long userId, String keyword) {
        List<Material> materials = materialMapper.searchByKeyword(userId, keyword);
        materials.forEach(this::enrichMaterialUrl);
        return materials;
    }

    /**
     * 根据UUID获取原始文件名
     */
    public String getOriginalFilenameByUuid(String uuid) {
        Material material = materialMapper.selectByUuid(uuid);
        return material != null ? material.getOriginalFilename() : null;
    }

    /**
     * 根据UUID获取存储路径
     */
    public String getStoragePathByUuid(String uuid) {
        Material material = materialMapper.selectByUuid(uuid);
        return material != null ? material.getUrl() : null;
    }
}