package ru.yandex.practicum.filmorate.validation.user;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

@Component
public class UserNormalizationService {

    /* имя для отображения может быть пустым — в таком случае будет использован логин */
    public void normalize(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
    }
}
