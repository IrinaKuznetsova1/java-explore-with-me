package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.CategoryDto;
import ru.practicum.ewm.main.dto.NewCategoryDto;
import ru.practicum.ewm.main.services.CategoryService;

@RestController
@RequestMapping(path = "/admin/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCategoryController {
    private final CategoryService service;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CategoryDto create(@Valid @RequestBody NewCategoryDto newCategory) {
        log.info("Получен запрос POST/admin/categories.");
        return service.create(newCategory);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{catId}")
    public void deleteCat(@PathVariable @Min(1) int catId) {
        log.info("Получен запрос DELETE/admin/categories/{}", catId);
        service.deleteCat(catId);
    }

    @PatchMapping("/{catId}")
    public CategoryDto updateCat(@PathVariable @Min(1) int catId,
                                 @Valid @RequestBody NewCategoryDto newCategory) {
        log.info("Получен запрос PATCH/admin/categories/{}", catId);
        return service.updateCat(catId, newCategory);
    }
}
