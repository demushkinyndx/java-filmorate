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
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Repository("userDbStorage")
@RequiredArgsConstructor
public class UserDbStorage implements UserStorageInterface<User> {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Collection<User> findAll() {
        List<User> users = jdbcTemplate.query(
                "SELECT user_id, email, user_name, login, birthdate FROM users ORDER BY user_id",
                (rs, rowNum) -> mapUser(rs.getInt("user_id"), rs.getString("email"), rs.getString("user_name"),
                        rs.getString("login"), rs.getDate("birthdate").toLocalDate())
        );
        users.forEach(user -> user.setFriends(loadFriendIds(user.getId())));
        return users;
    }

    @Override
    public User getById(int id) {
        try {
            User user = jdbcTemplate.queryForObject(
                    "SELECT user_id, email, user_name, login, birthdate FROM users WHERE user_id = ?",
                    (rs, rowNum) -> mapUser(rs.getInt("user_id"), rs.getString("email"), rs.getString("user_name"),
                            rs.getString("login"), rs.getDate("birthdate").toLocalDate()),
                    id
            );
            if (user == null) {
                throw new NotFoundException(ErrorCodes.USER_NOT_FOUND,
                        "Пользователь с id=%d не найден".formatted(id));
            }
            user.setFriends(loadFriendIds(id));
            return user;
        } catch (EmptyResultDataAccessException e) {
            throw new NotFoundException(ErrorCodes.USER_NOT_FOUND,
                    "Пользователь с id=%d не найден".formatted(id));
        }
    }

    @Override
    public User create(User user) {
        validateUniqueLoginForCreate(user.getLogin());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO users (email, user_name, login, birthdate) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, user.getEmail());
            statement.setString(2, user.getName());
            statement.setString(3, user.getLogin());
            statement.setDate(4, Date.valueOf(user.getBirthday()));
            return statement;
        }, keyHolder);
        Number id = extractGeneratedId(keyHolder, "USER_ID");
        if (id == null) {
            throw new ValidationException(ErrorCodes.INTERNAL_ERROR, "Не удалось сохранить пользователя");
        }
        user.setId(id.intValue());
        user.setFriends(new LinkedHashSet<>());
        return user;
    }

    @Override
    public User update(User user) {
        if (user.getId() <= 0) {
            throw new ValidationException(ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        }
        getById(user.getId());
        validateUniqueLoginForUpdate(user);
        jdbcTemplate.update(
                "UPDATE users SET email = ?, user_name = ?, login = ?, birthdate = ? WHERE user_id = ?",
                user.getEmail(),
                user.getName(),
                user.getLogin(),
                Date.valueOf(user.getBirthday()),
                user.getId()
        );
        User updatedUser = getById(user.getId());
        if (updatedUser.getFriends() == null) {
            user.setFriends(new LinkedHashSet<>());
        } else {
            user.setFriends(updatedUser.getFriends());
        }
        return user;
    }

    @Override
    public void addFriend(int userId, int friendId) {
        getById(userId);
        getById(friendId);
        jdbcTemplate.update("""
                        INSERT INTO friends (user_id, friend_id, confirmed)
                        SELECT ?, ?, FALSE
                        WHERE NOT EXISTS (
                            SELECT 1 FROM friends WHERE user_id = ? AND friend_id = ?
                        )
                        """,
                userId, friendId, userId, friendId
        );
    }

    @Override
    public void removeFriend(int userId, int friendId) {
        getById(userId);
        getById(friendId);
        jdbcTemplate.update("DELETE FROM friends WHERE user_id = ? AND friend_id = ?", userId, friendId);
    }

    @Override
    public List<User> getFriends(int userId) {
        getById(userId);
        return jdbcTemplate.query("""
                        SELECT u.user_id, u.email, u.user_name, u.login, u.birthdate
                        FROM friends f
                        INNER JOIN users u ON u.user_id = f.friend_id
                        WHERE f.user_id = ?
                        ORDER BY u.user_id
                        """,
                (rs, rowNum) -> {
                    User user = mapUser(rs.getInt("user_id"), rs.getString("email"), rs.getString("user_name"),
                            rs.getString("login"), rs.getDate("birthdate").toLocalDate());
                    user.setFriends(loadFriendIds(user.getId()));
                    return user;
                },
                userId
        );
    }

    @Override
    public List<User> getCommonFriends(int userId, int otherId) {
        getById(userId);
        getById(otherId);
        return jdbcTemplate.query("""
                        SELECT u.user_id, u.email, u.user_name, u.login, u.birthdate
                        FROM friends f1
                        INNER JOIN friends f2 ON f1.friend_id = f2.friend_id
                        INNER JOIN users u ON u.user_id = f1.friend_id
                        WHERE f1.user_id = ? AND f2.user_id = ?
                        ORDER BY u.user_id
                        """,
                (rs, rowNum) -> {
                    User user = mapUser(rs.getInt("user_id"), rs.getString("email"), rs.getString("user_name"),
                            rs.getString("login"), rs.getDate("birthdate").toLocalDate());
                    user.setFriends(loadFriendIds(user.getId()));
                    return user;
                },
                userId, otherId
        );
    }

    private Set<Integer> loadFriendIds(int userId) {
        return jdbcTemplate.query(
                        "SELECT friend_id FROM friends WHERE user_id = ? ORDER BY friend_id",
                        (rs, rowNum) -> rs.getInt("friend_id"),
                        userId
                ).stream()
                .sorted(Comparator.naturalOrder())
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
    }

    private void validateUniqueLoginForCreate(String login) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE login = ?",
                Integer.class,
                login
        );
        if (count != null && count > 0) {
            throw new ValidationException(ErrorCodes.USER_LOGIN_DUPLICATE, "Логин уже занят");
        }
    }

    private void validateUniqueLoginForUpdate(User user) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM users WHERE login = ? AND user_id <> ?",
                Integer.class,
                user.getLogin(),
                user.getId()
        );
        if (count != null && count > 0) {
            throw new ValidationException(ErrorCodes.USER_LOGIN_DUPLICATE, "Логин уже занят");
        }
    }

    private User mapUser(int id, String email, String name, String login, java.time.LocalDate birthday) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName(name);
        user.setLogin(login);
        user.setBirthday(birthday);
        user.setFriends(new LinkedHashSet<>());
        return user;
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
}
