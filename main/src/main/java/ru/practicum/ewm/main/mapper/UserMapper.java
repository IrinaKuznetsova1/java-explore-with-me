package ru.practicum.ewm.main.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import ru.practicum.ewm.main.dto.newRequests.NewUserRequest;
import ru.practicum.ewm.main.dto.responses.UserDto;
import ru.practicum.ewm.main.model.User;

import java.util.List;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING, unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    User toUser(NewUserRequest newUser);

    UserDto toUserDto(User user);

    List<UserDto> toUserDtoList(List<User> users);
}
