package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FilmDataTest extends AbstractTestHttpClient {


    @Test
    @DisplayName("Проверка кодов ошибок в лайках")
    void shouldHandleLikeEndpointsWithCodes() throws Exception {
        int userId = createUser("vova@yandex.ru", "vova");
        int filmId = createFilm("Истина в вине");

        ApiResponse addLike = sendJson("PUT", "/films/" + filmId + "/like/" + userId, "");
        ApiResponse removeLike = sendJson("DELETE", "/films/" + filmId + "/like/" + userId, "");
        ApiResponse addLikeUnknownFilm = sendJson("PUT", "/films/" + (filmId + 1) + "/like/" + userId, "");
        ApiResponse addLikeUnknownUser = sendJson("PUT", "/films/" + filmId + "/like/" + (userId + 1), "");
        ApiResponse removeLikeUnknownFilm = sendJson("DELETE", "/films/" + (filmId + 2) + "/like/" + userId, "");
        ApiResponse removeLikeUnknownUser = sendJson("DELETE", "/films/" + filmId + "/like/" + (userId + 2), "");

        assertEquals(HttpStatus.OK.value(), addLike.statusCode());
        assertEquals(HttpStatus.OK.value(), removeLike.statusCode());
        assertEquals(HttpStatus.NOT_FOUND.value(), addLikeUnknownFilm.statusCode());
        assertEquals(ErrorCodes.FILM_NOT_FOUND, addLikeUnknownFilm.body().get("code"));
        assertEquals(HttpStatus.NOT_FOUND.value(), addLikeUnknownUser.statusCode());
        assertEquals(ErrorCodes.USER_NOT_FOUND, addLikeUnknownUser.body().get("code"));
        assertEquals(HttpStatus.NOT_FOUND.value(), removeLikeUnknownFilm.statusCode());
        assertEquals(ErrorCodes.FILM_NOT_FOUND, removeLikeUnknownFilm.body().get("code"));
        assertEquals(HttpStatus.NOT_FOUND.value(), removeLikeUnknownUser.statusCode());
        assertEquals(ErrorCodes.USER_NOT_FOUND, removeLikeUnknownUser.body().get("code"));
    }

    private int createUser(String email, String login) throws Exception {
        String body = "{\"email\":\"%s\",\"login\":\"%s\",\"name\":\"Name\",\"birthday\":\"2000-01-01\"}"
                .formatted(email, login);
        ApiResponse response = sendJson("POST", "/users", body);
        return ((Number) response.body().get("id")).intValue();
    }

    private int createFilm(String name) throws Exception {
        String body = "{\"name\":\"%s\",\"description\":\"desc\",\"releaseDate\":\"2000-01-01\",\"duration\":120}"
                .formatted(name);
        ApiResponse response = sendJson("POST", "/films", body);
        return ((Number) response.body().get("id")).intValue();
    }

    private int findFilmIndexById(List<Map<String, Object>> films, int filmId) {
        for (int i = 0; i < films.size(); i++) {
            if (((Number) films.get(i).get("id")).intValue() == filmId) {
                return i;
            }
        }
        throw new IllegalStateException("Фильм с id=%d не найден в выдаче popular".formatted(filmId));
    }
}
