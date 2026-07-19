package ru.yandex.practicum.filmorate.validation.film;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;

public class ReleaseDateAfterCinemaStartValidator implements ConstraintValidator<ReleaseDateAfterCinemaStart, LocalDate> {
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !value.isBefore(CINEMA_BIRTHDAY);
    }
}
