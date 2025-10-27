package ru.practicum.ewm.main.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.enums.StateActionAdmin;
import ru.practicum.ewm.main.exceptions.*;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.ConfirmedRequestsCount;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.RequestRepository;
import ru.practicum.ewm.main.repository.UserRepository;
import ru.practicum.ewm.stats.client.StatClient;
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
        Map<Long, Long> confirmedCounts = getConfirmedRequests(events);
        if (views.isEmpty())
            return eventMapper.toEventShortDtoList(events);
        return events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setViews(views.get(event.getId()));
                    eventShortDto.setConfirmedRequests(confirmedCounts.get(event.getId()));
                    return eventShortDto;
                })
                .toList();
    }

    public EventFullDto createEvent(long userId, NewEventDto newEvent) {
        validateEventDate(newEvent.getEventDate(), 2);
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

    public EventFullDto updateEventByAdmin(UpdateEventAdminRequest request, long eventId) {
        Event event = validateEventExisted(eventId);
        Category category = null;
        if (request.getCategory() != null)
            category = validateCategoryExisted(request.getCategory());
        Event updEventForSave = eventMapper.updateAdminEvent(request, event, category);

        if (updEventForSave.getEventDate() != null && updEventForSave.getPublishedOn() != null) {
            if (updEventForSave.getEventDate().isBefore(updEventForSave.getPublishedOn().plusHours(1))) {
                log.warn("Выброшено TimeValidationException: дата события не может быть раньше,чем через 1 час от даты публикации.");
                throw new TimeValidationException("Невозможно обновить: дата события не может быть раньше, чем через 1 час от даты публикации.",
                        "Для запрошенной операции условия не выполнены.");
            }
        }
        if (event.getState() != EventsState.PENDING && request.getStateAction() == StateActionAdmin.PUBLISH_EVENT) {
            log.warn("Выброшено исключение NotAvailableException: опубликовать можно только события с EventsState.PENDING.");
            throw new ConflictException("Опубликовать можно только события с EventsState.PENDING.", "Для запрошенной операции условия не выполнены.");

        }
        if (event.getState() == EventsState.PUBLISHED && request.getStateAction() == StateActionAdmin.REJECT_EVENT) {
            log.warn("Выброшено исключение NotAvailableException: невозможно отклонить опубликованное событие.");
            throw new ConflictException("Невозможно отклонить опубликованное событие.", "Для запрошенной операции условия не выполнены.");

        }
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case PUBLISH_EVENT -> {
                    updEventForSave.setState(EventsState.PUBLISHED);
                    updEventForSave.setPublishedOn(LocalDateTime.now());
                }
                case REJECT_EVENT -> updEventForSave.setState(EventsState.CANCELED);
            }
        }
        Event updatedEvent = eventRepository.save(updEventForSave);
        updatedEvent.setViews(getEventsViews(eventId, event.getCreatedOn()));
        updatedEvent.setConfirmedRequests(requestRepository.getCountConfirmedRequestsByEventId(eventId));
        log.info("Событие  с id: {} успешно обновлено.", eventId);
        return eventMapper.toEventFullDto(updatedEvent);
    }

    public EventFullDto updateEventByUser(UpdateEventUserRequest request, long eventId, long userId) {
        validateUserExisted(userId);
        Category category = null;
        if (request.getCategory() != null)
            category = validateCategoryExisted(request.getCategory());
        Event event = validateEventExistedByUserId(eventId, userId);
        if (event.getState() == EventsState.PUBLISHED) {
            log.warn("Выброшено исключение NotAvailableException: невозможно обновить опубликованное событие.");
            throw new ConflictException("Невозможно обновить опубликованное событие.", "Для запрошенной операции условия не выполнены.");
        }
        if (request.getEventDate() != null)
            validateEventDate(request.getEventDate(), 2);
        if (request.getStateAction() != null) {
            switch (request.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventsState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventsState.CANCELED);
            }
        }
        Event updEvent = eventRepository.save(eventMapper.updateUserEvent(request, event, category));
        updEvent.setViews(getEventsViews(eventId, event.getCreatedOn()));
        updEvent.setConfirmedRequests(requestRepository.getCountConfirmedRequestsByEventId(eventId));
        log.info("Событие с id: {} успешно обновлено.", eventId);
        return eventMapper.toEventFullDto(updEvent);
    }

    public List<EventFullDto> findEventsByAdmin(
            List<Long> users,
            List<EventsState> states,
            List<Integer> categories,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            int from,
            int size) {
        if ((users != null && users.isEmpty()) ||
                (states != null && states.isEmpty()) ||
                (categories != null && categories.isEmpty())) {
            log.warn("Выброшено ValidationException: список в строке запроса не может быть пустым.");
            throw new ValidationException("Список в строке запроса не может быть пустым.", "Некорректный запрос.");
        }
        final Pageable pageable = PageRequest.of(from, size);
        List<Event> events = eventRepository.findEventsByAdminsFilters(users, states, categories, rangeStart, rangeEnd, pageable);
        if (events.isEmpty())
            return Collections.emptyList();
        Map<Long, Long> views = getViewsByUris(events);
        Map<Long, Long> eventsConfirmedRequests = getConfirmedRequests(events);
        events.forEach(event -> {
            event.setViews(views.get(event.getId()));
            event.setConfirmedRequests(eventsConfirmedRequests.get(event.getId()));
        });
        log.info("Запрашиваемые события найдены в количестве: {}.", events.size());
        return eventMapper.toEventFullDtoList(events);
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

    private void validateEventDate(LocalDateTime eventDate, int hours) {
        if (eventDate.isBefore(LocalDateTime.now().plusHours(hours))) {
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
        final LocalDateTime start = LocalDateTime.of(2000, 1, 1, 0, 0, 0);
        final LocalDateTime end = LocalDateTime.now();
        List<ViewStats> views = client.getStats(start.format(dtf), end.format(dtf), uris, true);
        if (views.isEmpty()) {
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
        return views.stream()
                .collect(Collectors.toMap(
                        viewStats -> getIdFromUriString(viewStats.getUri()), ViewStats::getHits
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

    private Map<Long, Long> getConfirmedRequests(List<Event> events) {
        List<Long> eventsIds = events.stream().map(Event::getId).toList();
        List<ConfirmedRequestsCount> confirmedRequestsCounts = requestRepository.getCountConfirmedRequests(eventsIds);
        if (confirmedRequestsCounts.isEmpty()) {
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
        return confirmedRequestsCounts.stream()
                .collect(Collectors.toMap(
                        ConfirmedRequestsCount::getEventId,
                        ConfirmedRequestsCount::getCount
                ));
    }


    private Long getIdFromUriString(String uri) {
        try {
            return Long.parseLong(uri.replace("events/", ""));
        } catch (NumberFormatException e) {
            log.warn("Не удалось извлечь id из uri.");
            return 0L;
        }
    }
}
