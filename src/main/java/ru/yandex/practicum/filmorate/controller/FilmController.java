package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;

import java.util.Collection;
import java.util.List;

@Slf4j
@RestController
@Validated
@RequestMapping("/films")
@RequiredArgsConstructor

public class FilmController {
    private final FilmService filmService;

    @GetMapping
    public Collection<Film> findAll() {
        return filmService.findAll();
    }

    @GetMapping("/{id}")
    public Film getById(@PathVariable @Positive Integer id) {
        return filmService.getById(id);
    }

    @PostMapping
    public Film create(@Valid @RequestBody Film film) {
        Film createdFilm = filmService.create(film);
        log.info("Добавлен фильм: id={}, name='{}'", film.getId(), film.getName());
        return createdFilm;
    }

    @PutMapping
    public Film update(@Valid @RequestBody Film film) {
        Film updatedFilm = filmService.update(film);
        log.info("Обновлен фильм: id={}, name='{}'", film.getId(), film.getName());
        return updatedFilm;
    }

    @PutMapping("/{id}/like/{userId}")
    public void addLikeToFilm(@PathVariable @Positive Integer id, @PathVariable @Positive Integer userId) {
        filmService.addLike(id, userId);
    }

    @DeleteMapping("/{id}/like/{userId}")
    public void removeLike(@PathVariable @Positive Integer id, @PathVariable @Positive Integer userId) {
        filmService.removeLike(id, userId);
    }

    @GetMapping("/popular")
    public List<Film> getPopularFilms(
            @RequestParam(required = false, defaultValue = "${filmorate.films.popular.default-count:10}")
            @Positive Integer count) {
        return filmService.getPopular(count);
    }
}
