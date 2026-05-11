package com.xixi.manjuagent.library;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class MaterialRepository {

    private final Map<String, Material> materials = new ConcurrentHashMap<>();

    public Material save(Material material) {
        if (material.getId() == null || material.getId().isEmpty()) {
            material.setId(UUID.randomUUID().toString());
        }
        material.setUpdatedAt(LocalDateTime.now());
        
        if (material.getCreatedAt() == null) {
            material.setCreatedAt(LocalDateTime.now());
        }
        
        materials.put(material.getId(), material);
        log.info("保存素材: {}", material.getName());
        return material;
    }

    public Optional<Material> findById(String id) {
        return Optional.ofNullable(materials.get(id));
    }

    public List<Material> findAll() {
        return new ArrayList<>(materials.values());
    }

    public List<Material> findByCategory(String category) {
        return materials.values().stream()
                .filter(m -> m.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }

    public List<Material> findByType(String type) {
        return materials.values().stream()
                .filter(m -> m.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public List<Material> findByTag(String tag) {
        return materials.values().stream()
                .filter(m -> m.getTags() != null && m.getTags().contains(tag))
                .collect(Collectors.toList());
    }

    public List<Material> findByGroupId(String groupId) {
        return materials.values().stream()
                .filter(m -> groupId.equals(m.getGroupId()))
                .collect(Collectors.toList());
    }

    public List<Material> findByGroupIds(List<String> groupIds) {
        return materials.values().stream()
                .filter(m -> m.getGroupId() != null && groupIds.contains(m.getGroupId()))
                .collect(Collectors.toList());
    }

    public List<Material> search(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return materials.values().stream()
                .filter(m -> 
                    (m.getName() != null && m.getName().toLowerCase().contains(lowerKeyword)) ||
                    (m.getDescription() != null && m.getDescription().toLowerCase().contains(lowerKeyword)) ||
                    (m.getTags() != null && m.getTags().stream().anyMatch(t -> t.toLowerCase().contains(lowerKeyword)))
                )
                .collect(Collectors.toList());
    }

    public boolean deleteById(String id) {
        Material removed = materials.remove(id);
        if (removed != null) {
            log.info("删除素材: {}", removed.getName());
            return true;
        }
        return false;
    }

    public boolean existsById(String id) {
        return materials.containsKey(id);
    }

    public long count() {
        return materials.size();
    }

    public List<String> getAllCategories() {
        return materials.values().stream()
                .map(Material::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }

    public List<String> getAllTags() {
        return materials.values().stream()
                .filter(m -> m.getTags() != null && !m.getTags().isEmpty())
                .flatMap(m -> m.getTags().stream())
                .distinct()
                .collect(Collectors.toList());
    }

    public void incrementUsageCount(String id) {
        Material material = materials.get(id);
        if (material != null) {
            material.setUsageCount(material.getUsageCount() + 1);
            material.setUpdatedAt(LocalDateTime.now());
        }
    }
}