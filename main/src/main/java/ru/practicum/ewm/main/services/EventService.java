package ru.practicum.ewm.main.services;

import com.querydsl.core.BooleanBuilder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.querydsl.core.types.Predicate;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.practicum.ewm.main.dto.*;
import ru.practicum.ewm.main.enums.EventsSort;
import ru.practicum.ewm.main.enums.EventsState;
import ru.practicum.ewm.main.enums.StateActionAdmin;
import ru.practicum.ewm.main.exceptions.*;
import ru.practicum.ewm.main.mapper.EventMapper;
import ru.practicum.ewm.main.model.*;
import ru.practicum.ewm.main.repository.CategoryRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.main.repository.UserRepository;
import ru.practicum.ewm.stats.client.StatClient;
import ru.practicum.ewm.stats.dto.EndpointHitNewRequest;
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

    private final StatClient client;
    private final EventMapper eventMapper;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public Collection<EventShortDto> findEventsByUserId(long userId, int from, int size) {
        validateUserExisted(userId);

        final PageRequest pageable = PageRequest.of(from / size, size);
        log.info("Поиск событий пользователя с id:{} c номера страницы {} и c количеством элементов на странице {}.", userId, from, size);
        List<Event> events = eventRepository.findByInitiatorIdOrderByCreatedOnDesc(userId, pageable);

        if (events.isEmpty()) {
            log.debug("События для пользователя с id: {} не найдены.", userId);
            return Collections.emptyList();
        }

        Map<Long, Long> views = getViewsByUris(events, true);
        if (views.isEmpty())
            return eventMapper.toEventShortDtoList(events);
        return events.stream()
                .map(event -> {
                    EventShortDto eventShortDto = eventMapper.toEventShortDto(event);
                    eventShortDto.setViews(views.getOrDefault(event.getId(), 0L));
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
        event.setViews(getEventsViews(eventId, event.getCreatedOn(), true));
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
            log.warn("Выброшено исключение ConflictException: опубликовать можно только события с EventsState.PENDING.");
            throw new ConflictException("Опубликовать можно только события с EventsState.PENDING.", "Для запрошенной операции условия не выполнены.");

        }
        if (event.getState() == EventsState.PUBLISHED && request.getStateAction() == StateActionAdmin.REJECT_EVENT) {
            log.warn("Выброшено исключение ConflictException: невозможно отклонить опубликованное событие.");
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
        updatedEvent.setViews(getEventsViews(eventId, event.getCreatedOn(), true));
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
            log.warn("Выброшено исключение ConflictException: невозможно обновить опубликованное событие.");
            throw new ConflictException("Невозможно обновить опубликованное событие.", "Для запрошенной операции условия не выполнены.");
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
        updEvent.setViews(getEventsViews(eventId, event.getCreatedOn(), true));
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
        validateRangeStartAndRangeEnd(rangeStart, rangeEnd);
        final Pageable pageable = PageRequest.of(from / size, size);
        Predicate predicate = getPredicateForAdminSearch(users, states, categories, rangeStart, rangeEnd);

        List<Event> events = eventRepository.findAll(predicate, pageable).getContent();
        if (events.isEmpty()) {
            log.info("События по  заданным параметрам не найдены.");
            return List.of();
        }
        Map<Long, Long> views = getViewsByUris(events, true);
        events.forEach(event -> event.setViews(views.getOrDefault(event.getId(), 0L)));
        log.info("Запрашиваемые события найдены в количестве: {}.", events.size());
        return eventMapper.toEventFullDtoList(events);
    }

    public List<EventShortDto> getPublicEvents(
            String text,
            List<Integer> categories,
            Boolean paid,
            LocalDateTime rangeStart,
            LocalDateTime rangeEnd,
            Boolean onlyAvailable,
            EventsSort sort,
            int from,
            int size,
            HttpServletRequest request) {
        validateRangeStartAndRangeEnd(rangeStart, rangeEnd);
        final Pageable pageable;
        if (sort == EventsSort.EVENT_DATE)
            pageable = PageRequest.of(from / size, size, Sort.by("eventDate").descending());
        else
            pageable = PageRequest.of(from / size, size);
        Predicate predicate = getPredicateForPublicSearch(text, categories, paid, rangeStart, rangeEnd, onlyAvailable);
        List<Event> events = eventRepository.findAll(predicate, pageable).getContent();
        if (events.isEmpty()) {
            log.info("События по заданным параметрам не найдены.");
            return List.of();
        }
        Map<Long, Long> views = getViewsByUris(events, true);
        events.forEach(event -> {
            event.setViews(views.getOrDefault(event.getId(), 0L));
            saveHit(request.getRemoteAddr(), request.getRequestURI() + "/" + event.getId());
        });
        if (sort == EventsSort.VIEWS) {
            events = events.stream().sorted(Comparator.comparing(Event::getViews).reversed()).toList();
        }
        log.info("Запрашиваемые события найдены в количестве:{}.", events.size());
        return eventMapper.toEventShortDtoList(events);
    }

    public EventFullDto getPublicEventById(long eventId, HttpServletRequest request) {
        Event event = validateEventExisted(eventId);
        if (event.getState() != EventsState.PUBLISHED) {
            log.warn("Выброшено NotFoundException: искомый объект не опубликован.");
            throw new NotFoundException("Событие с id: " + eventId + " не было найдено.", "Искомый объект не опубликован.");
        }
        event.setViews(getEventsViews(eventId, event.getPublishedOn(), true));
        saveHit(request.getRemoteAddr(), request.getRequestURI());
        log.info("Запрашиваемое событие с id: {} найдено.", event.getId());
        return eventMapper.toEventFullDto(event);
    }

    private Predicate getPredicateForAdminSearch(List<Long> users,
                                                 List<EventsState> states,
                                                 List<Integer> categories,
                                                 LocalDateTime rangeStart,
                                                 LocalDateTime rangeEnd) {
        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();

        // проверка запрашиваемых параметров
        if (users != null && !users.isEmpty())
            predicate.and(event.initiator.id.in(users));
        if (states != null && !states.isEmpty())
            predicate.and(event.state.in(states));
        if (categories != null && !categories.isEmpty())
            predicate.and(event.category.id.in(categories));
        if (rangeStart != null)
            predicate.and(event.eventDate.goe(rangeStart));
        if (rangeEnd != null)
            predicate.and(event.eventDate.loe(rangeEnd));
        return predicate;
    }

    private Predicate getPredicateForPublicSearch(String text,
                                                  List<Integer> categories,
                                                  Boolean paid,
                                                  LocalDateTime rangeStart,
                                                  LocalDateTime rangeEnd,
                                                  Boolean onlyAvailable) {
        QEvent event = QEvent.event;
        BooleanBuilder predicate = new BooleanBuilder();
        predicate.and(event.state.eq(EventsState.PUBLISHED));

        // проверка запрашиваемых параметров
        if (StringUtils.hasText(text)) {
            predicate.and(event.annotation.toLowerCase().contains(text.toLowerCase()))
                    .or(event.description.toLowerCase().contains(text.toLowerCase()));
        }
        if (categories != null && !categories.isEmpty())
            predicate.and(event.category.id.in(categories));
        if (paid != null)
            predicate.and(event.paid.eq(paid));
        if (rangeStart != null)
            predicate.and(event.eventDate.goe(rangeStart));
        if (rangeEnd != null)
            predicate.and(event.eventDate.loe(rangeEnd));
        if (rangeStart == null && rangeEnd == null)
            predicate.and(event.eventDate.goe(LocalDateTime.now()));
        if (onlyAvailable != null)
            predicate.and(event.participantLimit.eq(0L))
                    .or(event.participantLimit.gt(event.confirmedRequests));
        return predicate;
    }

    private void saveHit(String ip, String uri) {
        EndpointHitNewRequest newRequest = EndpointHitNewRequest.builder()
                .app("ewm-main-service")
                .uri(uri)
                .ip(ip)
                .timestamp(LocalDateTime.now())
                .build();
        log.info("Отправка EndpointHitNewRequest в client.");
        client.createHit(newRequest);
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

    private void validateRangeStartAndRangeEnd(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && end.isBefore(start)) {
            log.info("Выброшено ValidationException: дата start должна быть раньше даты end.");
            throw new ValidationException("Дата start должна быть раньше даты end.", "Для запрошенной операции условия не выполнены");
        }
    }

    private Map<Long, Long> getViewsByUris(List<Event> events, boolean uniqueIp) {
        List<String> uris = events
                .stream()
                .map(event -> "/events/" + event.getId())
                .toList();
        final LocalDateTime start;
        Event eventWithEarliestDate = events.stream().min(Comparator.comparing(Event::getCreatedOn)).orElse(null);
        if (eventWithEarliestDate == null)
            start = LocalDateTime.of(2000, 1, 1, 0, 0);
        else
            start = eventWithEarliestDate.getCreatedOn();
        final LocalDateTime end = LocalDateTime.now();
        List<ViewStats> views = client.getStats(start.format(dtf), end.format(dtf), uris, uniqueIp);
        if (views.isEmpty()) {
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
        return views.stream()
                .collect(Collectors.toMap(
                        viewStats -> getIdFromUriString(viewStats.getUri()), ViewStats::getHits
                ));
    }

    private Long getEventsViews(long eventId, LocalDateTime start, boolean uniqueIp) {
        List<String> uris = List.of("/events/" + eventId);
        final LocalDateTime end = LocalDateTime.now().plusHours(1);
        return client.getStats(start.format(dtf), end.format(dtf), uris, uniqueIp)
                .stream()
                .findFirst()
                .map(ViewStats::getHits)
                .orElse(0L);
    }

    private Long getIdFromUriString(String uri) {
        try {
            return Long.parseLong(uri.replace("/events/", ""));
        } catch (NumberFormatException e) {
            log.warn("Не удалось извлечь id из uri.");
            return 0L;
        }
    }
}
