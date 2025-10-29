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
import java.util.Set;

public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {

    List<Event> findByInitiatorIdOrderByCreatedOnDesc(long userId, PageRequest pageable);

    Optional<Event> findByIdAndInitiatorId(long eventId, long userId);

    List<Event> findByIdIn(Set<Long> eventsIds);
}
