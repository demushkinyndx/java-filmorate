package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository("filmDbStorage")
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorageInterface<Film> {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<Film> findAll() {
        List<Film> films = jdbcTemplate.query("""
                        SELECT f.film_id, f.film_name, f.description, f.release_date, f.duration,
                               m.rating_id, m.rating_name
                        FROM films f
                        INNER JOIN mpa_ratings m ON m.rating_id = f.rating_id
                        ORDER BY f.film_id
                        """,
                (rs, rowNum) -> mapFilm(
                        rs.getInt("film_id"),
                        rs.getString("film_name"),
                        rs.getString("description"),
                        rs.getDate("release_date").toLocalDate(),
                        rs.getInt("duration"),
                        new Mpa(rs.getInt("rating_id"), rs.getString("rating_name"))
                )
        );
        films.forEach(this::loadFilmRelations);
        return films;
    }

    @Override
    public Film getById(int id) {
        try {
            Film film = jdbcTemplate.queryForObject("""
                            SELECT f.film_id, f.film_name, f.description, f.release_date, f.duration,
                                   m.rating_id, m.rating_name
                            FROM films f
                            INNER JOIN mpa_ratings m ON m.rating_id = f.rating_id
                            WHERE f.film_id = ?
                            """,
                    (rs, rowNum) -> mapFilm(
                            rs.getInt("film_id"),
                            rs.getString("film_name"),
                            rs.getString("description"),
                            rs.getDate("release_date").toLocalDate(),
                            rs.getInt("duration"),
                            new Mpa(rs.getInt("rating_id"), rs.getString("rating_name"))
                    ),
                    id
            );
            if (film == null) {
                throw new NotFoundException(ErrorCodes.FILM_NOT_FOUND, "Фильм с id=%d не найден".formatted(id));
            }
            loadFilmRelations(film);
            return film;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(ErrorCodes.FILM_NOT_FOUND, "Фильм с id=%d не найден".formatted(id));
        }
    }

    @Override
    public Film create(Film film) {
        validateFilmInput(film);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                            INSERT INTO films (film_name, description, release_date, duration, rating_id)
                            VALUES (?, ?, ?, ?, ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, film.getName());
            statement.setString(2, film.getDescription());
            statement.setDate(3, Date.valueOf(film.getReleaseDate()));
            statement.setInt(4, film.getDuration());
            statement.setInt(5, film.getMpa().getId());
            return statement;
        }, keyHolder);
        Number id = extractGeneratedId(keyHolder, "FILM_ID");
        if (id == null) {
            throw new IllegalStateException("Не удалось получить id сохраненного фильма");
        }
        int filmId = id.intValue();
        film.setId(filmId);
        saveGenres(filmId, film.getGenres());
        return getById(filmId);
    }

    @Override
    public Film update(Film film) {
        validateFilmInput(film);
        getById(film.getId());
        int updatedRows = jdbcTemplate.update("""
                        UPDATE films
                        SET film_name = ?, description = ?, release_date = ?, duration = ?, rating_id = ?
                        WHERE film_id = ?
                        """,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa().getId(),
                film.getId()
        );
        if (updatedRows == 0) {
            throw new NotFoundException(ErrorCodes.FILM_NOT_FOUND,
                    "Фильм с id=%d не найден".formatted(film.getId()));
        }
        saveGenres(film.getId(), film.getGenres());
        return getById(film.getId());
    }

    @Override
    public void addLike(int filmId, int userId) {
        getById(filmId);
        jdbcTemplate.update("""
                        INSERT INTO films_likes (film_id, user_id)
                        SELECT ?, ?
                        WHERE NOT EXISTS (
                            SELECT 1 FROM films_likes WHERE film_id = ? AND user_id = ?
                        )
                        """,
                filmId, userId, filmId, userId
        );
    }

    @Override
    public void removeLike(int filmId, int userId) {
        getById(filmId);
        jdbcTemplate.update("DELETE FROM films_likes WHERE film_id = ? AND user_id = ?", filmId, userId);
    }

    @Override
    public List<Film> getPopular(int count) {
        List<Integer> filmIds = jdbcTemplate.query("""
                        SELECT f.film_id
                        FROM films f
                        LEFT JOIN films_likes fl ON fl.film_id = f.film_id
                        GROUP BY f.film_id
                        ORDER BY COUNT(fl.user_id) DESC, f.film_id ASC
                        LIMIT ?
                        """,
                (rs, rowNum) -> rs.getInt("film_id"),
                count
        );
        return filmIds.stream()
                .map(this::getById)
                .toList();
    }

    private void loadFilmRelations(Film film) {
        film.setGenres(loadGenres(film.getId()));
        film.setLikes(loadLikes(film.getId()));
    }

    private Set<Genre> loadGenres(int filmId) {
        List<Genre> genres = jdbcTemplate.query("""
                        SELECT g.genre_id, g."name"
                        FROM films_genres fg
                        INNER JOIN genres g ON g.genre_id = fg.genre_id
                        WHERE fg.film_id = ?
                        ORDER BY g.genre_id
                        """,
                (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")),
                filmId
        );
        return new LinkedHashSet<>(genres);
    }

    private Set<Integer> loadLikes(int filmId) {
        List<Integer> likes = jdbcTemplate.query("""
                        SELECT user_id
                        FROM films_likes
                        WHERE film_id = ?
                        ORDER BY user_id
                        """,
                (rs, rowNum) -> rs.getInt("user_id"),
                filmId
        );
        return new LinkedHashSet<>(likes);
    }

    private void saveGenres(int filmId, Set<Genre> genres) {
        jdbcTemplate.update("DELETE FROM films_genres WHERE film_id = ?", filmId);
        if (genres == null || genres.isEmpty()) {
            return;
        }
        List<Integer> genreIds = genres.stream()
                .map(Genre::getId)
                .distinct()
                .sorted(Comparator.naturalOrder())
                .toList();
        jdbcTemplate.batchUpdate(
                "INSERT INTO films_genres (film_id, genre_id) VALUES (?, ?)",
                genreIds,
                genreIds.size(),
                (ps, genreId) -> {
                    ps.setInt(1, filmId);
                    ps.setInt(2, genreId);
                }
        );
    }

    private Film mapFilm(int id, String name, String description, java.time.LocalDate releaseDate, int duration, Mpa mpa) {
        Film film = new Film();
        film.setId(id);
        film.setName(name);
        film.setDescription(description);
        film.setReleaseDate(releaseDate);
        film.setDuration(duration);
        film.setMpa(mpa);
        film.setGenres(new LinkedHashSet<>());
        film.setLikes(new LinkedHashSet<>());
        return film;
    }

    private Number extractGeneratedId(KeyHolder keyHolder, String keyName) {
        Number id = keyHolder.getKey();
        if (id != null) {
            return id;
        }
        Map<String, Object> keys = keyHolder.getKeys();
        if (keys == null) {
            return null;
        }
        Object value = keys.get(keyName);
        if (!(value instanceof Number) && !keys.isEmpty()) {
            value = keys.values().iterator().next();
        }
        return value instanceof Number number ? number : null;
    }

    private void validateFilmInput(Film film) {
        if (film == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_REQUEST, "Данные фильма не переданы");
        }
        if (film.getReleaseDate() == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_REQUEST, "Дата релиза должна быть указана");
        }
        if (film.getMpa() == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_REQUEST, "Рейтинг фильма должен быть указан");
        }
    }
}
