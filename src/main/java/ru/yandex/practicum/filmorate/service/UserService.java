package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.UserStorageInterface;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorageInterface<User> userStorage;

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public void normalize(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }

    public User getById(int id) {
        validatePositiveId(id, ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
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
            user.setFriends(new LinkedHashSet<>(existingUser.getFriends()));
        }
        return userStorage.update(user);
    }

    public void addFriend(int id, int friendId) {
        validatePositiveId(id, ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        validatePositiveId(friendId, ErrorCodes.USER_ID_INVALID, "Id друга должен быть положительным");
        User user = userStorage.getById(id);
        User friend = userStorage.getById(friendId);
        user.getFriends().add(friendId);
        friend.getFriends().add(id);
    }

    public void removeFriend(int id, int friendId) {
        validatePositiveId(id, ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        validatePositiveId(friendId, ErrorCodes.USER_ID_INVALID, "Id друга должен быть положительным");
        User user = userStorage.getById(id);
        User friend = userStorage.getById(friendId);
        user.getFriends().remove(friendId);
        friend.getFriends().remove(id);
    }

    public List<User> getFriends(int id) {
        validatePositiveId(id, ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        User user = userStorage.getById(id);
        return mapFriendIdsToUsers(user.getFriends());
    }

    public List<User> getCommonFriends(int id, int otherId) {
        validatePositiveId(id, ErrorCodes.USER_ID_INVALID, "Id пользователя должен быть положительным");
        validatePositiveId(otherId, ErrorCodes.USER_ID_INVALID, "Id другого пользователя должен быть положительным");
        User user = userStorage.getById(id);
        User otherUser = userStorage.getById(otherId);

        Set<Integer> otherFriends = otherUser.getFriends();
        return mapFriendIdsToUsers(
                user.getFriends().stream()
                        .filter(otherFriends::contains)
                        .collect(Collectors.toSet())
        );
    }

    private List<User> mapFriendIdsToUsers(Set<Integer> friendIds) {
        return friendIds.stream()
                .map(userStorage::getById)
                .toList();
    }

    private void validatePositiveId(int id, String code, String message) {
        if (id <= 0) {
            throw new ValidationException(code, message);
        }
    }
}
