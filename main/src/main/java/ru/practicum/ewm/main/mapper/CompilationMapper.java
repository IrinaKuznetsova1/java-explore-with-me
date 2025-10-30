package ru.practicum.ewm.main.mapper;

import org.mapstruct.*;
import ru.practicum.ewm.main.dto.CompilationDto;
import ru.practicum.ewm.main.dto.NewCompilation;
import ru.practicum.ewm.main.dto.UpdateCompilationAdminRequest;
import ru.practicum.ewm.main.model.Compilation;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        uses = {EventMapper.class},
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CompilationMapper {
    @Mapping(target = "events", ignore = true)
    Compilation toCompilation(NewCompilation newCompilation);

    @Mapping(target = "events", source = "events")
    CompilationDto toCompilationDto(Compilation compilation);

    List<CompilationDto> toCompilationDtoList(List<Compilation> compilationList);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "events", ignore = true)
    Compilation updateAdminCompilation(UpdateCompilationAdminRequest request,
                                       @MappingTarget Compilation compilation);
}
