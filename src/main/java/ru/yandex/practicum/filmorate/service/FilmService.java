package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorageInterface;
import ru.yandex.practicum.filmorate.storage.UserStorageInterface;

import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FilmService {
    private final FilmStorageInterface<Film> filmStorage;
    private final UserStorageInterface<User> userStorage;
    @Value("${filmorate.films.popular.default-count:10}")
    private int defaultPopularLimit;

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film getById(int id) {
        validatePositiveId(id, ErrorCodes.FILM_ID_INVALID, "Id фильма должен быть положительным");
        return filmStorage.getById(id);
    }

    public Film create(Film film) {
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        if (film.getId() > 0) {
            Film existingFilm = filmStorage.getById(film.getId());
            film.setLikes(new LinkedHashSet<>(existingFilm.getLikes()));
        }
        return filmStorage.update(film);
    }

    public void addLike(int filmId, int userId) {
        validatePositiveId(filmId, ErrorCodes.FILM_ID_INVALID, "Id фильма должен быть положительным");
        validatePositiveId(userId, ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        Film film = filmStorage.getById(filmId);
        userStorage.getById(userId);
        film.getLikes().add(userId);
    }

    public void removeLike(int filmId, int userId) {
        validatePositiveId(filmId, ErrorCodes.FILM_ID_INVALID, "Id фильма должен быть положительным");
        validatePositiveId(userId, ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        Film film = filmStorage.getById(filmId);
        userStorage.getById(userId);
        film.getLikes().remove(userId);
    }

    public List<Film> getPopular(Integer count) {
        int limit = count == null ? defaultPopularLimit : count;
        if (limit <= 0) {
            throw new ValidationException(ErrorCodes.VALIDATION_REQUEST, "Параметр count должен быть положительным");
        }

        return filmStorage.findAll().stream()
                .sorted(Comparator
                        .comparingInt((Film film) -> film.getLikes().size())
                        .reversed()
                        .thenComparingInt(Film::getId))
                .limit(limit)
                .toList();
    }

    private void validatePositiveId(int id, String code, String message) {
        if (id <= 0) {
            throw new ValidationException(code, message);
        }
    }
}
