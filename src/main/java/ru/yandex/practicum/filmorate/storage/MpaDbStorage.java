package ru.yandex.practicum.filmorate.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage {
    private final JdbcTemplate jdbcTemplate;

    public List<Mpa> findAll() {
        return jdbcTemplate.query(
                "SELECT rating_id, rating_name FROM mpa_ratings ORDER BY rating_id",
                (rs, rowNum) -> new Mpa(rs.getInt("rating_id"), rs.getString("rating_name"))
        );
    }

    public Mpa getById(int id) {
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT rating_id, rating_name FROM mpa_ratings WHERE rating_id = ?",
                    (rs, rowNum) -> new Mpa(rs.getInt("rating_id"), rs.getString("rating_name")),
                    id
            );
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(ErrorCodes.MPA_NOT_FOUND, "Рейтинг с id=%d не найден".formatted(id));
        }
    }
}
