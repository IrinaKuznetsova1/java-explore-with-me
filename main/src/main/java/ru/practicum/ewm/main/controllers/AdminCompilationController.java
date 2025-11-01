package ru.practicum.ewm.main.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.ewm.main.dto.responses.CompilationDto;
import ru.practicum.ewm.main.dto.newRequests.NewCompilation;
import ru.practicum.ewm.main.dto.updateRequests.UpdateCompilationAdminRequest;
import ru.practicum.ewm.main.services.CompilationService;

@RestController
@RequestMapping("/admin/compilations")
@RequiredArgsConstructor
@Validated
@Slf4j
public class AdminCompilationController {
    private final CompilationService compilationService;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public CompilationDto createCompilation(@Valid @RequestBody @NotNull NewCompilation newCompilation) {
        log.info("Получен запрос POST/admin/compilations.");
        return compilationService.createCompilation(newCompilation);
    }

    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{compId}")
    public void deleteCompilation(@PathVariable @Min(1) long compId) {
        log.info("Получен запрос DELETE/admin/compilations/{}.", compId);
        compilationService.deleteCompilationById(compId);
    }

    @PatchMapping("/{compId}")
    public CompilationDto updateCompilation(@PathVariable @Min(1) long compId,
                                            @Valid @RequestBody @NotNull UpdateCompilationAdminRequest request) {
        log.info("Получен запрос PATCH/admin/compilations/{}.", compId);
        return compilationService.updateCompilationByAdmin(compId, request);
    }
}
