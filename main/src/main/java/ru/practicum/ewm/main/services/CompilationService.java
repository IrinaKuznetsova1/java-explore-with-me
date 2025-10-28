package ru.practicum.ewm.main.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.NewCompilation;
import ru.practicum.ewm.main.dto.UpdateCompilationAdminRequest;
import ru.practicum.ewm.main.exceptions.ConflictException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.mapper.CompilationMapper;
import ru.practicum.ewm.main.model.Compilation;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.repository.CompilationRepository;
import ru.practicum.ewm.main.repository.EventRepository;
import ru.practicum.ewm.stats.client.StatClient;
import ru.practicum.ewm.stats.dto.ViewStats;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final StatClient client;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public CompilationDto createCompilation(NewCompilation newCompilation) {
        validateCompilationsTitle(newCompilation.getTitle());
        Compilation compilation = compilationMapper.toCompilation(newCompilation);
        if (newCompilation.getEvents() != null && !newCompilation.getEvents().isEmpty()) {
            compilation.setEvents(getSetEventsWithViews(newCompilation.getEvents()));
            log.info("Добавлены просмотры при создании новой подборки.");
        }
        Compilation savedComp = compilationRepository.save(compilation);
        log.info("Новая подборка сохранена, id: {}", savedComp.getId());
        return compilationMapper.toCompilationDto(compilation);
    }

    public void deleteCompilationById(long compId) {
        validateCompilationExisted(compId);
        compilationRepository.deleteById(compId);
        log.info("Подборка с id: {} удалена.", compId);
    }

    public CompilationDto updateCompilationByAdmin(long compId, UpdateCompilationAdminRequest request) {
        Compilation compilation = validateCompilationExisted(compId);
        validateCompilationsTitle(request.getTitle());
        if (request.getEvents() != null && !request.getEvents().isEmpty()) {
            compilation.setEvents(getSetEventsWithViews(request.getEvents()));
            log.info("Актуализированы просмотры при обновлении подборки.");
        }
        if (!compilation.getEvents().isEmpty()) {
            Set<Long> eventsIds = compilation.getEvents().stream()
                    .map(Event::getId)
                    .collect(Collectors.toSet());
            compilation.setEvents(getSetEventsWithViews(eventsIds));
        }
        Compilation updComp = compilationRepository.save(compilationMapper.updateAdminCompilation(request, compilation));
        log.info("Подборка с id: {} обновлена.", compId);
        return compilationMapper.toCompilationDto(updComp);
    }

    private Set<Event> getSetEventsWithViews(Set<Long> eventsIds) {
        List<Event> events = eventRepository.findByIdInOrderByCreatedOnDesc(eventsIds);
        Map<Long, Long> views = getViewsByUris(events);
        return events.stream()
                .peek(event -> event.setViews(views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toSet());
    }

    private Map<Long, Long> getViewsByUris(List<Event> events) {
        List<String> uris = events
                .stream()
                .map(event -> "/events/" + event.getId())
                .toList();
        final LocalDateTime start = events.getLast().getCreatedOn();
        final LocalDateTime end = LocalDateTime.now();
        List<ViewStats> views = client.getStats(start.format(dtf), end.format(dtf), uris, false);
        if (views.isEmpty()) {
            return events.stream()
                    .collect(Collectors.toMap(Event::getId, event -> 0L));
        }
        return views.stream()
                .collect(Collectors.toMap(
                        viewStats -> getIdFromUriString(viewStats.getUri()), ViewStats::getHits
                ));
    }

    private Long getIdFromUriString(String uri) {
        try {
            return Long.parseLong(uri.replace("/events/", ""));
        } catch (NumberFormatException e) {
            log.warn("Не удалось извлечь id из uri.");
            return 0L;
        }
    }

    private Compilation validateCompilationExisted(long compId) {
        return compilationRepository.findById(compId).orElseThrow(() ->
                new NotFoundException("Подборка с id: " + compId + " не найдена", "Искомый объект не найден."));
    }

    private void validateCompilationsTitle(String title) {
        if (compilationRepository.existsCompilationByTitle(title)) {
            log.warn("Выброшено ConflictException: подборка с названием: {} уже существует.", title);
            throw new ConflictException("Подборка с названием: " + title + " уже существует.", "Было нарушено ограничение целостности.");
        }
    }
}
