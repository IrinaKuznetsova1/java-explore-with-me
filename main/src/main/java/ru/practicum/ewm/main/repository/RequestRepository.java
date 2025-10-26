package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.main.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("""
            SELECT COUNT(r)
            FROM Request r
            WHERE r.event.id = :eventId
            AND r.status = 'CONFIRMED'
            """)
    Long getCountConfirmedRequestsByEventId(long eventId);

    List<Request> findAllByEventId(long eventId);

    List<Request> findAllByEventIdAndRequesterIdIn(long eventId, List<Long> ids);

    Optional<Request> findByEventIdAndRequesterId(long eventId, long requesterId);

    List<Request> findByRequesterId(long userId);
}
