package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;

import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserControllerValidationTest extends AbstractTestHttpClient {

    @Test
    @DisplayName("Создание пользователя ок")
    void shouldCreateUserWhenDataIsValid() throws Exception {
        String body = "{\"email\":\"user@mail.com\",\"login\":\"login\",\"name\":\"User\",\"birthday\":\"2000-01-01\"}";

        ApiResponse response = sendJson("POST", "/users", body);
        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals("user@mail.com", response.body().get("email"));
        assertTrue(response.body().containsKey("id"));
        assertInstanceOf(Number.class, response.body().get("id"));
    }

    @Test
    @DisplayName("Пустой email не проходит")
    void shouldRejectBlankEmail() throws Exception {
        String body = "{\"email\":\"\",\"login\":\"login\",\"name\":\"User\",\"birthday\":\"2000-01-01\"}";

        ApiResponse response = sendJson("POST", "/users", body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Email не может быть пустым и должен содержать символ @", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Email без @ не проходит")
    void shouldRejectEmailWithoutAtSign() throws Exception {
        String body = "{\"email\":\"usermail.com\",\"login\":\"login\",\"name\":\"User\",\"birthday\":\"2000-01-01\"}";

        ApiResponse response = sendJson("POST", "/users", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Email не может быть пустым и должен содержать символ @", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Неправильный email")
    void shouldRejectInvalidEmailFormat() throws Exception {
        String body = "{\"email\":\"это-неправильный?эмейл@.\",\"login\":\"login\",\"name\":\"User\",\"birthday\":\"2000-01-01\"}";

        ApiResponse response = sendJson("POST", "/users", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Email не может быть пустым и должен содержать символ @", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Логин с пробелом не проходит")
    void shouldRejectLoginWithSpaces() throws Exception {
        String body = "{\"email\":\"user@mail.com\",\"login\":\"my login\",\"name\":\"User\",\"birthday\":\"2000-01-01\"}";

        ApiResponse response = sendJson("POST", "/users", body);
        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Логин не может быть пустым или содержать пробелы", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("Нормализация пустого имени")
    void shouldSetLoginAsNameWhenNameIsBlank() throws Exception {
        String body = "{\"email\":\"user@gmail.com\",\"login\":\"mylogin\",\"name\":\" \",\"birthday\":\"2000-01-01\"}";

        ApiResponse response = sendJson("POST", "/users", body);

        assertEquals(HttpStatus.OK.value(), response.statusCode());
        assertEquals("mylogin", response.body().get("name"));
    }

    @Test
    @DisplayName("Дата рождения из будущего не проходит")
    void shouldRejectFutureBirthday() throws Exception {
        String futureDate = LocalDate.now().plusDays(1).toString();
        String body = String.format(
                "{\"email\":\"user@mail.com\",\"login\":\"mylogin\",\"name\":\"User\",\"birthday\":\"%s\"}",
                futureDate
        );

        ApiResponse response = sendJson("POST", "/users", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Дата рождения не может быть в будущем", response.body().get("error"));
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    @Test
    @DisplayName("400 на пустое тело запроса")
    void shouldReturnBadRequestForEmptyBody() throws Exception {
        ApiResponse response = sendJson("POST", "/users", "");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Тело запроса не может быть пустым", response.body().get("error"));
        assertEquals(ErrorCodes.REQUEST_BODY_MISSING, response.body().get("code"));
    }

    @Test
    @DisplayName("неверный формат даты - 400")
    void shouldReturnBadRequestForInvalidDateFormat() throws Exception {
        String body = "{\"email\":\"user@mail.com\",\"login\":\"mylogin\",\"name\":\"User\",\"birthday\":\"not-a-date\"}";

        ApiResponse response = sendJson("POST", "/users", body);

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals("Некорректный формат тела запроса", response.body().get("error"));
        assertEquals(ErrorCodes.REQUEST_BODY_INVALID_FORMAT, response.body().get("code"));
    }

    @Test
    @DisplayName("дублирующийся логин не проходит")
    void shouldRejectDuplicateLoginOnCreate() throws Exception {
        String firstUserBody = "{\"email\":\"first@mail.com\",\"login\":\"same-login\",\"name\":\"First\",\"birthday\":\"2000-01-01\"}";
        String secondUserBody = "{\"email\":\"second@mail.com\",\"login\":\"same-login\",\"name\":\"Second\",\"birthday\":\"2001-01-01\"}";

        ApiResponse firstResponse = sendJson("POST", "/users", firstUserBody);
        ApiResponse secondResponse = sendJson("POST", "/users", secondUserBody);

        assertEquals(HttpStatus.OK.value(), firstResponse.statusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), secondResponse.statusCode());

        assertEquals("Логин уже занят", secondResponse.body().get("error"));
        assertEquals(ErrorCodes.USER_LOGIN_DUPLICATE, secondResponse.body().get("code"));
    }


    @Test
    @DisplayName("Обновление с логином другого пользователя не проходит")
    void shouldRejectDuplicateLoginOnUpdate() throws Exception {
        String firstUserBody = "{\"email\":\"first-update@mail.com\",\"login\":\"first-login\",\"name\":\"First\",\"birthday\":\"2000-01-01\"}";
        String secondUserBody = "{\"email\":\"second-update@mail.com\",\"login\":\"second-login\",\"name\":\"Second\",\"birthday\":\"2001-01-01\"}";

        ApiResponse firstResponse = sendJson("POST", "/users", firstUserBody);
        ApiResponse secondResponse = sendJson("POST", "/users", secondUserBody);

        Number secondId = (Number) secondResponse.body().get("id");
        String duplicateUpdateBody = String.format(
                "{\"id\":%d,\"email\":\"second-update@mail.com\",\"login\":\"first-login\",\"name\":\"Second updated\",\"birthday\":\"2001-01-01\"}",
                secondId.intValue()
        );

        ApiResponse updateResponse = sendJson("PUT", "/users", duplicateUpdateBody);

        assertEquals(HttpStatus.OK.value(), firstResponse.statusCode());
        assertEquals(HttpStatus.OK.value(), secondResponse.statusCode());
        assertEquals(HttpStatus.BAD_REQUEST.value(), updateResponse.statusCode());
        assertEquals("Логин уже занят", updateResponse.body().get("error"));
        assertEquals(ErrorCodes.USER_LOGIN_DUPLICATE, updateResponse.body().get("code"));
    }

    @Test
    @DisplayName("Успешное обновление")
    void shouldUpdateExistingUser() throws Exception {
        String createBody = "{\"email\":\"before@mail.com\",\"login\":\"before-login\",\"name\":\"Before\",\"birthday\":\"2000-01-01\"}";

        ApiResponse createResponse = sendJson("POST", "/users", createBody);
        Number createdId = (Number) createResponse.body().get("id");
        String updateBody = String.format(
                "{\"id\":%d,\"email\":\"after@mail.com\",\"login\":\"after-login\",\"name\":\"After\",\"birthday\":\"2000-01-01\"}",
                createdId.intValue()
        );

        ApiResponse updateResponse = sendJson("PUT", "/users", updateBody);
        ApiListResponse usersResponse = getJsonList("/users");

        assertEquals(HttpStatus.OK.value(), updateResponse.statusCode());
        assertEquals(createdId.intValue(), updateResponse.body().get("id"));
        assertEquals("after@mail.com", updateResponse.body().get("email"));
        assertEquals("after-login", updateResponse.body().get("login"));


        assertEquals(HttpStatus.OK.value(), usersResponse.statusCode());
        Map<String, Object> updatedUser = usersResponse.body().stream()
                .filter(user -> createdId.intValue() == ((Number) user.get("id")).intValue())
                .findFirst()
                .orElse(null);

        assertNotNull(updatedUser, "Обновленный пользователь не найден в списке");
        assertEquals(createdId.intValue(), ((Number) updatedUser.get("id")).intValue());
        assertEquals("after@mail.com", updatedUser.get("email"));
        assertEquals("after-login", updatedUser.get("login"));
    }

    @Test
    @DisplayName("Обновление несуществующего пользователя - ошибка")
    void shouldRejectUpdateForUnknownUser() throws Exception {
        String updateBody = "{\"id\":99999,\"email\":\"unknown@mail.com\",\"login\":\"unknown-login\",\"name\":\"Unknown\",\"birthday\":\"2000-01-01\"}";

        ApiResponse updateResponse = sendJson("PUT", "/users", updateBody);
        assertEquals(HttpStatus.NOT_FOUND.value(), updateResponse.statusCode());
        assertEquals("Пользователь с id=99999 не найден", updateResponse.body().get("error"));
        assertEquals(ErrorCodes.USER_NOT_FOUND, updateResponse.body().get("code"));
    }
}
