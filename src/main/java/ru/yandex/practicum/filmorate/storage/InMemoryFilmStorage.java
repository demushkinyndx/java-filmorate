package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class InMemoryFilmStorage implements FilmStorageInterface<Film> {
    private final Map<Integer, Film> films = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public Collection<Film> findAll() {
        return List.copyOf(films.values());
    }

    @Override
    public Film getById(int id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException(ErrorCodes.FILM_NOT_FOUND, "Фильм с id=%d не найден".formatted(id));
        }
        return film;
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
        getById(film.getId());

        films.put(film.getId(), film);
        return film;
    }
}
