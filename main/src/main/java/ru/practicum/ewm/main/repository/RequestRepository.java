package ru.practicum.ewm.main.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.ewm.main.model.Request;

import java.util.List;
import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findAllByEventId(long eventId);

    List<Request> findAllByIdInAndEventId(List<Long> ids, long eventId);

    Optional<Request> findByEventIdAndRequesterId(long eventId, long requesterId);

    List<Request> findByRequesterId(long userId);
}
