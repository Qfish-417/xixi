package com.xixi.manjuagent.library;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {

    private String id;
    private String name;
    private String description;
    private String owner;
    @Builder.Default
    private List<String> members = new ArrayList<>();
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public void addMember(String userId) {
        if (!members.contains(userId)) {
            members.add(userId);
        }
    }

    public void removeMember(String userId) {
        members.remove(userId);
    }

    public boolean hasMember(String userId) {
        return members.contains(userId);
    }

    public boolean isOwner(String userId) {
        return owner.equals(userId);
    }
}