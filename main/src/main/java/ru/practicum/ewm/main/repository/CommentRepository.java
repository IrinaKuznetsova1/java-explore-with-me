package ru.practicum.ewm.main.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import ru.practicum.ewm.main.enums.CommentState;
import ru.practicum.ewm.main.model.Comment;

import java.util.List;
import java.util.Optional;

public interface CommentRepository extends JpaRepository<Comment, Long>, QuerydslPredicateExecutor<Comment> {
    long countByEventIdAndState(long eventId, CommentState state);

    Optional<Comment> findByIdAndAuthorId(long comId, long userId);

    List<Comment> findByAuthorId(long authorId, Pageable pageable);

    List<Comment> findByEventIdAndState(long eventId, CommentState state, Pageable pageable);

    Optional<Comment> findByIdAndEventId(long comId, long eventId);
}
