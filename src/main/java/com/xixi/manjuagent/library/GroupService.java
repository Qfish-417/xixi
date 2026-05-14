package com.xixi.manjuagent.library;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class GroupService {

    private final GroupRepository groupRepository;

    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
        initializeDefaultGroups();
    }

    private void initializeDefaultGroups() {
        if (groupRepository.count() == 0) {
            log.info("初始化默认组别...");

            Group defaultGroup = Group.builder()
                    .name("默认素材库")
                    .description("系统默认素材库，所有用户均可访问")
                    .owner("system")
                    .members(List.of("user1", "user2", "user3"))
                    .build();
            groupRepository.save(defaultGroup);

            Group demoGroup = Group.builder()
                    .name("演示组")
                    .description("演示用素材组")
                    .owner("admin")
                    .members(List.of("user1"))
                    .build();
            groupRepository.save(demoGroup);

            log.info("默认组别初始化完成，共 {} 个组别", groupRepository.count());
        }
    }

    public Group createGroup(Group group) {
        log.info("创建组别: {}", group.getName());
        return groupRepository.save(group);
    }

    public Optional<Group> getGroup(String id) {
        return groupRepository.findById(id);
    }

    public List<Group> getAllGroups() {
        return groupRepository.findAll();
    }

    public List<Group> getGroupsByOwner(String ownerId) {
        return groupRepository.findByOwner(ownerId);
    }

    public List<Group> getGroupsByMember(String memberId) {
        return groupRepository.findByMember(memberId);
    }

    public Group updateGroup(String id, Group update) {
        return groupRepository.findById(id)
                .map(existing -> {
                    if (update.getName() != null) {
                        existing.setName(update.getName());
                    }
                    if (update.getDescription() != null) {
                        existing.setDescription(update.getDescription());
                    }
                    return groupRepository.save(existing);
                })
                .orElseThrow(() -> new IllegalArgumentException("组别不存在: " + id));
    }

    public boolean deleteGroup(String id) {
        return groupRepository.deleteById(id);
    }

    public Group addMember(String groupId, String userId) {
        return groupRepository.findById(groupId)
                .map(group -> {
                    group.addMember(userId);
                    return groupRepository.save(group);
                })
                .orElseThrow(() -> new IllegalArgumentException("组别不存在: " + groupId));
    }

    public Group removeMember(String groupId, String userId) {
        return groupRepository.findById(groupId)
                .map(group -> {
                    group.removeMember(userId);
                    return groupRepository.save(group);
                })
                .orElseThrow(() -> new IllegalArgumentException("组别不存在: " + groupId));
    }

    public boolean isMember(String groupId, String userId) {
        return groupRepository.findById(groupId)
                .map(group -> group.hasMember(userId) || group.isOwner(userId))
                .orElse(false);
    }

    public long count() {
        return groupRepository.count();
    }
}