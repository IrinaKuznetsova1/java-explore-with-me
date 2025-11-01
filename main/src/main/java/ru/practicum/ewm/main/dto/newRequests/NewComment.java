package ru.practicum.ewm.main.dto.newRequests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NewComment {
    @NotBlank(message = "поле 'text' не должно быть null или быть пустым.")
    @Size(max = 2000, message = "количество символов поля 'text' -  2000.")
    private String text;
}
