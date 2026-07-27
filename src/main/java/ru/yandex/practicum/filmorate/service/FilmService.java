package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film getById(int id) {
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
        Film film = filmStorage.getById(filmId);
        userStorage.getById(userId);
        film.getLikes().add(userId);
    }

    public void removeLike(int filmId, int userId) {
        Film film = filmStorage.getById(filmId);
        userStorage.getById(userId);
        film.getLikes().remove(userId);
    }

    public List<Film> getPopular(Integer limit) {
        return filmStorage.findAll().stream()
                .sorted(Comparator
                        .comparingInt((Film film) -> film.getLikes().size())
                        .reversed()
                        .thenComparingInt(Film::getId))
                .limit(limit)
                .toList();
    }
}
