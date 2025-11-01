package ru.practicum.ewm.main.controllers;

import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.responses.CategoryDto;
import ru.practicum.ewm.main.services.CategoryService;

import java.util.Collection;

@RestController
@RequestMapping(path = "/categories")
@RequiredArgsConstructor
@Validated
@Slf4j
public class PublicCategoryController {
    private final CategoryService service;

    @GetMapping("/{catId}")
    public CategoryDto findById(@PathVariable @Min(1) int catId) {
        log.info("Получен запрос GET/categories/{}.", catId);
        return service.findById(catId);
    }

    @GetMapping
    public Collection<CategoryDto> findCategories(@RequestParam(defaultValue = "0") @Min(0) int from,
                                                  @RequestParam(defaultValue = "10") @Min(1) int size) {
        log.info("Получен запрос GET/categories?from={}&size={}", from, size);
        return service.findCategories(from, size);
    }
}
