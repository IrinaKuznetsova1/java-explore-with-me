package ru.practicum.ewm.stats.server.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.stats.dto.ViewStats;
import ru.practicum.ewm.stats.server.model.EndpointHit;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsRepository extends JpaRepository<EndpointHit, Long> {
    @Query("""
            SELECT new ru.practicum.ewm.stats.dto.ViewStats(e.app, e.uri, COUNT(DISTINCT e.ip))
            FROM EndpointHit e
            WHERE (:uris is NULL OR e.uri IN :uris)
            AND e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(DISTINCT e.ip) DESC
            """)
    List<ViewStats> findUniqueViewStats(LocalDateTime start, LocalDateTime end, List<String> uris);

    @Query("""
            SELECT new ru.practicum.ewm.stats.dto.ViewStats(e.app, e.uri, COUNT(e.ip))
            FROM EndpointHit e
            WHERE (:uris is NULL OR e.uri IN :uris)
            AND e.timestamp BETWEEN :start AND :end
            GROUP BY e.app, e.uri
            ORDER BY COUNT(e.ip) DESC
            """)
    List<ViewStats> findAllViewStats(LocalDateTime start, LocalDateTime end, List<String> uris);
}
