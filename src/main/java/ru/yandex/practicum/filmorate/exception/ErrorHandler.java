package ru.yandex.practicum.filmorate.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ErrorHandler {
    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleValidationException(final ValidationException e) {
        log.warn("Ошибка валидации: {}", e.getMessage());
        return errorResponse(e.getCode(), e.getMessage());
    }

    @ExceptionHandler
    public ResponseEntity<Map<String, Object>> handleNotFoundException(final NotFoundException e) {
        log.warn("Объект не найден: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst().filter(error -> error.getDefaultMessage() != null).map(DefaultMessageSourceResolvable::getDefaultMessage).orElse("Некорректные данные запроса");

        log.warn("Ошибка валидации тела запроса: {}", message);
        return errorResponse(ErrorCodes.VALIDATION_REQUEST, message);
    }

    @ExceptionHandler
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleHandlerMethodValidationException(final HandlerMethodValidationException e) {
        String message = e.getAllErrors()
                .stream()
                .findFirst().filter(error -> error.getDefaultMessage() != null).map(MessageSourceResolvable::getDefaultMessage).orElse("Некорректные параметры запроса");
        log.warn("Ошибка валидации параметров запроса: {}", message);
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
