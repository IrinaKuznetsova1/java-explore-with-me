package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.practicum.ewm.main.model.ConfirmedRequestsCount;
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
    long getCountConfirmedRequestsByEventId(long eventId);


    @Query("""
            SELECT new ru.practicum.ewm.main.model.ConfirmedRequestsCount(r.event.id, COUNT(r))
            FROM Request r
            WHERE r.event.id IN :eventsIds
            AND r.status = 'CONFIRMED'
            GROUP BY r.event.id
            """)
    List<ConfirmedRequestsCount> getCountConfirmedRequests(List<Long> eventsIds);

    List<Request> findAllByEventId(long eventId);

    List<Request> findAllByIdInAndEventId(List<Long> ids, long eventId);

    Optional<Request> findByEventIdAndRequesterId(long eventId, long requesterId);

    List<Request> findByRequesterId(long userId);
}
