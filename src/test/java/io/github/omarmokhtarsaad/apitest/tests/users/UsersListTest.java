package io.github.omarmokhtarsaad.apitest.tests.users;

import static io.github.omarmokhtarsaad.apitest.framework.assertion.ApiAssertions.assertThat;

import io.github.omarmokhtarsaad.apitest.example.users.UserPage;
import io.github.omarmokhtarsaad.apitest.example.users.UsersApi;
import io.github.omarmokhtarsaad.apitest.framework.testng.BaseApiTest;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Feature("Users")
@Story("Listing")
public class UsersListTest extends BaseApiTest {

    @DataProvider(name = "pages", parallel = true)
    public Object[][] pages() {
        return new Object[][] {{1}, {2}, {3}};
    }

    @Test(groups = "regression", dataProvider = "pages", description = "Each requested page is returned")
    public void returnsRequestedPage(int page) {
        UserPage result = assertThat(new UsersApi(api()).list(page))
                .hasStatus(200)
                .bodyAs(UserPage.class)
                .actual();

        assertThat(result.page()).isEqualTo(page);
        assertThat(result.items()).hasSizeLessThanOrEqualTo(result.size());
        assertThat(result.total()).isGreaterThanOrEqualTo(result.items().size());
    }
}
