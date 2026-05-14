package com.xixi.manjuagent.controller;

import com.xixi.manjuagent.library.Group;
import com.xixi.manjuagent.library.GroupService;
import com.xixi.manjuagent.library.LibraryMaterialService;
import com.xixi.manjuagent.library.Material;
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

    private final LibraryMaterialService materialService;
    private final GroupService groupService;

    public LibraryController(LibraryMaterialService materialService, GroupService groupService) {
        this.materialService = materialService;
        this.groupService = groupService;
    }

    @PostMapping("/materials")
    public ResponseEntity<Material> createMaterial(@RequestBody Material material) {
        log.info("创建素材: {}", material.getName());
        Material created = materialService.saveMaterial(material);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/groups/{groupId}/materials")
    public ResponseEntity<Material> createMaterialInGroup(
            @PathVariable String groupId,
            @RequestBody Material material) {
        log.info("在组 {} 中创建素材: {}", groupId, material.getName());
        material.setGroupId(groupId);
        Material created = materialService.saveMaterial(material);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/materials")
    public ResponseEntity<List<Material>> getAllMaterials() {
        List<Material> materials = materialService.getAllMaterials();
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/materials/{id}")
    public ResponseEntity<Material> getMaterialById(@PathVariable String id) {
        return materialService.getMaterialById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/materials/category/{category}")
    public ResponseEntity<List<Material>> getMaterialsByCategory(@PathVariable String category) {
        List<Material> materials = materialService.getMaterialsByCategory(category);
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/materials/search")
    public ResponseEntity<List<Material>> searchMaterials(@RequestParam String keyword) {
        List<Material> materials = materialService.searchMaterials(keyword);
        return ResponseEntity.ok(materials);
    }

    @PutMapping("/materials/{id}")
    public ResponseEntity<Material> updateMaterial(@PathVariable String id, @RequestBody Material material) {
        log.info("更新素材 {}: {}", id, material.getName());
        return materialService.getMaterialById(id)
                .map(existing -> {
                    material.setId(id);
                    Material updated = materialService.saveMaterial(material);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/materials/{id}")
    public ResponseEntity<Void> deleteMaterial(@PathVariable String id) {
        log.info("删除素材: {}", id);
        materialService.deleteMaterial(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/groups")
    public ResponseEntity<Group> createGroup(@RequestBody Group group) {
        log.info("创建组别: {}", group.getName());
        Group created = groupService.createGroup(group);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/groups")
    public ResponseEntity<List<Group>> getAllGroups() {
        List<Group> groups = groupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/groups/{id}")
    public ResponseEntity<Group> getGroupById(@PathVariable String id) {
        return groupService.getGroup(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/groups/{id}")
    public ResponseEntity<Group> updateGroup(@PathVariable String id, @RequestBody Group group) {
        log.info("更新组别 {}: {}", id, group.getName());
        try {
            Group updated = groupService.updateGroup(id, group);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/groups/{id}")
    public ResponseEntity<Void> deleteGroup(@PathVariable String id) {
        log.info("删除组别: {}", id);
        groupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/groups/{id}/materials")
    public ResponseEntity<List<Material>> getMaterialsInGroup(@PathVariable String id) {
        List<Material> materials = materialService.getMaterialsByGroupId(id);
        return ResponseEntity.ok(materials);
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalMaterials", materialService.count());
        stats.put("totalGroups", groupService.count());
        return ResponseEntity.ok(stats);
    }
}