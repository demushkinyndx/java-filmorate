package ru.yandex.practicum.filmorate.storage;

import java.util.Collection;

public interface UserStorage<T> {
    Collection<T> findAll();

    T create(T entity);

    T update(T entity);
}
