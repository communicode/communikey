/*
 * This file is part of communikey.
 * Copyright (C) 2016-2018  communicode AG <communicode.de>
 *
 * communikey is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package de.communicode.communikey.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.equalTo;

import de.communicode.communikey.IntegrationBaseTest;
import de.communicode.communikey.controller.RequestMappings;
import de.communicode.communikey.domain.User;
import de.communicode.communikey.domain.UserGroup;
import de.communicode.communikey.service.payload.UserCreationPayload;
import io.restassured.http.ContentType;
import org.junit.Test;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests for the {@link UserGroup} REST API.
 *
 * @author sgreb@communicode.de
 * @since 0.4.0
 */
public class UserGroupApiIt extends IntegrationBaseTest {

    private Map<String, String> userGroupPayload = new HashMap<>();
    private User user = new User();

    @Test
    public void testAddUserAsAdmin() {
        initializeTestUserGroupPayload();
        initializeTestUser();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(new UserCreationPayload(user))
        .when()
            .post(RequestMappings.USERS + RequestMappings.USERS_REGISTER);

        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
            .param("login", user.getLogin())
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_USERS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("users")
            .body("size()", equalTo(1));
    }

    @Test
    public void testAddUserAsUser() {
        initializeTestUserGroupPayload();
        initializeTestUser();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(new UserCreationPayload(user))
        .when()
            .post(RequestMappings.USERS + RequestMappings.USERS_REGISTER);

        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(userOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
            .param("login", user.getLogin())
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_USERS)
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void testCreateAsAdmin() {
        initializeTestUserGroupPayload();
        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
        .when()
            .post(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.CREATED.value());
    }

    @Test
    public void testCreateAsUser() {
        initializeTestUserGroupPayload();
        given()
            .auth().oauth2(userOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
        .when()
            .post(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void testDeleteAsAdmin() {
        initializeTestUserGroupPayload();
        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .delete(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.NOT_FOUND.value());
    }

    @Test
    public void testDeleteAsUser() {
        initializeTestUserGroupPayload();
        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(userOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .delete(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void testDeleteAllAsAdmin() {
        initializeTestUserGroupPayload();
        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
        .when()
            .post(RequestMappings.USER_GROUPS);

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
        .when()
            .get(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", equalTo(1));

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
        .when()
            .delete(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.NO_CONTENT.value());

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
        .when()
            .get(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", equalTo(0));
    }

    @Test
    public void testDeleteAllAsUser() {
        initializeTestUserGroupPayload();
        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
        .when()
            .post(RequestMappings.USER_GROUPS);

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
        .when()
            .get(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", equalTo(1));

        given()
            .auth().oauth2(userOAuth2AccessToken)
            .when()
        .delete(RequestMappings.USER_GROUPS)
            .then()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
        .when()
            .get(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", equalTo(1));
    }

    @Test
    public void testGetAsAdmin() {
        initializeTestUserGroupPayload();
        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("id", equalTo(createdUserGroupId.intValue()));
    }

    @Test
    public void testGetAsUser() {
        initializeTestUserGroupPayload();
        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(userOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void testGetAllAsAdmin() {
        initializeTestUserGroupPayload();
        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
        .when()
            .post(RequestMappings.USER_GROUPS);

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
        .when()
            .get(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("size()", equalTo(1));
    }

    @Test
    public void testGetAllAsUser() {
        initializeTestUserGroupPayload();
        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
        .when()
            .post(RequestMappings.USER_GROUPS);

        given()
            .auth().oauth2(userOAuth2AccessToken)
        .when()
            .get(RequestMappings.USER_GROUPS)
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @Test
    public void testRemoveUserAsAdmin() {
        initializeTestUserGroupPayload();
        initializeTestUser();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(new UserCreationPayload(user))
        .when()
            .post(RequestMappings.USERS + RequestMappings.USERS_REGISTER);

        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
            .param("login", user.getLogin())
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_USERS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("users")
            .body("size()", equalTo(1));

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("login", user.getLogin())
            .param("login", user.getLogin())
        .when()
            .get(RequestMappings.USERS + RequestMappings.USERS_LOGIN)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("groups")
            .body("size()", equalTo(1));

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
            .param("login", user.getLogin())
        .when()
            .delete(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_USERS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("users")
            .body("size()", equalTo(0));

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("login", user.getLogin())
            .param("login", user.getLogin())
        .when()
            .get(RequestMappings.USERS + RequestMappings.USERS_LOGIN)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("groups")
            .body("size()", equalTo(0));
    }

    @Test
    public void testRemoveUserAsUser() {
        initializeTestUserGroupPayload();
        initializeTestUser();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(new UserCreationPayload(user))
            .when()
                .post(RequestMappings.USERS + RequestMappings.USERS_REGISTER);

        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
            .param("login", user.getLogin())
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_USERS)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("users")
            .body("size()", equalTo(1));

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("login", user.getLogin())
        .when()
            .get(RequestMappings.USERS + RequestMappings.USERS_LOGIN)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("groups")
            .body("size()", equalTo(1));

        given()
            .auth().oauth2(userOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
            .param("login", user.getLogin())
        .when()
            .delete(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_USERS)
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("users")
            .body("size()", equalTo(1));

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("login", user.getLogin())
        .when()
            .get(RequestMappings.USERS + RequestMappings.USERS_LOGIN)
        .then()
            .statusCode(HttpStatus.OK.value())
            .root("groups")
            .body("size()", equalTo(1));
    }

    @Test
    public void testUpdateAsAdmin() {
        initializeTestUserGroupPayload();
        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        String newUserGroupName = fairy.textProducer().word(1);
        userGroupPayload.replace("name", newUserGroupName);

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .pathParam("userGroupId", createdUserGroupId)
            .body(userGroupPayload)
        .when()
            .put(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.OK.value())
            .body("name", equalTo(newUserGroupName));

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Test
    public void testUpdateAsUser() {
        initializeTestUserGroupPayload();
        Long createdUserGroupId = given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .body(userGroupPayload)
            .when()
                .post(RequestMappings.USER_GROUPS)
            .then()
                .extract().body().as(UserGroup.class).getId();

        String newUserGroupName = fairy.textProducer().word(1);
        userGroupPayload.replace("name", newUserGroupName);

        given()
            .auth().oauth2(userOAuth2AccessToken)
            .contentType(ContentType.JSON)
            .pathParam("userGroupId", createdUserGroupId)
            .body(userGroupPayload)
        .when()
            .put(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.FORBIDDEN.value());

        given()
            .auth().oauth2(adminUserOAuth2AccessToken)
            .pathParam("userGroupId", createdUserGroupId)
        .when()
            .get(RequestMappings.USER_GROUPS + RequestMappings.USER_GROUPS_ID)
        .then()
            .statusCode(HttpStatus.OK.value());
    }

    /**
     * Initializes the test user group payload.
     */
    private void initializeTestUserGroupPayload() {
        userGroupPayload.put("name", fairy.textProducer().word(1));
    }

    /**
     * Initializes the test user.
     */
    private void initializeTestUser() {
        user.setFirstName(fairy.person().getFirstName());
        user.setEmail(user.getFirstName() + "@communicode.de");
        user.setLogin(user.getFirstName().toLowerCase());
        user.setPassword(fairy.person().getPassword());
        user.setLastName(fairy.person().getLastName());
    }
}
