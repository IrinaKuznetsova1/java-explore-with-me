package ru.practicum.ewm.main.dto.newRequests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class NewUserRequest {
    @NotBlank(message = "поле 'email' не должно быть null или быть пустым.")
    @Email(message = "поле 'email' должно соответствовать формату адреса электронной почты.")
    @Size(min = 6, max = 254, message = "количество символов поля 'email' - от 6 до 254.")
    private String email;

    @NotBlank(message = "поле 'name' не должно быть null или быть пустым.")
    @Size(min = 2, max = 250, message = "количество символов поля 'name' - от 2 до 250.")
    private String name;
}
