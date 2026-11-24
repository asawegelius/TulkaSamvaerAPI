package com.tulka.samvaer.platform.identity.application;

import java.text.Collator;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.tulka.samvaer.platform.identity.MemberDirectory;
import com.tulka.samvaer.platform.identity.MemberRef;
import com.tulka.samvaer.platform.identity.domain.Member;
import com.tulka.samvaer.platform.identity.infrastructure.MemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService implements MemberDirectory {

    /** Danish collation, so Æ, Ø and Å sort after Z. */
    private static final Collator NAME_ORDER = Collator.getInstance(Locale.forLanguageTag("da-DK"));

    private final MemberRepository members;

    public MemberService(MemberRepository members) {
        this.members = members;
    }

    @Override
    public Map<UUID, MemberRef> refsOf(Collection<UUID> memberIds) {
        Set<UUID> ids = new HashSet<>(memberIds);
        Map<UUID, MemberRef> found = members.findAllById(ids).stream()
                .collect(Collectors.toMap(Member::getId, Member::toRef));
        return ids.stream().collect(Collectors.toMap(Function.identity(),
                id -> found.getOrDefault(id, MemberRef.former(id))));
    }

    @Override
    public Set<UUID> existing(Collection<UUID> memberIds) {
        return members.findAllById(new HashSet<>(memberIds)).stream().map(Member::getId).collect(Collectors.toSet());
    }

    public java.util.Optional<Member> find(UUID id) {
        return members.findById(id);
    }

    /** All members whose name contains {@code query} (case-insensitive), sorted by name. */
    public List<MemberRef> search(String query) {
        String q = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
        return members.findAll().stream()
                .filter(m -> m.getDisplayName().toLowerCase(Locale.ROOT).contains(q))
                .sorted(Comparator.comparing(Member::getDisplayName, NAME_ORDER))
                .map(Member::toRef)
                .toList();
    }
}
