package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import lombok.Data;
import ru.yandex.practicum.filmorate.validation.user.LoginWithoutSpaces;

import java.time.LocalDate;

@Data
public class User {
    private int id;

    @NotBlank(message = "Email не может быть пустым и должен содержать символ @")
    @Email(message = "Email не может быть пустым и должен содержать символ @")
    private String email;

    @NotBlank(message = "Логин не может быть пустым или содержать пробелы")
    @LoginWithoutSpaces
    private String login;

    private String name;

    @NotNull(message = "Дата рождения должна быть указана")
    @PastOrPresent(message = "Дата рождения не может быть в будущем")
    private LocalDate birthday;
}
