package com.xixi.manjuagent.controller;

import com.xixi.manjuagent.entity.Material;
import com.xixi.manjuagent.service.MaterialService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/materials")
@RequiredArgsConstructor
@Slf4j
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserMaterials(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword) {
        
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        List<Material> materials;
        if (keyword != null && !keyword.isEmpty()) {
            materials = materialService.searchMaterials(userId, keyword);
        } else if (type != null && !type.isEmpty()) {
            materials = materialService.getUserMaterialsByType(userId, type);
        } else {
            materials = materialService.getUserMaterials(userId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("materials", materials);
        response.put("total", materials.size());
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMaterial(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        if (!material.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限访问"));
        }

        return ResponseEntity.ok(material);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteMaterial(@PathVariable Long id) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        if (!material.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限删除"));
        }

        materialService.deleteMaterial(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMaterial(@PathVariable Long id, @RequestBody Material updateData) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        if (!material.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限修改"));
        }

        if (updateData.getName() != null) {
            material.setName(updateData.getName());
        }
        if (updateData.getTags() != null) {
            material.setTags(updateData.getTags());
        }
        if (updateData.getDescription() != null) {
            material.setDescription(updateData.getDescription());
        }

        materialService.updateMaterial(material);
        return ResponseEntity.ok(material);
    }

    /**
     * 更新素材名称
     */
    @PutMapping("/{id}/name")
    public ResponseEntity<?> updateMaterialName(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        String name = request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "名称不能为空"));
        }

        Material material = materialService.getMaterialById(id);
        if (material == null) {
            return ResponseEntity.notFound().build();
        }

        if (!material.getUserId().equals(userId)) {
            return ResponseEntity.status(403).body(Map.of("error", "无权限修改"));
        }

        material.setName(name.trim());
        materialService.updateMaterial(material);
        
        return ResponseEntity.ok(Map.of("message", "名称修改成功", "material", material));
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getMaterialStats() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        List<Material> allMaterials = materialService.getUserMaterials(userId);
        
        long imageCount = allMaterials.stream().filter(m -> "image".equals(m.getType())).count();
        long videoCount = allMaterials.stream().filter(m -> "video".equals(m.getType())).count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", allMaterials.size());
        stats.put("imageCount", imageCount);
        stats.put("videoCount", videoCount);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * 上传素材（支持图片和视频）
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadMaterial(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "type", required = false) String type) {
        
        Long userId = getCurrentUserId();
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未认证"));
        }

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "请选择文件"));
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "无法识别文件类型"));
        }

        // 根据文件类型自动判断或使用传入的type参数
        String materialType;
        if (type != null && !type.isEmpty()) {
            materialType = type;
        } else if (contentType.startsWith("image/")) {
            materialType = "image";
        } else if (contentType.startsWith("video/")) {
            materialType = "video";
        } else {
            return ResponseEntity.badRequest().body(Map.of("error", "不支持的文件类型"));
        }

        try {
            Material material;
            if ("video".equals(materialType)) {
                material = materialService.uploadUserVideo(userId, file);
            } else {
                material = materialService.uploadUserImage(userId, file);
            }
            return ResponseEntity.ok(Map.of("message", "上传成功", "material", material));
        } catch (Exception e) {
            log.error("上传失败: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", "上传失败: " + e.getMessage()));
        }
    }

    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return (Long) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.warn("无法获取当前用户ID: {}", e.getMessage());
        }
        return null;
    }
}