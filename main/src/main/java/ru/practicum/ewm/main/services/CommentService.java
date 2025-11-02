package ru.practicum.ewm.main.services;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.newRequests.NewComment;
import ru.practicum.ewm.main.dto.responses.CommentDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentAdminRequest;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCommentUserRequest;
import ru.practicum.ewm.main.enums.CommentSort;
import ru.practicum.ewm.main.enums.CommentState;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.exceptions.ConflictException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.exceptions.ValidationException;
import ru.practicum.ewm.main.mapper.CommentMapper;
import ru.practicum.ewm.main.model.*;
import ru.practicum.ewm.main.repository.CommentRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CommentService {
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CommentMapper commentMapper;

    public CommentDto createComment(long authorId, long eventId, NewComment newComment) {
        User author = validateUserExisted(authorId);
        Event event = validateEventExisted(eventId);
        if (event.getState() != EventsState.PUBLISHED) {
            log.warn("Выброшено ConflictException: комментировать можно только опублкованные события.");
            throw new ConflictException("Комментировать можно только опублкованные события.", "Для запрошенной операции условия не выполнены.");
        }
        if (!event.isAllowComments()) {
            log.warn("Выброшено ConflictException: комментирование события с id: {} запрещено.", eventId);
            throw new ConflictException("Комментирование события с id: " + eventId + " запрещено.", "Для запрошенной операции условия не выполнены.");
        }

        Comment comment = commentRepository.save(commentMapper.toComment(newComment, event, author));
        log.info("Новый комментарий сохранен с id: {}", comment.getId());
        return commentMapper.toCommentDto(comment);
    }

    public CommentDto updateCommentByUser(long authorId, long comId, UpdateCommentUserRequest request) {
        Comment comment = validateCommentExistedByUserId(comId, authorId);
        Comment updComment = commentRepository.save(commentMapper.updateCommentUser(request, comment));
        log.info("Комментарий с id: {} обновлен.", updComment.getId());
        return commentMapper.toCommentDto(updComment);
    }

    public CommentDto updateCommentByAdmin(long comId, UpdateCommentAdminRequest request) {
        Comment comment = validateCommentExisted(comId);
        Comment updComment = commentRepository.save(commentMapper.updateCommentAdmin(request, comment));
        log.info("Комментарий с id:{} обновлен.", updComment.getId());
        return commentMapper.toCommentDto(updComment);
    }

    public void deleteComment(long authorId, long comId) {
        validateCommentExistedByUserId(comId, authorId);
        commentRepository.deleteById(comId);
        log.info("Комментарий с id: {} удален.", comId);
    }

    @Transactional(readOnly = true)
    public CommentDto findCommentByIdUser(long authorId, long comId) {
        Comment comment = validateCommentExistedByUserId(comId, authorId);
        log.info("Комментарий с id: {} найден", comment.getId());
        return commentMapper.toCommentDto(comment);
    }

    @Transactional(readOnly = true)
    public CommentDto findCommentByIdAdmin(long comId) {
        Comment comment = validateCommentExisted(comId);
        log.info("Комментарий с id: {} найден.", comment.getId());
        return commentMapper.toCommentDto(comment);
    }

    @Transactional(readOnly = true)
    public CommentDto findCommentByIdPublic(long eventId, long comId) {
        validateEventExisted(eventId);
        Comment comment = commentRepository.findByIdAndEventId(comId, eventId)
                        .orElseThrow(() -> new NotFoundException(
                                "Комментарий с id: " + comId + " события с id: " + eventId + " не найден.",
                                "Искомый объект не был найден."));
        if (comment.getState() != CommentState.PUBLISHED) {
            log.warn("Выброшено ConflictException: запрашиваемый комментарий не был опубликован.");
            throw new ConflictException(
                    "Запрашиваемый комментарий не был опубликован.",
                    "Для запрошенной операции условия не выполнены.");
        }
        log.info("Комментарий  с id: {} найден.", comment.getId());
        return commentMapper.toCommentDto(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> findCommentsByAuthor(long authorId, int from, int size) {
        validateUserExisted(authorId);
        final Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));
        List<Comment> comments = commentRepository.findByAuthorId(authorId, pageable);
        log.info("Комментарии найдены в количестве: {}.", comments.size());
        return commentMapper.toCommentDtoList(comments);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> findCommentsByAdmin(List<Long> users,
                                                List<CommentState> states,
                                                List<Long> events,
                                                LocalDateTime rangeStart,
                                                LocalDateTime rangeEnd,
                                                int from,
                                                int size) {
        if (rangeStart != null && rangeEnd != null && rangeEnd.isBefore(rangeStart)) {
            log.info("Выброшено ValidationException: дата start должна быть раньше даты end.");
            throw new ValidationException("Дата start должна быть раньше даты end.", "Для запрошенной операции условия не выполнены");
        }
        final Pageable pageable = PageRequest.of(from / size, size, Sort.by(Sort.Direction.DESC, "created"));
        Predicate predicate = getPredicateForAdminSearch(users, states, events, rangeStart, rangeEnd);

        List<Comment> comments = commentRepository.findAll(predicate, pageable).getContent();
        if (comments.isEmpty()) {
            log.info("Комментарии по заданным параметрам не найдены.");
            return List.of();
        }
        log.info("Запрашиваемые события найдены в количестве: {}.", comments.size());
        return commentMapper.toCommentDtoList(comments);
    }

    @Transactional(readOnly = true)
    public List<CommentDto> findCommentsPublic(long eventId,
                                               int from,
                                               int size,
                                               CommentSort sort) {
        validateEventExisted(eventId);
        final Pageable pageable = PageRequest.of(from / size, size,
                Sort.by(Sort.Direction.DESC, "created"));
        List<Comment> comments = commentRepository.findByEventIdAndState(eventId, CommentState.PUBLISHED, pageable);
        List<CommentDto> commentDtoList = commentMapper.toCommentDtoList(comments);
        if (sort == CommentSort.USEFUL) {
            commentDtoList = commentDtoList
                    .stream()
                    .sorted(Comparator.comparing(CommentDto::getUseful).reversed())
                    .toList();
            log.info("Комментарии найдены в количестве: {}", commentDtoList.size());
            return commentDtoList;
        }
        log.info("Комментарии найдены в количестве: {}", commentDtoList.size());
        return commentDtoList;
    }

    public CommentDto addLikeToComment(long userId, long comId) {
        User user = validateUserExisted(userId);
        Comment comment = validateCommentExisted(comId);
        if (comment.getAuthor().getId() == userId) {
            log.warn("Выброшено ConflictException: нельзя добавить лайк своему комментарию.");
            throw new ConflictException("Нельзя добавить лайк своему комментарию.", "Для запрошенной операции условия не выполнены.");
        }
        if (comment.getState() != CommentState.PUBLISHED) {
            log.warn("Выброшено ConflictException: нельзя добавить лайк неопубликованному комментарию.");
            throw new ConflictException("Нельзя добавить лайк неопубликованному комментарию.", "Для запрошенной операции условия не выполнены.");
        }
        if (comment.getLikes().contains(user)) {
            log.warn("Выброшено ConflictException: лайк уже добавлен.");
            throw new ConflictException("Лайк пользователя с id: " + userId + "к комментарию с id: " + comId + " уже добавлен.",
                    "Пользователь может добавить только один лайк.");
        }
        comment.getLikes().add(user);
        comment.getDislikes().remove(user);
        Comment updatedComment = commentRepository.save(comment);
        log.info("Лайк сохранен, новое количество лайков: {}", updatedComment.getLikes().size());
        return commentMapper.toCommentDto(updatedComment);
    }

    public CommentDto deleteLikeFromComment(long userId, long comId) {
        User user = validateUserExisted(userId);
        Comment comment = validateCommentExisted(comId);
        if (comment.getAuthor().getId() == userId) {
            log.warn("Выброшено ConflictException: нельзя удалить лайк у своего комментария.");
            throw new ConflictException("Нельзя удалить лайк у своего комментария.", "Для запрошенной операции условия не выполнены.");
        }
        if (comment.getState() != CommentState.PUBLISHED) {
            log.warn("Выброшено ConflictException: нельзя удалить лайк у неопубликованного комментария.");
            throw new ConflictException("Нельзя удалить лайк у неопубликованного комментария.", "Для запрошенной операции условия не выполнены.");
        }
        if (!comment.getLikes().contains(user)) {
            log.warn("Выброшено ConflictException: лайк с userId: {} не существует.", userId);
            throw new ConflictException("Лайк пользователя с id: " + userId + "к комментарию с id: " + comId + " отсутствует.",
                    "Пользователь не может удалить несуществующий лайк.");
        }
        comment.getLikes().remove(user);
        Comment updatedComment = commentRepository.save(comment);
        log.info("Лайк удален, новое количество лайков: {}", updatedComment.getLikes().size());
        return commentMapper.toCommentDto(updatedComment);
    }

    public CommentDto addDislikeToComment(long userId, long comId) {
        User user = validateUserExisted(userId);
        Comment comment = validateCommentExisted(comId);
        if (comment.getAuthor().getId() == userId) {
            log.warn("Выброшено ConflictException: нельзя добавить дизлайк своему комментарию.");
            throw new ConflictException("Нельзя добавить дизлайк своему комментарию.", "Для запрошенной операции условия не выполнены.");
        }
        if (comment.getState() != CommentState.PUBLISHED) {
            log.warn("Выброшено ConflictException: нельзя добавить дизлайк неопубликованному комментарию.");
            throw new ConflictException("Нельзя добавить дизлайк неопубликованному комментарию.", "Для запрошенной операции условия не выполнены.");
        }
        if (comment.getDislikes().contains(user)) {
            log.warn("Выброшено ConflictException: дизлайк уже добавлен.");
            throw new ConflictException("Дизлайк пользователя с id: " + userId + "к комментарию с id: " + comId + " уже добавлен.",
                    "Пользователь может добавить только один дизлайк.");
        }
        comment.getDislikes().add(user);
        comment.getLikes().remove(user);
        Comment updatedComment = commentRepository.save(comment);
        log.info("Дизлайк сохранен, новое количество дизлайков: {}", updatedComment.getDislikes().size());
        return commentMapper.toCommentDto(updatedComment);
    }

    public CommentDto deleteDislikeFromComment(long userId, long comId) {
        User user = validateUserExisted(userId);
        Comment comment = validateCommentExisted(comId);
        if (comment.getAuthor().getId() == userId) {
            log.warn("Выброшено ConflictException: нельзя удалить дизлайк у своего комментария.");
            throw new ConflictException("Нельзя удалить дизлайк у своего комментария.", "Для запрошенной операции условия не выполнены.");
        }
        if (comment.getState() != CommentState.PUBLISHED) {
            log.warn("Выброшено ConflictException: нельзя удалить дизлайк у неопубликованного комментария.");
            throw new ConflictException("Нельзя удалить дизлайк у неопубликованного комментария.", "Для запрошенной операции условия не выполнены.");
        }
        if (!comment.getDislikes().contains(user)) {
            log.warn("Выброшено ConflictException: дизлайк с userId: {} не существует.", userId);
            throw new ConflictException("Дизлайк пользователя с id: " + userId + "к комментарию с id: " + comId + " отсутствует.",
                    "Пользователь не может удалить несуществующий дизлайк.");
        }
        comment.getDislikes().remove(user);
        Comment updatedComment = commentRepository.save(comment);
        log.info("Дизлайк удален, новое количество дизлайков: {}", updatedComment.getDislikes().size());
        return commentMapper.toCommentDto(updatedComment);
    }

    private Predicate getPredicateForAdminSearch(List<Long> users,
                                                 List<CommentState> states,
                                                 List<Long> events,
                                                 LocalDateTime rangeStart,
                                                 LocalDateTime rangeEnd) {
        QComment comment = QComment.comment;
        BooleanBuilder predicate = new BooleanBuilder();

        // проверка запрашиваемых параметров
        if (users != null && !users.isEmpty())
            predicate.and(comment.author.id.in(users));
        if (states != null && !states.isEmpty())
            predicate.and(comment.state.in(states));
        if (events != null && !events.isEmpty())
            predicate.and(comment.event.id.in(events));
        if (rangeStart != null)
            predicate.and(comment.created.goe(rangeStart));
        if (rangeEnd != null)
            predicate.and(comment.created.loe(rangeEnd));
        return predicate;
    }

    private User validateUserExisted(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + " не найден.", "Искомый объект не был найден."));
    }

    private Event validateEventExisted(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id: " + eventId + " не найдено.", "Искомый объект не был найден."));
    }

    private Comment validateCommentExistedByUserId(long comId, long userId) {
        return commentRepository.findByIdAndAuthorId(comId, userId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id: " + comId + " пользователя с id: "
                        + userId + " не найден.", "Искомый объект не был найден."));
    }

    private Comment validateCommentExisted(long comId) {
        return commentRepository.findById(comId)
                .orElseThrow(() -> new NotFoundException("Комментарий с id: " + comId + " не найден.",
                        "Искомый объект не был найден."));
    }
}
