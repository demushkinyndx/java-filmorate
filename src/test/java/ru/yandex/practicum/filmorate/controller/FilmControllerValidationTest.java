package ru.yandex.practicum.filmorate.controller;

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
        //Корректный фильм с датой-границей валидируется
    void shouldCreateFilmWhenDataIsValid() throws Exception {
        String body = String.format(
                "{\"name\":\"Film\",\"description\":\"%s\",\"releaseDate\":\"1895-12-28\",\"duration\":120}",
                "a".repeat(200)
        );

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals("Film", response.body().get("name"));
        assertTrue(response.body().containsKey("id"));
        assertInstanceOf(Number.class, response.body().get("id"));
    }

    @Test
        //пустое название фильма не проходит
    void shouldRejectBlankName() throws Exception {
        String body = "{\"name\":\" \",\"description\":\"desc\",\"releaseDate\":\"2000-01-01\",\"duration\":100}";

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Название фильма не может быть пустым", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
        //описание длиннее 200 символов не проходит
    void shouldRejectTooLongDescription() throws Exception {
        String body = String.format(
                "{\"name\":\"Film\",\"description\":\"%s\",\"releaseDate\":\"2000-01-01\",\"duration\":100}",
                "a".repeat(201)
        );

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Описание фильма не может быть длиннее 200 символов", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
        //дата релиза раньше 28.12.1895 не проходит
    void shouldRejectReleaseDateBeforeCinemaBirthday() throws Exception {
        String body = "{\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"1895-12-27\",\"duration\":100}";

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
        //неположительная продолжительность не проходит
    void shouldRejectNonPositiveDuration() throws Exception {
        String body = "{\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"2000-01-01\",\"duration\":0}";

        ApiResponse response = sendJson("POST", "/films", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Продолжительность фильма должна быть положительным числом", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
        //пустое тело запроса 400
    void shouldReturnBadRequestForEmptyBody() throws Exception {
        ApiResponse response = sendJson("POST", "/films", "");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Тело запроса не может быть пустым", response.body().get("error"));
        assertEquals(ErrorCodes.REQUEST_BODY_MISSING, response.body().get("code"));
    }

    @Test
        //кривой формат даты - ошибка
    void shouldReturnBadRequestForInvalidDateFormat() throws Exception {
        String body = "{\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"not-a-date\",\"duration\":100}";

        ApiResponse response = sendJson("POST", "/films", body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());

        assertEquals("Некорректный формат тела запроса", response.body().get("error"));
        assertEquals(ErrorCodes.REQUEST_BODY_INVALID_FORMAT, response.body().get("code"));
    }

    @Test
        //несуществующий id фильма
    void shouldRejectUnknownFilmId() throws Exception {
        String body = "{\"id\":9999999,\"name\":\"Film\",\"description\":\"desc\",\"releaseDate\":\"2000-01-01\",\"duration\":100}";

        ApiResponse response = sendJson("PUT", "/films", body);

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
        assertEquals("Фильм с id=9999999 не найден", response.body().get("error"));
        assertEquals(ErrorCodes.FILM_NOT_FOUND, response.body().get("code"));
    }
}
