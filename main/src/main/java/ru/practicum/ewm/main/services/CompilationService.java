package ru.practicum.ewm.main.services;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.constants.Constants;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CompilationService {
    private final CompilationRepository compilationRepository;
    private final EventRepository eventRepository;
    private final CompilationMapper compilationMapper;
    private final StatClient client;

    public CompilationDto createCompilation(NewCompilation newCompilation) {
        validateCompilationsTitle(newCompilation.getTitle());
        Compilation compilation = compilationMapper.toCompilation(newCompilation);
        if (newCompilation.getEvents() != null && !newCompilation.getEvents().isEmpty()) {
            compilation.setEvents(getSetEventsWithViewsByIds(newCompilation.getEvents()));
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
            compilation.setEvents(getSetEventsWithViewsByIds(request.getEvents()));
            log.info("Актуализированы просмотры при обновлении подборки.");
        }
        if (!compilation.getEvents().isEmpty()) {
            compilation.setEvents(getSetEventsWithViewsByEvents(compilation.getEvents()));
        }
        Compilation updComp = compilationRepository.save(compilationMapper.updateAdminCompilation(request, compilation));
        log.info("Подборка с id: {} обновлена.", compId);
        return compilationMapper.toCompilationDto(updComp);
    }

    @Transactional(readOnly = true)
    public List<CompilationDto> findPublicCompilations(Boolean pinned, int from, int size) {
        Pageable pageable = PageRequest.of(from / size, size);
        List<Compilation> compilations;
        if (pinned != null)
            compilations = compilationRepository.findByPinned(pinned, pageable);
        else
            compilations = compilationRepository.findAll(pageable).getContent();

        if (compilations.isEmpty())
            return List.of();
        for (Compilation compilation : compilations) {
            if (compilation.getEvents() != null && !compilation.getEvents().isEmpty()) {
                compilation.setEvents(getSetEventsWithViewsByEvents(compilation.getEvents()));
            }
        }

        return compilationMapper.toCompilationDtoList(compilations);
    }

    @Transactional(readOnly = true)
    public CompilationDto findPublicCompilationById(long compId) {
        Compilation compilation = validateCompilationExisted(compId);
        if (!compilation.getEvents().isEmpty()) {
            compilation.setEvents(getSetEventsWithViewsByEvents(compilation.getEvents()));
        }
        log.info("Подборка с id: {} найдена.", compilation.getId());
        return compilationMapper.toCompilationDto(compilation);
    }

    private Set<Event> getSetEventsWithViewsByIds(Set<Long> eventsIds) {
        List<Event> events = eventRepository.findByIdIn(eventsIds);
        Map<Long, Long> views = getViewsByUris(events);
        return events.stream()
                .peek(event -> event.setViews(views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toSet());
    }

    private Set<Event> getSetEventsWithViewsByEvents(Set<Event> events) {
        Map<Long, Long> views = getViewsByUris(events.stream().toList());
        return events.stream()
                .peek(event -> event.setViews(views.getOrDefault(event.getId(), 0L)))
                .collect(Collectors.toSet());
    }

    private Map<Long, Long> getViewsByUris(List<Event> events) {
        List<String> uris = events
                .stream()
                .map(event -> "/events/" + event.getId())
                .toList();
        Event eventWithEarliestDate = events.stream().min(Comparator.comparing(Event::getCreatedOn)).orElse(null);
        final LocalDateTime start;
        if (eventWithEarliestDate == null)
            start = LocalDateTime.of(2000, 1, 1, 0, 0);
        else
            start = eventWithEarliestDate.getCreatedOn();
        final LocalDateTime end = LocalDateTime.now();
        List<ViewStats> views = client.getStats(start.format(Constants.DTF), end.format(Constants.DTF), uris, false);
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
        if (title != null && compilationRepository.existsCompilationByTitle(title)) {
            log.warn("Выброшено ConflictException: подборка с названием: {} уже существует.", title);
            throw new ConflictException("Подборка с названием: " + title + " уже существует.", "Было нарушено ограничение целостности.");
        }
    }
}
