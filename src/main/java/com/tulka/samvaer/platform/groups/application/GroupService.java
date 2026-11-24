package com.tulka.samvaer.platform.groups.application;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.tulka.samvaer.platform.groups.GroupDirectory;
import com.tulka.samvaer.platform.groups.domain.Group;
import com.tulka.samvaer.platform.groups.infrastructure.GroupRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class GroupService implements GroupDirectory {

    private static final Collator NAME_ORDER = Collator.getInstance(Locale.forLanguageTag("da-DK"));

    private final GroupRepository groups;

    public GroupService(GroupRepository groups) {
        this.groups = groups;
    }

    /** The member's groups, sorted by name. */
    public List<Group> groupsOf(UUID memberId) {
        return groups.findAllIncluding(memberId).stream()
                .sorted(Comparator.comparing(Group::getName, NAME_ORDER))
                .toList();
    }

    @Override
    public Set<UUID> groupIdsOf(UUID memberId) {
        return groups.findAllIncluding(memberId).stream().map(Group::getId).collect(Collectors.toSet());
    }

    @Override
    public Set<UUID> memberIdsOf(Collection<UUID> groupIds) {
        Set<UUID> result = new HashSet<>();
        for (Group group : groups.findAllById(new HashSet<>(groupIds))) {
            result.add(group.getOwnerId());
            result.addAll(group.getMemberIds());
        }
        return result;
    }

    @Override
    public List<String> namesOf(List<UUID> groupIds) {
        Map<UUID, String> names = nameMap(groupIds);
        return groupIds.stream().map(names::get).filter(Objects::nonNull).toList();
    }

    @Override
    public Map<UUID, String> nameMap(Collection<UUID> groupIds) {
        return groups.findAllById(new HashSet<>(groupIds)).stream()
                .collect(Collectors.toMap(Group::getId, Group::getName));
    }
}
