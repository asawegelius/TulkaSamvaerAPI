package com.tulka.samvaer.platform.groups.api;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import com.tulka.samvaer.platform.groups.application.GroupService;
import com.tulka.samvaer.platform.identity.CurrentMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Platform")
class MyGroupsController {

    private final CurrentMember currentMember;
    private final GroupService groups;

    MyGroupsController(CurrentMember currentMember, GroupService groups) {
        this.currentMember = currentMember;
        this.groups = groups;
    }

    @GetMapping("/api/v1/me/groups")
    @Operation(operationId = "listMyGroups", summary = "Groups the current member can share with or invite")
    List<GroupResponse> myGroups() {
        UUID me = currentMember.id();
        return groups.groupsOf(me).stream()
                .map(g -> {
                    var everyone = new HashSet<>(g.getMemberIds());
                    everyone.add(g.getOwnerId());
                    return new GroupResponse(g.getId(), g.getName(), everyone.size(), g.getOwnerId().equals(me));
                })
                .toList();
    }
}
