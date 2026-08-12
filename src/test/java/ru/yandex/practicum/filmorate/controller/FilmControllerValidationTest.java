package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FilmControllerValidationTest extends AbstractTestHttpClient {

    @Test
    @DisplayName("Корректный фильм с граничной датой релиза создается")
    void shouldCreateFilmWhenDataIsValid() throws Exception {
        String body = String.format(
                "{\"name\":\"Film\",\"description\":\"%s\",\"releaseDate\":\"1895-12-28\",\"duration\":120,\"mpa\":{\"id\":1}}",
                "a".repeat(200)
        );

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals("Film", response.body().get("name"));
        assertTrue(response.body().containsKey("id"));
        assertInstanceOf(Number.class, response.body().get("id"));
    }

    @Test
    @DisplayName("Пустое название фильма не проходит валидацию")
    void shouldRejectBlankName() throws Exception {
        String body = "{\"name\":\" \",\"description\":\"desc\",\"releaseDate\":\"2000-01-01\",\"duration\":100,\"mpa\":{\"id\":1}}";

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Название фильма не может быть пустым", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Описание длиннее 200 символов не проходит валидацию")
    void shouldRejectTooLongDescription() throws Exception {
        String body = String.format(
                "{\"name\":\"Film\",\"description\":\"%s\",\"releaseDate\":\"2000-01-01\",\"duration\":100,\"mpa\":{\"id\":1}}",
                "a".repeat(201)
        );

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Описание фильма не может быть длиннее 200 символов", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Дата релиза раньше 28.12.1895 не проходит валидацию")
    void shouldRejectReleaseDateBeforeCinemaBirthday() throws Exception {
        String body = "{\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"1895-12-27\",\"duration\":100,\"mpa\":{\"id\":1}}";

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Неположительная продолжительность не проходит валидацию")
    void shouldRejectNonPositiveDuration() throws Exception {
        String body = "{\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"2000-01-01\",\"duration\":0,\"mpa\":{\"id\":1}}";

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Продолжительность фильма должна быть положительным числом", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Пустое тело запроса возвращает 400")
    void shouldReturnBadRequestForEmptyBody() throws Exception {
        ApiResponse response = sendJson("POST", "/films", "");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Тело запроса не может быть пустым", response.body().get("error"));
        assertEquals(ErrorCodes.REQUEST_BODY_MISSING, response.body().get("code"));
    }

    @Test
    @DisplayName("Некорректный формат даты возвращает 400")
    void shouldReturnBadRequestForInvalidDateFormat() throws Exception {
        String body = "{\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"not-a-date\",\"duration\":100,\"mpa\":{\"id\":1}}";

        ApiResponse response = sendJson("POST", "/films", body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());

        assertEquals("Некорректный формат тела запроса", response.body().get("error"));
        assertEquals(ErrorCodes.REQUEST_BODY_INVALID_FORMAT, response.body().get("code"));
    }

    @Test
    @DisplayName("Обновление несуществующего фильма возвращает 404")
    void shouldRejectUnknownFilmId() throws Exception {
        String body = "{\"id\":9999999,\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"2000-01-01\",\"duration\":100,\"mpa\":{\"id\":1}}";

        ApiResponse response = sendJson("PUT", "/films", body);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
        assertEquals("Фильм с id=9999999 не найден", response.body().get("error"));
        assertEquals(ErrorCodes.FILM_NOT_FOUND, response.body().get("code"));
    }
}
