package com.xixi.manjuagent.controller;

import com.xixi.manjuagent.library.Group;
import com.xixi.manjuagent.library.GroupService;
import com.xixi.manjuagent.library.Material;
import com.xixi.manjuagent.library.MaterialService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/library")
@Slf4j
public class LibraryController {

    private final MaterialService materialService;
    private final GroupService groupService;

    public LibraryController(MaterialService materialService, GroupService groupService) {
        this.materialService = materialService;
        this.groupService = groupService;
    }

    // ==================== 素材相关API ====================

    @PostMapping("/materials")
    public ResponseEntity<Material> createMaterial(@RequestBody Material material) {
        log.info("创建素材: {}", material.getName());
        Material created = materialService.createMaterial(material);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/groups/{groupId}/materials")
    public ResponseEntity<Material> createMaterialInGroup(
            @PathVariable String groupId,
            @RequestBody Material material) {
        log.info("创建素材到组别 {}: {}", groupId, material.getName());
        Material created = materialService.createMaterial(groupId, material);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/materials")
    public ResponseEntity<List<Material>> getAllMaterials(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String groupId,
            @RequestParam(required = false) String userId) {
        
        List<Material> materials;
        
        if (groupId != null && !groupId.isEmpty()) {
            materials = materialService.getMaterialsByGroup(groupId);
        } else if (userId != null && !userId.isEmpty()) {
            materials = materialService.getMaterialsByUser(userId);
        } else if (category != null && !category.isEmpty()) {
            materials = materialService.getMaterialsByCategory(category);
        } else if (type != null && !type.isEmpty()) {
            materials = materialService.getMaterialsByType(type);
        } else {
            materials = materialService.getAllMaterials();
        }
        
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/materials/{id}")
    public ResponseEntity<?> getMaterial(@PathVariable String id) {
        return materialService.getMaterial(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/materials/{id}")
    public ResponseEntity<?> updateMaterial(
            @PathVariable String id,
            @RequestBody Material update) {
        try {
            Material updated = materialService.updateMaterial(id, update);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<?> deleteMaterial(@PathVariable String id) {
        if (materialService.deleteMaterial(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/materials/search")
    public ResponseEntity<List<Material>> searchMaterials(@RequestParam String keyword) {
        List<Material> results = materialService.searchMaterials(keyword);
        return ResponseEntity.ok(results);
    }

    @GetMapping("/materials/categories")
    public ResponseEntity<List<String>> getAllCategories() {
        List<String> categories = materialService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/materials/tags")
    public ResponseEntity<List<String>> getAllTags() {
        List<String> tags = materialService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @PostMapping("/materials/{id}/use")
    public ResponseEntity<Map<String, String>> useMaterial(@PathVariable String id) {
        materialService.useMaterial(id);
        Map<String, String> response = new HashMap<>();
        response.put("message", "素材使用成功");
        return ResponseEntity.ok(response);
    }

    // ==================== 组别相关API ====================

    @PostMapping("/groups")
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        log.info("创建组别: {}", group.getName());
        Group created = groupService.createGroup(group);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<Group>> getAllGroups(
            @RequestParam(required = false) String ownerId,
            @RequestParam(required = false) String memberId) {
        
        List<Group> groups;
        
        if (ownerId != null && !ownerId.isEmpty()) {
            groups = groupService.getGroupsByOwner(ownerId);
        } else if (memberId != null && !memberId.isEmpty()) {
            groups = groupService.getGroupsByMember(memberId);
        } else {
            groups = groupService.getAllGroups();
        }
        
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<?> getGroup(@PathVariable String id) {
        return groupService.getGroup(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<?> updateGroup(
            @PathVariable String id,
            @RequestBody Group update) {
        try {
            Group updated = groupService.updateGroup(id, update);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<?> deleteGroup(@PathVariable String id) {
        if (groupService.deleteGroup(id)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/groups/{id}/members")
    public ResponseEntity<Group> addMember(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        String userId = request.get("userId");
        Group updated = groupService.addMember(id, userId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/groups/{id}/members/{userId}")
    public ResponseEntity<Group> removeMember(
            @PathVariable String id,
            @PathVariable String userId) {
        Group updated = groupService.removeMember(id, userId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/groups/{id}/materials")
    public ResponseEntity<List<Material>> getGroupMaterials(@PathVariable String id) {
        List<Material> materials = materialService.getMaterialsByGroup(id);
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/users/{userId}/materials")
    public ResponseEntity<List<Material>> getUserMaterials(@PathVariable String userId) {
        List<Material> materials = materialService.getMaterialsByUser(userId);
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/users/{userId}/groups")
    public ResponseEntity<List<Group>> getUserGroups(@PathVariable String userId) {
        List<Group> groups = groupService.getGroupsByMember(userId);
        return ResponseEntity.ok(groups);
    }
}