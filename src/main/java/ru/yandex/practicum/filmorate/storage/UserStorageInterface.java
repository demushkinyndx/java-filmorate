package ru.yandex.practicum.filmorate.storage;

import java.util.Collection;
import java.util.List;

public interface UserStorageInterface<T> {
    Collection<T> findAll();

    T getById(int id);

    T create(T entity);

    T update(T entity);

    void addFriend(int userId, int friendId);

    void removeFriend(int userId, int friendId);

    List<T> getFriends(int userId);

    List<T> getCommonFriends(int userId, int otherId);
}
