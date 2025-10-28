package ru.practicum.ewm.main.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.model.Event;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    List<Event> findByInitiatorIdOrderByCreatedOnDesc(long userId, PageRequest pageable);

    Optional<Event> findByIdAndInitiatorId(long eventId, long userId);

    @Query("""
            SELECT e
            FROM Event e
            WHERE (:users is NULL OR e.initiator.id IN :users)
            AND (:states is NULL OR e.state IN :states)
            AND (:categories is NULL OR e.category.id IN :categories)
            AND (COALESCE(:rangeStart, NULL) is NULL OR e.eventDate >= :rangeStart)
            AND (COALESCE(:rangeEnd, NULL) is NULL OR e.eventDate <= :rangeEnd)
            """)
    List<Event> findEventsByAdminsFilters(
            List<Long> users,
            List<EventsState> states,
            List<Integer> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Pageable pageable);
}
