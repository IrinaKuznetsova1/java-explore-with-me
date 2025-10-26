package ru.practicum.ewm.main.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.core5.http.HttpException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.client.RestClientException;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.exceptions.NotAvailableException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.exceptions.TimeValidationException;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.mapper.RequestMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.Request;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;
import ru.practicum.ewm.main.repository.UserRepository;
import ru.practicum.ewm.main.statClient.StatClient;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final RequestRepository requestRepository;

    private final StatClient client;
    private final EventMapper eventMapper;
    private final RequestMapper requestMapper;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Collection<EventShortDto> findEventsByUserId(long userId, int from, int size) {
        validateUserExisted(userId);

        final PageRequest pageable = PageRequest.of(from, size);
        log.info("Поиск событий пользователя с id:{} c номера страницы {} и c количеством элементов на странице {}.", userId, from, size);
        List<Event> events = eventRepository.findByInitiatorIdOrderByCreatedOnDesc(userId, pageable);

        if (events.isEmpty()) {
            log.debug("События для пользователя с id: {} не найдены.", userId);
            return Collections.emptyList();
        }

        Map<Long, Long> views = getViewsByUris(events);
        if (views.isEmpty())
            return eventMapper.toEventShortDtoList(events);
        return events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setViews(views.get(event.getId()));
                    return eventShortDto;
                })
                .toList();
    }

    public EventFullDto createEvent(long userId, NewEventDto newEvent) {
        validateEventDate(newEvent.getEventDate());
        final User user = validateUserExisted(userId);
        final Category category = validateCategoryExisted(newEvent.getCategory());
        Event savedEvent = eventRepository.save(eventMapper.toEvent(newEvent, category, user));
        return eventMapper.toEventFullDto(savedEvent);
    }

    public EventFullDto findEventByIdAndUserId(long eventId, long userId) {
        validateUserExisted(userId);
        Event event = validateEventExistedByUserId(eventId, userId);
        event.setViews(getEventsViews(eventId, event.getCreatedOn()));
        event.setConfirmedRequests(requestRepository.getCountConfirmedRequestsByEventId(eventId));
        return eventMapper.toEventFullDto(event);
    }

    public EventFullDto updateEventByUser(UpdateEventUserRequest request, long eventId, long userId) {
        validateUserExisted(userId);
        Category category = null;
        if (request.getCategory() != null)
            category = validateCategoryExisted(request.getCategory());
        Event event = validateEventExistedByUserId(eventId, userId);
        if (event.getState() == EventsState.PUBLISHED) {
            log.warn("Выброшено исключение NotAvailableException: невозможно обновить опубликованное событие.");
            throw new NotAvailableException("Невозможно обновить опубликованное событие.", "Для запрошенной операции условия не выполнены.");
        }
        if (request.getEventDate() != null)
            validateEventDate(request.getEventDate());
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventsState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventsState.CANCELED);
            }
        }
        Event updEvent = eventRepository.save(eventMapper.updateUserEvent(request, event, category));
        updEvent.setViews(getEventsViews(eventId, event.getCreatedOn()));
        event.setConfirmedRequests(requestRepository.getCountConfirmedRequestsByEventId(eventId));
        log.info("Событие с id: {} успешно обновлено.", eventId);
        return eventMapper.toEventFullDto(updEvent);
    }

    private User validateUserExisted(long userId) {
        return userRepository.findById(userId).
                orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + " не найден.", "Искомый объект не был найден."));
    }

    private Category validateCategoryExisted(int catId) {
        return categoryRepository.findById(catId).
                orElseThrow(() -> new NotFoundException("Категория с id: " + catId + " не найдена.", "Искомый объект не был найден."));
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

    private void validateEventDate(LocalDateTime eventDate) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(2))) {
            log.warn("Выброшено TimeValidationException: дата события не может быть раньше,чем через 2 часа.");
            throw new TimeValidationException("Для запрошенной операции условия не выполнены.",
                    "Дата события не может быть раньше,чем через 2 часа.");
        }
    }

    private Map<Long, Long> getViewsByUris(List<Event> events) {
        List<String> uris = events
                .stream()
                .map(event -> "events/" + event.getId())
                .toList();
        final LocalDateTime start = events.getLast().getCreatedOn();
        final LocalDateTime end = LocalDateTime.now();
        return client.getStats(start.format(dtf), end.format(dtf), uris, true).stream()
                .collect(Collectors.toMap(
                        viewStats -> extractIdFromUri(viewStats.getUri()), ViewStats::getHits
                ));
    }

    private Long getEventsViews(long eventId, LocalDateTime start) {
        List<String> uris = List.of("event/" + eventId);
        final LocalDateTime end = LocalDateTime.now();
        return client.getStats(start.format(dtf), end.format(dtf), uris, true)
                .stream()
                .findFirst()
                .map(ViewStats::getHits)
                .orElse(0L);
    }


    private Long extractIdFromUri(String uri) {
        try {
            return Long.parseLong(uri.replace("events/", ""));
        } catch (NumberFormatException e) {
            log.warn("Не удалось извлечь id из uri.");
            return 0L;
        }
    }
}
