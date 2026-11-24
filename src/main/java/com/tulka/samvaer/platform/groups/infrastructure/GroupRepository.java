package com.tulka.samvaer.platform.groups.infrastructure;

import java.util.List;
import java.util.UUID;

import com.tulka.samvaer.platform.groups.domain.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GroupRepository extends JpaRepository<Group, UUID> {

    @Query("select distinct g from Group g left join g.memberIds m where g.ownerId = :memberId or m = :memberId")
    List<Group> findAllIncluding(@Param("memberId") UUID memberId);
}
