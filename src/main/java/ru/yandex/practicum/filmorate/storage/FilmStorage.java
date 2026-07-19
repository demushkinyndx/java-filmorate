package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class FilmStorage implements Storage<Film> {
    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public Collection<Film> findAll() {
        return films.values();
    }

    @Override
    public Film create(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        if (film.getId() <= 0) {
            throw new ValidationException(ErrorCodes.FILM_ID_INVALID, "Id фильма должен быть положительным");
        }
        if (!films.containsKey(film.getId())) {
            throw new ValidationException(ErrorCodes.FILM_NOT_FOUND, "Фильм с id=%d не найден".formatted(film.getId()));
        }

        films.put(film.getId(), film);
        return film;
    }
}
