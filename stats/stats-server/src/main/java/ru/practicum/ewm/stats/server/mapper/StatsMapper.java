package ru.practicum.ewm.stats.server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import ru.practicum.ewm.stats.dto.EndpointHitDto;
import ru.practicum.ewm.stats.dto.EndpointHitNewRequest;
import ru.practicum.ewm.stats.server.model.EndpointHit;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface StatsMapper {

    EndpointHit toEndpointHit(EndpointHitNewRequest newRequest);

    EndpointHitDto toEndpointHitDto(EndpointHit endpointHit);
}
