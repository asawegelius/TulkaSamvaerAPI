package com.tulka.samvaer.sections.events.infrastructure;

import java.util.UUID;

import com.tulka.samvaer.sections.events.domain.Series;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeriesRepository extends JpaRepository<Series, UUID> {
}
