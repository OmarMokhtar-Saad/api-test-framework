package io.github.omarmokhtarsaad.apitest.tests.users;

import static io.github.omarmokhtarsaad.apitest.framework.assertion.ApiAssertions.assertThat;

import io.github.omarmokhtarsaad.apitest.example.users.CreateUserRequest;
import io.github.omarmokhtarsaad.apitest.example.users.User;
import io.github.omarmokhtarsaad.apitest.example.users.UserFactory;
import io.github.omarmokhtarsaad.apitest.example.users.UserRole;
import io.github.omarmokhtarsaad.apitest.example.users.UsersApi;
import io.github.omarmokhtarsaad.apitest.framework.testng.BaseApiTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import java.time.Duration;
import org.testng.annotations.Test;

@Feature("Users")
@Story("CRUD happy paths")
public class UsersCrudTest extends BaseApiTest {

    private UsersApi users() {
        return new UsersApi(api());
    }

    @Test(
            groups = {"smoke", "regression"},
            description = "An existing user can be fetched by id")
    public void getsExistingUser() {
        Response response = users().get(1);

        assertThat(response)
                .hasStatus(200)
                .hasContentType("application/json")
                .respondedWithin(Duration.ofSeconds(2))
                .bodyAs(User.class)
                .satisfies(user -> {
                    assertThat(user.id()).isEqualTo(1);
                    assertThat(user.role()).isEqualTo(UserRole.ADMIN);
                });
    }

    @Test(
            groups = {"smoke", "regression"},
            description = "Creating a user returns 201 and echoes the input")
    public void createsUser() {
        CreateUserRequest request = UserFactory.validMember();

        Response response = users().create(request);
        assertThat(response).hasStatus(201).hasHeader("Location");
        User created = assertThat(response).bodyAs(User.class).actual();
        onCleanup("delete user " + created.id(), () -> users().delete(created.id()));

        assertThat(response).jsonPath("email").isEqualTo(request.email());
        assertThat(response).jsonPath("name").isEqualTo(request.name());
        assertThat(response).jsonPath("role").isEqualTo("MEMBER");
    }

    @Test(groups = "regression", description = "A user can be replaced with PUT")
    public void replacesUser() {
        CreateUserRequest replacement = UserFactory.valid(UserRole.VIEWER);

        assertThat(users().replace(1, replacement))
                .hasStatus(200)
                .jsonPath("role")
                .isEqualTo("VIEWER");
    }

    @Test(groups = "regression", description = "Deleting a user returns 204 with no body")
    public void deletesUser() {
        Response response = users().delete(1);

        assertThat(response).hasStatus(204);
        assertThat(response.asString()).isEmpty();
    }
}
