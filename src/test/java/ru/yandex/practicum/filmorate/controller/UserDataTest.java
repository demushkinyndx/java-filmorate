package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import ru.yandex.practicum.filmorate.exception.ErrorCodes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class UserDataTest extends AbstractTestHttpClient {

    @Test
    @DisplayName("Взаимное добавление друзей")
    void shouldAddFriendMutual() throws Exception {
        int vovaId = createUser("vova@yandex.ru", "vova");
        int mashaId = createUser("masha@mail.ru", "masha");

        ApiResponse addFriendResponse = sendJson("PUT", "/users/" + vovaId + "/friends/" + mashaId, "");
        ApiListResponse userFriends = getJsonList("/users/" + vovaId + "/friends");
        ApiListResponse friendFriends = getJsonList("/users/" + mashaId + "/friends");

        assertEquals(HttpStatus.OK.value(), addFriendResponse.statusCode());
        assertEquals(1, userFriends.body().size());
        assertEquals(mashaId, ((Number) userFriends.body().getFirst().get("id")).intValue());
        assertEquals(1, friendFriends.body().size());
        assertEquals(vovaId, ((Number) friendFriends.body().getFirst().get("id")).intValue());
    }

    @Test
    @DisplayName("Поудалять друзей взаимно и отвечать 200 OK")
    void shouldRemoveFriendSymmetrically() throws Exception {
        int userId = createUser("vova@yandex.ru", "vova");
        int friendId = createUser("masha@mail.ru", "masha");
        sendJson("PUT", "/users/" + userId + "/friends/" + friendId, "");

        ApiResponse removeFriendResponse = sendJson("DELETE", "/users/" + userId + "/friends/" + friendId, "");
        ApiListResponse userFriends = getJsonList("/users/" + userId + "/friends");
        ApiListResponse friendFriends = getJsonList("/users/" + friendId + "/friends");

        assertEquals(HttpStatus.OK.value(), removeFriendResponse.statusCode());
        assertTrue(userFriends.body().isEmpty());
        assertTrue(friendFriends.body().isEmpty());
    }

    @Test
    @DisplayName("Не падать на удалении несуществующей дружбы")
    void shouldRetunOkRemovingMissingFriend() throws Exception {
        int userId = createUser("vova@yandex.ru", "vova");
        int friendId = createUser("masha@mail.ru", "masha");

        ApiResponse removeFriendResponse = sendJson("DELETE", "/users/" + userId + "/friends/" + friendId, "");
        ApiListResponse userFriends = getJsonList("/users/" + userId + "/friends");
        ApiListResponse friendFriends = getJsonList("/users/" + friendId + "/friends");

        assertEquals(HttpStatus.OK.value(), removeFriendResponse.statusCode());
        assertTrue(userFriends.body().isEmpty());
        assertTrue(friendFriends.body().isEmpty());
    }

    @Test
    @DisplayName("404 на несуществующих пользователей")
    void shouldReturn404ForUnknownFriends() throws Exception {
        int userId = createUser("vova@yandex.ru", "vova");
        int friendId = createUser("masha@mail.ru", "masha");

        ApiResponse addUnknownFriend = sendJson("PUT", "/users/" + userId + "/friends/" + (friendId + 1), "");
        ApiResponse getUnknownUserFriends = getJsonObject("/users/" + (userId + 2) + "/friends");
        ApiResponse removeUnknownUser = sendJson("DELETE", "/users/" + (userId + 3) + "/friends/" + friendId, "");
        ApiResponse removeUnknownFriend = sendJson("DELETE", "/users/" + userId + "/friends/" + (friendId + 2), "");

        assertEquals(HttpStatus.NOT_FOUND.value(), addUnknownFriend.statusCode());
        assertEquals(ErrorCodes.USER_NOT_FOUND, addUnknownFriend.body().get("code"));
        assertEquals(HttpStatus.NOT_FOUND.value(), getUnknownUserFriends.statusCode());
        assertEquals(ErrorCodes.USER_NOT_FOUND, getUnknownUserFriends.body().get("code"));
        assertEquals(HttpStatus.NOT_FOUND.value(), removeUnknownUser.statusCode());
        assertEquals(ErrorCodes.USER_NOT_FOUND, removeUnknownUser.body().get("code"));
        assertEquals(HttpStatus.NOT_FOUND.value(), removeUnknownFriend.statusCode());
        assertEquals(ErrorCodes.USER_NOT_FOUND, removeUnknownFriend.body().get("code"));
    }

    @Test
    @DisplayName("Добавление и вывод друзей")
    void shouldReturnFriends() throws Exception {
        int user1Id = createUser("vova@yandex.ru", "vova");
        int user2Id = createUser("masha@mail.ru", "masha");
        int user3Id = createUser("leo@spamcop.net", "leo");

        sendJson("PUT", "/users/" + user1Id + "/friends/" + user3Id, "");
        sendJson("PUT", "/users/" + user1Id + "/friends/" + user2Id, "");
        sendJson("PUT", "/users/" + user2Id + "/friends/" + user3Id, "");

        ApiListResponse commonFriends = getJsonList("/users/" + user1Id + "/friends/common/" + user2Id);
        assertEquals(HttpStatus.OK.value(), commonFriends.statusCode());
        assertEquals(1, commonFriends.body().size());
        assertEquals(user3Id, ((Number) commonFriends.body().getFirst().get("id")).intValue());
    }

    @Test
    @DisplayName("Нельзя добавить самого себя в друзья")
    void shouldRejectAddingSelfAsFriend() throws Exception {
        int userId = createUser("vova@yandex.ru", "vova");

        ApiResponse response = sendJson("PUT", "/users/" + userId + "/friends/" + userId, "");

        assertEquals(HttpStatus.BAD_REQUEST.value(), response.statusCode());
        assertEquals(ErrorCodes.VALIDATION_REQUEST, response.body().get("code"));
    }

    private int createUser(String email, String login) throws Exception {
        String body = "{\"email\":\"%s\",\"login\":\"%s\",\"name\":\"Name\",\"birthday\":\"2000-01-01\"}"
                .formatted(email, login);
        ApiResponse response = sendJson("POST", "/users", body);
        return ((Number) response.body().get("id")).intValue();
    }
}
