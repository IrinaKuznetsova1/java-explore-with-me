package ru.practicum.ewm.main.dto.responses;

import lombok.*;

@Data
@AllArgsConstructor
public class UserDto {
    private long id;
    private String email;
    private String name;
}
