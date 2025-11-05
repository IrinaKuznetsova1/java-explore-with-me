package ru.practicum.ewm.main.dto.newRequests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NewCategoryDto {
    @NotBlank(message = "поле 'name' не должно быть null или быть пустым.")
    @Size(min = 1, max = 50, message = "количество символов поля 'name' - от 1 до 50.")
    private String name;
}
