package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage {
    private final JdbcTemplate jdbcTemplate;

    public List<Genre> findAll() {
        return jdbcTemplate.query(
                "SELECT genre_id, \"name\" FROM genres ORDER BY genre_id",
                (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name"))
        );
    }

    public Genre getById(int id) {
        try {
            Genre genre = jdbcTemplate.queryForObject(
                    "SELECT genre_id, \"name\" FROM genres WHERE genre_id = ?",
                    (rs, rowNum) -> new Genre(rs.getInt("genre_id"), rs.getString("name")),
                    id
            );
            if (genre == null) {
                throw new NotFoundException(ErrorCodes.GENRE_NOT_FOUND, "Жанр с id=%d не найден".formatted(id));
            }
            return genre;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(ErrorCodes.GENRE_NOT_FOUND, "Жанр с id=%d не найден".formatted(id));
        }
    }
}
