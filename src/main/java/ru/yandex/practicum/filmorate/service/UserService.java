package ru.yandex.practicum.filmorate.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorageInterface;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

@Service
public class UserService {
    private final UserStorageInterface<User> userStorage;

    public UserService(@Qualifier("userDbStorage") UserStorageInterface<User> userStorage) {
        this.userStorage = userStorage;
    }

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public void normalize(User user) {
        if (user == null) {
            throw new ValidationException(ErrorCodes.VALIDATION_REQUEST, "Данные пользователя не переданы");
        }
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    public User getById(int id) {
        return userStorage.getById(id);
    }

    public User create(User user) {
        normalize(user);
        return userStorage.create(user);
    }

    public User update(User user) {
        normalize(user);
        if (user.getId() > 0) {
            User existingUser = userStorage.getById(user.getId());
            if (existingUser.getFriends() == null) {
                user.setFriends(new LinkedHashSet<>());
            } else {
                user.setFriends(new LinkedHashSet<>(existingUser.getFriends()));
            }
        }
        return userStorage.update(user);
    }

    public void addFriend(int id, int friendId) {
        if (id == friendId) {
            throw new ValidationException(
                    ErrorCodes.VALIDATION_REQUEST,
                    "Нельзя добавлять самого себя в друзья"
            );
        }
        userStorage.addFriend(id, friendId);
    }

    public void removeFriend(int id, int friendId) {
        userStorage.removeFriend(id, friendId);
    }

    public List<User> getFriends(int id) {
        return userStorage.getFriends(id);
    }

    public List<User> getCommonFriends(int id, int otherId) {
        return userStorage.getCommonFriends(id, otherId);
    }
}
