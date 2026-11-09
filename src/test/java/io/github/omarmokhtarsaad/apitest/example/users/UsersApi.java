package io.github.omarmokhtarsaad.apitest.example.users;

import io.github.omarmokhtarsaad.apitest.framework.http.ApiClient;
import io.qameta.allure.Step;
import io.restassured.response.Response;
import java.util.Objects;

/**
 * The Users resource. One method per endpoint; each returns the raw {@link Response} so the test
 * decides what to assert, including error statuses. No assertions live here.
 */
public final class UsersApi {

    static final String USERS = "/users";
    static final String USER = "/users/{id}";

    private final ApiClient client;

    public UsersApi(ApiClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Step("GET /users/{id}")
    public Response get(long id) {
        return client.request().pathParam("id", id).get(USER);
    }

    @Step("GET /users?page={page}")
    public Response list(int page) {
        return client.request().queryParam("page", page).get(USERS);
    }

    @Step("POST /users")
    public Response create(CreateUserRequest body) {
        return client.request().body(body).post(USERS);
    }

    @Step("PUT /users/{id}")
    public Response replace(long id, CreateUserRequest body) {
        return client.request().pathParam("id", id).body(body).put(USER);
    }

    @Step("DELETE /users/{id}")
    public Response delete(long id) {
        return client.request().pathParam("id", id).delete(USER);
    }
}
