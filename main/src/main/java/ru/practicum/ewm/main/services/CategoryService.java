package ru.practicum.ewm.main.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.NewCategoryDto;
import ru.practicum.ewm.main.exceptions.DuplicatedDataException;
import ru.practicum.ewm.main.exceptions.NotFoundException;
import ru.practicum.ewm.main.mapper.CategoryMapper;
import ru.practicum.ewm.main.model.Category;
import ru.practicum.ewm.main.repository.CategoryRepository;

import java.util.Collection;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;

    public CategoryDto create(NewCategoryDto newCategory) {
        if (repository.findByName(newCategory.getName()).isPresent()) {
            log.warn("Выброшено исключение DuplicatedDataException, категория с указанным названием: {} уже существует.", newCategory.getName());
            throw new DuplicatedDataException("Категория с указанным названием уже существует.", "Было нарушено ограничение целостности");
        }
        final Category category = repository.save(mapper.toCategory(newCategory));
        log.info("Категория успешно сохранена.");
        return mapper.toCategoryDto(category);
    }

    public void deleteCat(int catId) {
        repository.findById(catId).
                orElseThrow(() -> new NotFoundException("Категория с id: " + catId + " не найдена.", "Искомый объект не был найден."));
        repository.deleteById(catId);
        log.info("Категория с id {} удалена.", catId);
    }

    public CategoryDto updateCat(int catId, NewCategoryDto newCategory) {
        final Category cat = repository.findById(catId).
                orElseThrow(() -> new NotFoundException("Категория с id: " + catId + " не найдена.", "Искомый объект не был найден."));
        if (cat.getName().equals(newCategory.getName()))
            return mapper.toCategoryDto(cat);
        if (repository.findByName(newCategory.getName()).isPresent()) {
            log.warn("Выброшено исключение DuplicatedDataException, категория с указанным названием: {} уже существует.", newCategory.getName());
            throw new DuplicatedDataException("Категория с указанным названием уже существует.", "Было нарушено ограничение целостности");
        }
        cat.setName(newCategory.getName());
        final Category updCategory = repository.save(cat);
        log.info("Категория с id {} обновлена.", catId);
        return mapper.toCategoryDto(updCategory);
    }

    public CategoryDto findById(int catId) {
        final Category category = repository.findById(catId).
                orElseThrow(() -> new NotFoundException("Категория с id: " + catId + " не найдена.", "Искомый объект не был найден."));
        log.info("Категория с id {} найдена.", catId);
        return mapper.toCategoryDto(category);
    }

    public Collection<CategoryDto> findCategories(int from, int size) {
        final PageRequest pageable = PageRequest.of(from, size, Sort.by("id").ascending());
        log.info("Поиск категорий c номера страницы {} и c количеством элементов на странице {}.", from, size);
        return mapper.toCategoryDtoList(repository.findAll(pageable).getContent());
    }
}
