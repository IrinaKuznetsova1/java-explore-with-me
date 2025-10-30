package ru.practicum.ewm.main.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateRequest;
import ru.practicum.ewm.main.dto.EventRequestStatusUpdateResult;
import ru.practicum.ewm.main.dto.ParticipationRequestDto;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.enums.RequestStatus;
import ru.practicum.ewm.main.exceptions.ConflictException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.mapper.RequestMapper;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.Request;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;
import ru.practicum.ewm.main.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RequestService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RequestRepository requestRepository;
    private final RequestMapper requestMapper;

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> findUsersRequests(long userId) {
        validateUserExisted(userId);
        return requestMapper.toParticipationRequestDtoList(requestRepository.findByRequesterId(userId));
    }

    public ParticipationRequestDto create(long userId, long eventId) {
        User requester = validateUserExisted(userId);
        Event event = validateEventExisted(eventId);
        if (requestRepository.findByEventIdAndRequesterId(eventId, userId).isPresent()) {
            log.warn("Выброшено ConflictException: заявка на участие уже сохранена.");
            throw new ConflictException("Невозможно повторно сохранить запрос.", "Заявка на участие уже сохранена.");
        }

        if (event.getInitiator().getId() == userId) {
            log.warn("Выброшено ConflictException: инициатор события не может добавить запрос на участие в своём событии.");
            throw new ConflictException("Невозможно сохранить запрос.", "Инициатор события не может добавить запрос на участие в своём событии.");
        }

        if (event.getState() != EventsState.PUBLISHED) {
            log.warn("Выброшено ConflictException: нельзя участвовать в неопубликованном событии.");
            throw new ConflictException("Невозможно сохранить запрос.", "Нельзя участвовать в неопубликованном событии.");
        }

        long countConfirmedRequests = event.getConfirmedRequests();
        if (event.getParticipantLimit() != 0 && (event.getParticipantLimit() < ++countConfirmedRequests)) {
            log.warn("Выброшено ConflictException: достигнут лимит запросов на участие .");
            throw new ConflictException("Невозможно сохранить запрос.", "Достигнут лимит запросов на участие .");
        }

        Request newRequest = Request.builder()
                .created(LocalDateTime.now())
                .event(event)
                .requester(requester)
                .build();
        if (event.getParticipantLimit() == 0 || !event.isRequestModeration()) {
            newRequest.setStatus(RequestStatus.CONFIRMED);
            event.setConfirmedRequests(countConfirmedRequests + 1);
            eventRepository.save(event);
        } else
            newRequest.setStatus(RequestStatus.PENDING);

        Request savedRequest = requestRepository.save(newRequest);
        log.info("Запрос на участие успешно сохранен.");
        return requestMapper.toParticipationRequestDto(savedRequest);
    }

    public ParticipationRequestDto cancelRequest(long userId, long requestId) {
        validateUserExisted(userId);
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Запрос на участие с id: " + requestId + " не найден.",
                        "Искомый объект не был найден."));
        if (request.getRequester().getId() != userId) {
            throw new NotFoundException("Запрос на участие с id: " + requestId + " пользователя с id: " + userId + " не найден.",
                    "Искомый объект не был найден.");
        }
        RequestStatus oldStatus = request.getStatus();
        request.setStatus(RequestStatus.CANCELED);
        Request updRequest = requestRepository.save(request);
        if (oldStatus == RequestStatus.CONFIRMED) {
            Event event = request.getEvent();
            long newConfirmedRequests = event.getConfirmedRequests() - 1;
            event.setConfirmedRequests(newConfirmedRequests);
            eventRepository.save(event);
        }
        log.info("Запрос на участие успешно отменен.");
        return requestMapper.toParticipationRequestDto(updRequest);
    }

    @Transactional(readOnly = true)
    public List<ParticipationRequestDto> findRequests(long eventId, long userId) {
        validateUserExisted(userId);
        validateEventExistedByUserId(eventId, userId);
        List<Request> requestList = requestRepository.findAllByEventId(eventId);
        if (requestList.isEmpty())
            return Collections.emptyList();
        return requestMapper.toParticipationRequestDtoList(requestList);
    }

    public EventRequestStatusUpdateResult updateRequestsStatus(long eventId,
                                                               long userId,
                                                               EventRequestStatusUpdateRequest request) {
        validateUserExisted(userId);
        Event event = validateEventExistedByUserId(eventId, userId);
        List<Request> requestsToUpdate = requestRepository.findAllByIdInAndEventId(request.getRequestIds(), eventId);
        if (event.getParticipantLimit() == 0 || !event.isRequestModeration()) {
            log.warn("Выброшено ConflictException: подтверждение заявок не требуется.");
            throw new ConflictException("Подтверждение заявок не требуется.", "Некорректный запрос.");
        }
        List<Request> confirmedRequests = new ArrayList<>();
        List<Request> rejectedRequests = new ArrayList<>();

        if (request.getStatus() == RequestStatus.REJECTED) {
            requestsToUpdate.forEach(req -> {
                if (req.getStatus() == RequestStatus.PENDING) {
                    req.setStatus(RequestStatus.REJECTED);
                    requestRepository.save(req);
                    rejectedRequests.add(req);
                } else {
                    log.warn("Выброшено ConflictException: невозможно обновить статус.");
                    throw new ConflictException("Выброшено ConflictException: невозможно обновить статус.", "Некорректный запрос.");
                }
            });
        }
        if (request.getStatus() == RequestStatus.CONFIRMED) {
            requestsToUpdate.forEach(req -> {
                if (req.getStatus() == RequestStatus.PENDING) {
                    long count = event.getConfirmedRequests();
                    if (count == event.getParticipantLimit()) {
                        log.warn("Выброшено ConflictException: Достигнут лимит по заявкам на данное событие..");
                        throw new ConflictException("Достигнут лимит по заявкам на данное событие.", "Для запрошенной операции условия не выполнены.");
                    }
                    req.setStatus(RequestStatus.CONFIRMED);
                    requestRepository.save(req);
                    confirmedRequests.add(req);
                    event.setConfirmedRequests(++count);
                } else {
                    log.warn("Выброшено  ConflictException: невозможно обновить статус.");
                    throw new ConflictException("Выброшено ConflictException: невозможно обновить статус.", "Некорректный запрос.");
                }
            });
            eventRepository.save(event);
        }
        return new EventRequestStatusUpdateResult(
                requestMapper.toParticipationRequestDtoList(confirmedRequests),
                requestMapper.toParticipationRequestDtoList(rejectedRequests));
    }

    private User validateUserExisted(long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + " не найден.", "Искомый объект не был найден."));
    }

    private Event validateEventExistedByUserId(long eventId, long userId) {
        return eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> new NotFoundException("Событие с id: " + eventId +
                        " пользователя с id: " + userId + " не найдено.", "Искомый объект не был найден."));
    }

    private Event validateEventExisted(long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id: " + eventId + " не найдено.", "Искомый объект не был найден."));
    }
}
