package ru.yandex.practicum.filmorate.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleValidationException(final ValidationException e) {
        HttpStatus status = resolveStatus(e.getCode());
        log.warn("Ошибка валидации: {}", e.getMessage());
        return ResponseEntity.status(status).body(errorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Некорректные данные запроса")
                .orElse("Некорректные данные запроса");

        log.warn("Ошибка валидации тела запроса: {}", message);
        return errorResponse(ErrorCodes.VALIDATION_REQUEST, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleHttpMessageNotReadableException(final HttpMessageNotReadableException e) {
        String message;
        String code;
        if (isEmptyRequestBody(e)) {
            message = "Тело запроса не может быть пустым";
            code = ErrorCodes.REQUEST_BODY_MISSING;
        } else {
            message = "Некорректный формат тела запроса";
            code = ErrorCodes.REQUEST_BODY_INVALID_FORMAT;
        }
        log.warn("Ошибка чтения тела запроса: {}", message, e);
        return errorResponse(code, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleUnexpectedException(final Exception e) {
        log.error("Непредвиденная ошибка сервера", e);
        return errorResponse(ErrorCodes.INTERNAL_ERROR, "Внутренняя ошибка сервера");
    }

    private HttpStatus resolveStatus(String code) {
        return switch (code) {
            case ErrorCodes.USER_NOT_FOUND, ErrorCodes.FILM_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case ErrorCodes.USER_LOGIN_DUPLICATE -> HttpStatus.CONFLICT;
            default -> HttpStatus.BAD_REQUEST;
        };
    }

    private boolean isEmptyRequestBody(HttpMessageNotReadableException e) {
        if (e.getCause() == null) {
            return true;
        }
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("No content to map due to end-of-input");
    }

    private Map<String, Object> errorResponse(String code, String message) {
        return Map.of(
                "code", code,
                "error", message
        );
    }
}
