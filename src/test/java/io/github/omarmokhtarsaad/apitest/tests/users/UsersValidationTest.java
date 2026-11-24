package io.github.omarmokhtarsaad.apitest.tests.users;

import static io.github.omarmokhtarsaad.apitest.framework.assertion.ApiAssertions.assertThat;

import io.github.omarmokhtarsaad.apitest.example.users.UserFactory;
import io.github.omarmokhtarsaad.apitest.example.users.UsersApi;
import io.github.omarmokhtarsaad.apitest.framework.testng.BaseApiTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import io.restassured.response.Response;
import org.testng.annotations.Test;

@Feature("Users")
@Story("Validation and error handling")
public class UsersValidationTest extends BaseApiTest {

    private static final String ERROR_SCHEMA = "schemas/users/error.json";

    @Test(groups = "regression", description = "An unknown id returns 404 with an error body")
    public void unknownUserReturns404() {
        Response response = new UsersApi(api()).get(999);

        assertThat(response).hasStatus(404).matchesSchema(ERROR_SCHEMA).hasJsonPath("error", "not_found");
    }

    @Test(groups = "regression", description = "Creating a user without an email is rejected with 400")
    public void createWithoutEmailReturns400() {
        Response response = new UsersApi(api()).create(UserFactory.validMember().withEmail(null));

        assertThat(response).hasStatus(400).matchesSchema(ERROR_SCHEMA).hasJsonPath("fields[0].field", "email");
    }

    @Test(groups = "regression", description = "A request without credentials is rejected with 401")
    public void missingCredentialsReturns401() {
        Response response = new UsersApi(api().withoutAuth()).get(1);

        assertThat(response).hasStatus(401).hasHeader("WWW-Authenticate", "Bearer");
    }
}
