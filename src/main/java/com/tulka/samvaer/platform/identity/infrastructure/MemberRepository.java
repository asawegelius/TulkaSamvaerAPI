package com.tulka.samvaer.platform.identity.infrastructure;

import java.util.UUID;

import com.tulka.samvaer.platform.identity.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, UUID> {
}
