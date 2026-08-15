package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;
import ru.yandex.practicum.filmorate.validation.film.ReleaseDateAfterCinemaStart;

import java.util.LinkedHashSet;
import java.time.LocalDate;
import java.util.Set;

@Data
public class Film {
    private int id;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    @Size(max = 200, message = "Описание фильма не может быть длиннее 200 символов")
    private String description;

    @NotNull(message = "Дата релиза должна быть указана")
    @ReleaseDateAfterCinemaStart
    private LocalDate releaseDate;

    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private int duration;

    @NotNull(message = "Рейтинг фильма должен быть указан")
    private Mpa mpa;

    private Set<Genre> genres = new LinkedHashSet<>();

    private Set<Integer> likes = new LinkedHashSet<>();
}
