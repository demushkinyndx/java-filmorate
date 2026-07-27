package ru.yandex.practicum.filmorate.storage;

import java.util.Collection;

public interface UserStorageInterface<T> {
    Collection<T> findAll();

    T getById(int id);

    T create(T entity);

    T update(T entity);
}
