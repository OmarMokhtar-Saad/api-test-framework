package io.github.omarmokhtarsaad.apitest.tests.users;

import static io.github.omarmokhtarsaad.apitest.framework.assertion.ApiAssertions.assertThat;

import io.github.omarmokhtarsaad.apitest.example.users.User;
import io.github.omarmokhtarsaad.apitest.example.users.UserFactory;
import io.github.omarmokhtarsaad.apitest.example.users.UsersApi;
import io.github.omarmokhtarsaad.apitest.framework.testng.BaseApiTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

/** Response shapes only; behaviour is covered by the CRUD and validation tests. */
@Feature("Users")
@Story("Contract")
public class UsersContractTest extends BaseApiTest {

    @Test(groups = "contract", description = "GET /users/{id} matches the User schema")
    public void getUserMatchesSchema() {
        assertThat(new UsersApi(api()).get(1)).hasStatus(200).matchesSchema("schemas/users/user.json");
    }

    @Test(groups = "contract", description = "POST /users response matches the User schema")
    public void createUserMatchesSchema() {
        UsersApi users = new UsersApi(api());
        Response response = users.create(UserFactory.validMember());
        assertThat(response).hasStatus(201);
        long id = assertThat(response).bodyAs(User.class).actual().id();
        onCleanup("delete user " + id, () -> users.delete(id));

        assertThat(response).matchesSchema("schemas/users/user.json");
    }

    @Test(groups = "contract", description = "GET /users matches the UserPage schema")
    public void listUsersMatchesSchema() {
        assertThat(new UsersApi(api()).list(1)).hasStatus(200).matchesSchema("schemas/users/user-page.json");
    }
}
