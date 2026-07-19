package ru.yandex.practicum.filmorate.storage;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class UserStorage implements Storage<User> {
    private final Map<Integer, User> users = new LinkedHashMap<>();
    private int nextId = 1;

    @Override
    public Collection<User> findAll() {
        return List.copyOf(users.values()); //чтобы внутреннюю коллекцию не меняли
    }

    @Override
    public User create(User user) {
        validateUniqueLoginForCreate(user.getLogin());
        user.setId(nextId++); //пока нет удаления пользователей нет смысла брать максимальный id из users

        users.put(user.getId(), user);
        return user;
    }

    @Override
    public User update(User user) {
        if (user.getId() <= 0) {
            throw new ValidationException(ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        }
        if (!users.containsKey(user.getId())) {
            throw new ValidationException(ErrorCodes.USER_NOT_FOUND, "Пользователь с id=%d не найден".formatted(user.getId()));
        }
        validateUniqueLoginForUpdate(user); //чтобы при апдейте не случилось два одинаковых логина

        users.put(user.getId(), user);
        return user;
    }

    private void validateUniqueLoginForCreate(String login) {
        boolean loginExists = users.values().stream()
                .anyMatch(existingUser -> existingUser.getLogin().equals(login));

        if (loginExists) {
            throw new ValidationException(ErrorCodes.USER_LOGIN_DUPLICATE, "Логин уже занят");
        }
    }

    private void validateUniqueLoginForUpdate(User user) {
        boolean loginExists = users.values().stream()
                .anyMatch(existingUser -> existingUser.getLogin().equals(user.getLogin())
                        && existingUser.getId() != user.getId());

        if (loginExists) {
            throw new ValidationException(ErrorCodes.USER_LOGIN_DUPLICATE, "Логин уже занят");
        }
    }
}
