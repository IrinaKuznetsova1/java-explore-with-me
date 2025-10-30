package ru.practicum.ewm.main.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Set;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class NewCompilation {
    @NotBlank(message = "Значения поля 'title' не должно быть null или быть пустым.")
    @Size(min = 3, max = 50, message = "количество символов поля 'title' - от 3 до 50.")
    private String title;
    @Builder.Default
    private Boolean pinned = false;
    private Set<Long> events;
}
