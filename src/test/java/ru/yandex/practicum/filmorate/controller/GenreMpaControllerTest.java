package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class GenreMpaControllerTest extends AbstractTestHttpClient {

    @Test
    @DisplayName("GET /genres возвращает справочник жанров")
    void shouldReturnGenres() throws Exception {
        ApiListResponse response = getJsonList("/genres");

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals(6, response.body().size());
        assertEquals(1, ((Number) response.body().getFirst().get("id")).intValue());
    }

    @Test
    @DisplayName("GET /genres/{id} возвращает жанр")
    void shouldReturnGenreById() throws Exception {
        ApiResponse response = getJsonObject("/genres/1");

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals(1, ((Number) response.body().get("id")).intValue());
        assertEquals("Комедия", response.body().get("name"));
    }

    @Test
    @DisplayName("GET /mpa возвращает 5 рейтингов")
    void shouldReturnMpaRatings() throws Exception {
        ApiListResponse response = getJsonList("/mpa");

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals(5, response.body().size());
        List<Integer> ids = response.body().stream()
                .map(item -> ((Number) item.get("id")).intValue())
                .sorted()
                .toList();
        assertEquals(List.of(1, 2, 3, 4, 5), ids);
    }

    @Test
    @DisplayName("GET /mpa/{id} возвращает рейтинг")
    void shouldReturnMpaById() throws Exception {
        ApiResponse response = getJsonObject("/mpa/1");

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals(1, ((Number) response.body().get("id")).intValue());
        assertEquals("G", response.body().get("name"));
    }

    @Test
    @DisplayName("GET /mpa/{id} на несуществующий рейтинг возвращаем 404")
    void shouldReturn404ForUnknownMpa() throws Exception {
        ApiResponse response = getJsonObject("/mpa/999");

        assertEquals(HttpStatus.NOT_FOUND.value(), response.statusCode());
        assertEquals(ErrorCodes.MPA_NOT_FOUND, response.body().get("code"));
    }
}
