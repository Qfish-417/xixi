package com.xixi.manjuagent.library;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class MaterialService {

    private final MaterialRepository repository;
    private final GroupRepository groupRepository;

    public MaterialService(MaterialRepository repository, GroupRepository groupRepository) {
        this.repository = repository;
        this.groupRepository = groupRepository;
        initializeDefaultMaterials();
    }

    private void initializeDefaultMaterials() {
        if (repository.count() == 0) {
            log.info("初始化默认素材库...");
            
            Material sakura = Material.builder()
                    .name("樱花少女")
                    .imageUrl("https://example.com/materials/sakura-girl.png")
                    .description("粉色头发的可爱少女角色，适合校园题材")
                    .category("character")
                    .tags(List.of("少女", "校园", "粉色", "可爱"))
                    .type("image")
                    .build();
            repository.save(sakura);

            Material classroom = Material.builder()
                    .name("教室背景")
                    .imageUrl("https://example.com/materials/classroom.png")
                    .description("日系动漫风格教室场景")
                    .category("background")
                    .tags(List.of("教室", "校园", "室内"))
                    .type("image")
                    .build();
            repository.save(classroom);

            Material magicWand = Material.builder()
                    .name("魔法棒")
                    .imageUrl("https://example.com/materials/magic-wand.png")
                    .description("闪耀的魔法道具")
                    .category("prop")
                    .tags(List.of("魔法", "道具", "奇幻"))
                    .type("image")
                    .build();
            repository.save(magicWand);

            Material sparkles = Material.builder()
                    .name("星光特效")
                    .imageUrl("https://example.com/materials/sparkles.png")
                    .description("闪烁的星光特效")
                    .category("effect")
                    .tags(List.of("特效", "星光", "华丽"))
                    .type("image")
                    .build();
            repository.save(sparkles);

            Material loveStory = Material.builder()
                    .name("恋爱剧本模板")
                    .imageUrl("https://example.com/materials/love-script.png")
                    .description("校园恋爱故事剧本模板")
                    .category("script")
                    .tags(List.of("剧本", "恋爱", "校园"))
                    .type("text")
                    .build();
            repository.save(loveStory);

            log.info("默认素材库初始化完成，共 {} 个素材", repository.count());
        }
    }

    public Material createMaterial(Material material) {
        log.info("创建素材: {}", material.getName());
        return repository.save(material);
    }

    public Optional<Material> getMaterial(String id) {
        return repository.findById(id);
    }

    public List<Material> getAllMaterials() {
        return repository.findAll();
    }

    public List<Material> getMaterialsByCategory(String category) {
        return repository.findByCategory(category);
    }

    public List<Material> getMaterialsByType(String type) {
        return repository.findByType(type);
    }

    public List<Material> searchMaterials(String keyword) {
        return repository.search(keyword);
    }

    public List<Material> getMaterialsByGroup(String groupId) {
        return repository.findByGroupId(groupId);
    }

    public List<Material> getMaterialsByUser(String userId) {
        List<String> userGroups = groupRepository.findByMember(userId).stream()
                .map(Group::getId)
                .toList();
        return repository.findByGroupIds(userGroups);
    }

    public Material createMaterial(String groupId, Material material) {
        material.setGroupId(groupId);
        log.info("创建素材到组别 {}: {}", groupId, material.getName());
        return repository.save(material);
    }

    public Material updateMaterial(String id, Material update) {
        return repository.findById(id)
                .map(existing -> {
                    if (update.getName() != null) {
                        existing.setName(update.getName());
                    }
                    if (update.getImageUrl() != null) {
                        existing.setImageUrl(update.getImageUrl());
                    }
                    if (update.getDescription() != null) {
                        existing.setDescription(update.getDescription());
                    }
                    if (update.getCategory() != null) {
                        existing.setCategory(update.getCategory());
                    }
                    if (update.getTags() != null) {
                        existing.setTags(update.getTags());
                    }
                    if (update.getType() != null) {
                        existing.setType(update.getType());
                    }
                    existing.setUpdatedAt(java.time.LocalDateTime.now());
                    return repository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("素材不存在: " + id));
    }

    public boolean deleteMaterial(String id) {
        return repository.deleteById(id);
    }

    public List<String> getAllCategories() {
        return repository.getAllCategories();
    }

    public List<String> getAllTags() {
        return repository.getAllTags();
    }

    public void useMaterial(String id) {
        repository.incrementUsageCount(id);
    }
}