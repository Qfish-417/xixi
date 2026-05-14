package com.xixi.manjuagent.library;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LibraryMaterialService {

    private final MaterialRepository repository;

    public LibraryMaterialService(MaterialRepository repository) {
        this.repository = repository;
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
                    .tags(List.of("少女", "樱花", "校园"))
                    .build();

            Material cyberpunk = Material.builder()
                    .name("赛博朋克战士")
                    .imageUrl("https://example.com/materials/cyberpunk-warrior.png")
                    .description("未来风格的机械战士，适合科幻题材")
                    .category("character")
                    .tags(List.of("赛博朋克", "战士", "科幻"))
                    .build();

            repository.save(sakura);
            repository.save(cyberpunk);
            log.info("默认素材库初始化完成");
        }
    }

    public List<Material> getAllMaterials() {
        return repository.findAll();
    }

    public Optional<Material> getMaterialById(String id) {
        return repository.findById(id);
    }

    public List<Material> getMaterialsByCategory(String category) {
        return repository.findByCategory(category);
    }

    public List<Material> searchMaterials(String keyword) {
        return repository.search(keyword);
    }

    public Material saveMaterial(Material material) {
        return repository.save(material);
    }

    public void deleteMaterial(String id) {
        repository.deleteById(id);
    }

    public long count() {
        return repository.count();
    }

    public List<Material> getMaterialsByGroupId(String groupId) {
        return repository.findByGroupId(groupId);
    }
}