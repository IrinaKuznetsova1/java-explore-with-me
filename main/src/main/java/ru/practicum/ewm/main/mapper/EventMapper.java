package ru.practicum.ewm.main.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.main.dto.newRequests.NewEventDto;
import ru.practicum.ewm.main.dto.responses.EventFullDto;
import ru.practicum.ewm.main.dto.responses.EventShortDto;
import ru.practicum.ewm.main.dto.updateRequests.UpdateEventAdminRequest;
import ru.practicum.ewm.main.dto.updateRequests.UpdateEventUserRequest;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.model.Event;
import ru.practicum.ewm.main.model.User;

import java.time.LocalDateTime;
import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {CategoryMapper.class, UserMapper.class, CommentMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface EventMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "createdOn", expression = "java(getNow())")
    @Mapping(target = "state", expression = "java(ru.practicum.ewm.main.enums.EventsState.PENDING)")
    Event toEvent(NewEventDto newEvent, Category category, User initiator);

    @Mapping(target = "comments", source = "comments")
    EventFullDto toEventFullDto(Event event);

    List<EventFullDto> toEventFullDtoList(List<Event> events);

    EventShortDto toEventShortDto(Event event);

    List<EventShortDto> toEventShortDtoList(List<Event> events);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    Event updateAdminEvent(UpdateEventAdminRequest request, @MappingTarget Event event, Category category);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "category")
    Event updateUserEvent(UpdateEventUserRequest request, @MappingTarget Event event, Category category);

    default LocalDateTime getNow() {
        return LocalDateTime.now();
    }
}
