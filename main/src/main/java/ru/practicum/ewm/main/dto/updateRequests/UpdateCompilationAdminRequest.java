package ru.practicum.ewm.main.dto.updateRequests;

import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

import java.util.Set;

@Builder
@Jacksonized
@Getter
public class UpdateCompilationAdminRequest {
    @Size(min = 3, max = 50, message = "количество символов поля 'title' - от 3 до 50.")
    private String title;
    private Boolean pinned;
    private Set<Long> events;
}
