package ru.practicum.ewm.main.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.dto.NewUserRequest;
import ru.practicum.ewm.main.dto.UserDto;
import ru.practicum.ewm.main.exceptions.DuplicatedDataException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.mapper.UserMapper;
import ru.practicum.ewm.main.model.User;
import ru.practicum.ewm.main.repository.UserRepository;

import java.util.Collection;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;

    public Collection<UserDto> findUsers(List<Long> ids, int from, int size) {
        final PageRequest pageable = PageRequest.of(from / size, size, Sort.by("id").ascending());
        if (ids == null || ids.isEmpty()) {
            log.info("Поиск пользователей c номера страницы {} и c количеством элементов на странице {}.", from, size);
            return mapper.toUserDtoList(repository.findAll(pageable).getContent());
        } else {
            log.info("Поиск пользователей c учетом списка ids c номера страницы {} и c количеством элементов на странице {}.", from, size);
            return mapper.toUserDtoList(repository.findByIdIn(ids, pageable));
        }
    }

    public UserDto create(NewUserRequest newUser) {
        if (repository.findByEmail(newUser.getEmail()).isPresent()) {
            log.warn("Выброшено исключение DuplicatedDataException, пользователь с указанным email: {} уже существует.", newUser.getEmail());
            throw new DuplicatedDataException("Пользователь с указанным email уже существует.", "Было нарушено ограничение целостности");
        }
        final User user = repository.save(mapper.toUser(newUser));
        log.info("Пользователь успешно сохранен.");
        return mapper.toUserDto(user);
    }

    public void deleteUser(long userId) {
        repository.findById(userId).
                orElseThrow(() -> new NotFoundException("Пользователь с id: " + userId + " не найден.", "Искомый объект не был найден."));
        repository.deleteById(userId);
        log.info("Пользователь с id {} удален.", userId);
    }
}
