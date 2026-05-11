package com.xixi.manjuagent.library;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class GroupRepository {

    private final Map<String, Group> groups = new ConcurrentHashMap<>();

    public Group save(Group group) {
        if (group.getId() == null || group.getId().isEmpty()) {
            group.setId(UUID.randomUUID().toString());
        }
        
        if (group.getCreatedAt() == null) {
            group.setCreatedAt(LocalDateTime.now());
        }
        
        groups.put(group.getId(), group);
        log.info("保存组别: {}", group.getName());
        return group;
    }

    public Optional<Group> findById(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    public List<Group> findAll() {
        return new ArrayList<>(groups.values());
    }

    public List<Group> findByOwner(String ownerId) {
        return groups.values().stream()
                .filter(g -> g.getOwner().equals(ownerId))
                .collect(Collectors.toList());
    }

    public List<Group> findByMember(String memberId) {
        return groups.values().stream()
                .filter(g -> g.hasMember(memberId) || g.getOwner().equals(memberId))
                .collect(Collectors.toList());
    }

    public boolean deleteById(String id) {
        Group removed = groups.remove(id);
        if (removed != null) {
            log.info("删除组别: {}", removed.getName());
            return true;
        }
        return false;
    }

    public boolean existsById(String id) {
        return groups.containsKey(id);
    }

    public long count() {
        return groups.size();
    }
}