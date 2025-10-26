package ru.practicum.ewm.main.repository;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.main.model.Event;

import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByInitiatorIdOrderByCreatedOnDesc(long userId, PageRequest pageable);

    Optional<Event> findByIdAndInitiatorId(long eventId, long userId);
}
