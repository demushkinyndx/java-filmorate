package ru.yandex.practicum.filmorate.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.Storage;
import ru.yandex.practicum.filmorate.validation.user.UserNormalizationService;

import java.util.Collection;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserNormalizationService userNormalizationService;
    private final Storage<User> userStorage;

    @GetMapping
    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        userNormalizationService.normalize(user);
        userStorage.create(user);
        log.info("Создан пользователь: id={}, login='{}', email='{}'", user.getId(), user.getLogin(), user.getEmail());
        return user;
    }

    @PutMapping
    public User update(@Valid @RequestBody User user) {
        userNormalizationService.normalize(user);
        userStorage.update(user);
        log.info("Обновлен пользователь: id={}, login='{}', email='{}'", user.getId(), user.getLogin(), user.getEmail());
        return user;
    }
}
