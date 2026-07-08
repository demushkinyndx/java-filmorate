package ru.yandex.practicum.filmorate.validation.user;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LoginWithoutSpacesValidator implements ConstraintValidator<LoginWithoutSpaces, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return value.chars().noneMatch(Character::isWhitespace);
    }
}
