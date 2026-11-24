package com.tulka.samvaer.sections.events.infrastructure;

import java.time.Instant;
import java.util.Collection;
import java.util.UUID;

import com.tulka.samvaer.sections.events.domain.Event;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventRepository extends JpaRepository<Event, UUID> {

    /**
     * Events {@code viewer} may see that overlap [from, to). Must match {@link Event#isVisibleTo}.
     * {@code viewerGroupIds} must not be empty; pass a placeholder id when the viewer has no groups.
     */
    @Query(value = """
            select e from Event e
            where e.cancelledAt is null
              and e.endsAt >= :from
              and e.startsAt < :to
              and (e.hostId = :viewer
                   or e.visibilityScope = com.tulka.samvaer.platform.visibility.VisibilityScope.ALL_MEMBERS
                   or (e.visibilityScope = com.tulka.samvaer.platform.visibility.VisibilityScope.GROUPS
                       and (exists (select 1 from Answer a where a.event = e and a.memberId = :viewer)
                            or exists (select 1 from Event e2 join e2.visibilityGroupIds g
                                       where e2 = e and g in :viewerGroupIds))))
            order by e.startsAt, e.id
            """,
            countQuery = """
            select count(e) from Event e
            where e.cancelledAt is null
              and e.endsAt >= :from
              and e.startsAt < :to
              and (e.hostId = :viewer
                   or e.visibilityScope = com.tulka.samvaer.platform.visibility.VisibilityScope.ALL_MEMBERS
                   or (e.visibilityScope = com.tulka.samvaer.platform.visibility.VisibilityScope.GROUPS
                       and (exists (select 1 from Answer a where a.event = e and a.memberId = :viewer)
                            or exists (select 1 from Event e2 join e2.visibilityGroupIds g
                                       where e2 = e and g in :viewerGroupIds))))
            """)
    Page<Event> findVisible(@Param("viewer") UUID viewer, @Param("viewerGroupIds") Collection<UUID> viewerGroupIds,
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);
}
