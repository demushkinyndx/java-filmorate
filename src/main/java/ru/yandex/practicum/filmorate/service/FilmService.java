package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.FilmStorageInterface;
import ru.yandex.practicum.filmorate.storage.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.UserStorageInterface;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class FilmService {
    private final FilmStorageInterface<Film> filmStorage;
    private final UserStorageInterface<User> userStorage;
    private final GenreDbStorage genreStorage;
    private final MpaDbStorage mpaStorage;

    public FilmService(@Qualifier("filmDbStorage") FilmStorageInterface<Film> filmStorage,
                       @Qualifier("userDbStorage") UserStorageInterface<User> userStorage,
                       GenreDbStorage genreStorage,
                       MpaDbStorage mpaStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.genreStorage = genreStorage;
        this.mpaStorage = mpaStorage;
    }

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film getById(int id) {
        return filmStorage.getById(id);
    }

    public Film create(Film film) {
        normalizeReferences(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        normalizeReferences(film);
        return filmStorage.update(film);
    }

    public void addLike(int filmId, int userId) {
        userStorage.getById(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        userStorage.getById(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopular(Integer limit) {
        return filmStorage.getPopular(limit);
    }

    private void normalizeReferences(Film film) {
        if (film == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_REQUEST, "Данные фильма не переданы");
        }
        if (film.getMpa() == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_REQUEST, "Рейтинг фильма должен быть указан");
        }
        film.setMpa(mpaStorage.getById(film.getMpa().getId()));
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            film.setGenres(new LinkedHashSet<>());
            return;
        }
        LinkedHashSet<Genre> normalizedGenres = film.getGenres().stream()
                .map(Genre::getId)
                .distinct()
                .sorted()
                .map(genreStorage::getById)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        film.setGenres(normalizedGenres);
    }
}
