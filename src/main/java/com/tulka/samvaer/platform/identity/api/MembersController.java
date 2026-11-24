package com.tulka.samvaer.platform.identity.api;

import java.util.List;

import com.tulka.samvaer.platform.identity.CurrentMember;
import com.tulka.samvaer.platform.identity.MemberRef;
import com.tulka.samvaer.platform.identity.application.MemberService;
import com.tulka.samvaer.shared.error.ApiException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Platform")
class MembersController {

    private final CurrentMember currentMember;
    private final MemberService members;

    MembersController(CurrentMember currentMember, MemberService members) {
        this.currentMember = currentMember;
        this.members = members;
    }

    @GetMapping("/me")
    @Operation(operationId = "getMe", summary = "The signed-in member")
    MeResponse me() {
        var me = members.find(currentMember.id()).orElseThrow(ApiException::notLoggedIn);
        var ref = me.toRef();
        return new MeResponse(ref.id(), ref.displayName(), ref.initials(), ref.avatarMediaId(),
                me.getRoles().stream().sorted().toList());
    }

    @GetMapping("/members")
    @Operation(operationId = "listMembers", summary = "All members, for inviting individuals")
    List<MemberRef> list(@RequestParam(required = false) String q) {
        currentMember.id();
        return members.search(q);
    }
}
