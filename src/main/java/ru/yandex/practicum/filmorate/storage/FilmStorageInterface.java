package ru.yandex.practicum.filmorate.storage;

import java.util.Collection;
import java.util.List;

public interface FilmStorageInterface<T> {
    Collection<T> findAll();

    T getById(int id);

    T create(T entity);

    T update(T entity);

    void addLike(int filmId, int userId);

    void removeLike(int filmId, int userId);

    List<T> getPopular(int count);
}
