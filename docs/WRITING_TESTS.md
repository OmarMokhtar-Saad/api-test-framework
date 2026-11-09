# Writing tests

This guide adds tests for a new API by walking through the example `users` domain. Each step names the
file to copy. Read [ARCHITECTURE.md](ARCHITECTURE.md) first if you want to know why the layers look
like this.

## The layers

| Layer | Where | Example | Rule |
|---|---|---|---|
| Client | `src/test/java/.../example/<domain>/` | `UsersApi` | One method per endpoint. Returns the raw `Response`. No assertions. |
| Models | same package | `User`, `CreateUserRequest`, `UserPage`, `UserRole` | Java records and enums. No Lombok. |
| Factory | same package | `UserFactory` | Builds valid request bodies with seeded, unique data. |
| Test | `src/test/java/.../tests/<domain>/` | `UsersCrudTest` | Extends `BaseApiTest`. Arrange, act, assert, register cleanup. |
| Stubs | `src/test/resources/wiremock/mappings/` | `users.json` | Lets the test run offline against `env=mock`. |
| Schemas | `src/test/resources/schemas/<domain>/` | `user.json` | JSON Schema for contract tests. |

Domain code never goes in `src/main`. The framework stays domain-agnostic.

## 1. Add a client

One class per resource. Each method maps to one endpoint, carries an Allure `@Step`, and returns the
`Response` so the test decides what to assert.

```java
public final class OrdersApi {

    static final String ORDERS = "/orders";
    static final String ORDER = "/orders/{id}";

    private final ApiClient client;

    public OrdersApi(ApiClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    @Step("GET /orders/{id}")
    public Response get(long id) {
        return client.request().pathParam("id", id).get(ORDER);
    }

    @Step("POST /orders")
    public Response create(CreateOrderRequest body) {
        return client.request().body(body).post(ORDERS);
    }
}
```

- Always start from `client.request()`. It already carries the base URI, base path, timeouts, JSON
  mapper, auth filter and the redacting exchange recorder. Never call `RestAssured.given()` yourself.
- Paths are relative to `basePath` (for example `/api/v1`), so write `/orders`, not `/api/v1/orders`.
- The client takes an `ApiClient` in its constructor. That is how a test swaps auth (see
  [Calling without auth](#calling-without-auth)).

## 2. Add models

Use records. Jackson maps them directly; unknown response fields are ignored by the shared mapper in
`framework/json/ObjectMappers`.

```java
public record Order(long id, String sku, int quantity, Instant createdAt) {}

public record CreateOrderRequest(String sku, int quantity) {
    public CreateOrderRequest withQuantity(int newQuantity) {
        return new CreateOrderRequest(sku, newQuantity);
    }
}
```

Add `withX` methods for the fields negative tests need to change, as `CreateUserRequest.withEmail` does.

## 3. Add a factory

A factory returns a valid body with values that are reproducible (seeded) and unique (collision-free
across parallel tests and parallel CI runs).

```java
public final class OrderFactory {
    private OrderFactory() {}

    public static CreateOrderRequest valid() {
        TestDataFactory data = TestDataFactory.current();
        return new CreateOrderRequest(
                "SKU-" + TestDataFactory.uniqueSuffix(), data.faker().number().numberBetween(1, 10));
    }
}
```

- `TestDataFactory.current().faker()` is seeded from `data.seed`. The seed is logged at start-up.
- `TestDataFactory.uniqueEmail()` returns an address on the reserved `.test` domain. Never use real
  addresses or phone numbers.
- `uniqueSuffix()` and `uniqueEmail()` include the run id, so you can find and delete leftovers.

## 4. Write the test

```java
public class OrdersCrudTest extends BaseApiTest {

    private OrdersApi orders() {
        return new OrdersApi(api());
    }

    @Test(groups = {"smoke", "regression"}, description = "Creating an order returns 201")
    public void createsOrder() {
        CreateOrderRequest request = OrderFactory.valid();

        Response response = orders().create(request);
        assertThat(response).hasStatus(201);
        Order created = assertThat(response).bodyAs(Order.class).actual();
        onCleanup("delete order " + created.id(), () -> orders().delete(created.id()));

        assertThat(response).jsonPath("sku").isEqualTo(request.sku());
    }
}
```

- Static-import `io.github.omarmokhtarsaad.apitest.framework.assertion.ApiAssertions.assertThat`. It
  gives you `ResponseAssert` for a `Response` and plain AssertJ for everything else.
- `ResponseAssert` offers `hasStatus`, `isSuccessful`, `hasHeader`, `hasContentType`, `matchesSchema`,
  `respondedWithin`, `jsonPath` and `bodyAs`. A failure message includes the last HTTP exchange
  (redacted), so you rarely need extra logging.
- Every `@Test` needs a `description` and at least one group.
- Keep no state in fields. Tests run with `parallel="methods"`, so two methods of one class may run at
  the same time. Build what you need inside the method, as `orders()` does.
- `BaseApiTest` gives you `api()`, `config()`, `data()` and `onCleanup(...)`.

### Clean up what you create

Register the cleanup right after the create call succeeds, before any further assertion can fail.

- Cleanups run after the test in reverse order (LIFO), even when it failed or was skipped.
- A failing cleanup is logged and attached to the Allure result as `Cleanup failed: <description>`. It
  does not fail the test or skip later tests.
- Cleanups are thread-local. Call `onCleanup` on the test's own thread, not from a
  `CompletableFuture`, an executor or an Awaitility callback; a cleanup registered on another thread is
  never run.

### Create your own data

Do not depend on records that already exist in an environment, and never change or delete them. The
example tests `replacesUser` and `deletesUser` act on user `1`: that is fine against the WireMock stubs
and destructive against a real environment. When you point tests at a real API, create the record in
the test (or a factory helper), act on it, and clean it up.

### Calling without auth

`ApiClient` is immutable. `withoutAuth()`, `withAuth(strategy)` and `withFilter(filter)` return a new
client and leave the shared one unchanged:

```java
Response response = new UsersApi(api().withoutAuth()).get(1);
assertThat(response).hasStatus(401).hasHeader("WWW-Authenticate", "Bearer");
```

## 5. Groups and suites

| Group | Meaning | Suite |
|---|---|---|
| `smoke` | fast, read-mostly checks that an environment is up | `suites/smoke.xml` |
| `regression` | full behavioural coverage, including negative cases | `suites/regression.xml` |
| `contract` | responses match JSON schemas | `suites/contract.xml` |
| `unit` | framework unit tests, no network except local stubs | `suites/unit.xml` |

`suites/all.xml` (the default) runs the unit tests and every API test. Suites live in
`src/test/resources/suites/` and select by group and package (`...apitest.tests.*`), so a new test class
under `tests/<domain>/` is picked up without editing any suite.

```bash
./mvnw verify -Dsuite=smoke
./mvnw verify -Dtest=OrdersCrudTest
```

To add a suite, copy `smoke.xml`, change the group, and keep `parallel="methods"`. Suite files are why
Surefire is pinned to 3.5.x ([ADR 0005](adr/0005-surefire-3-5-for-testng-suites.md)).

### Framework unit tests

Unit tests for framework code go in `src/test/java/.../unit/` with `groups = "unit"`; the unit suites
pick up that package. One exception: `ListenerRulesTest` lives in the `framework.testng` test package
so it can call package-private listener methods. Because it is outside `unit/`, it is listed by class
name in `suites/unit.xml` and `suites/all.xml`. Do the same for any other test that needs package
access, or it will not run.

## 6. Stubs for the mock environment

With `env=mock` (the default), `MockApiServerListener` starts WireMock on a free port, loads every file
in `src/test/resources/wiremock/mappings/`, and points `baseUrl` and `auth.oauth2.tokenUrl` at it.

Add a mapping file per resource, for example `orders.json`:

```json
{
  "mappings": [
    {
      "name": "create order",
      "priority": 5,
      "request": {
        "method": "POST",
        "urlPath": "/api/v1/orders",
        "headers": { "Authorization": { "equalTo": "Bearer mock-access-token" } }
      },
      "response": {
        "status": 201,
        "headers": { "Content-Type": "application/json", "Location": "/api/v1/orders/501" },
        "body": "{\"id\":501,\"sku\":\"{{jsonPath request.body '$.sku'}}\",\"quantity\":1}",
        "transformers": [ "response-template" ]
      }
    }
  ]
}
```

- Stub paths include the base path (`/api/v1`), because WireMock sees the full URL.
- Match on `Authorization: Bearer mock-access-token`, so a test that forgets auth fails like it would on
  a real API. `users.json` has a lower-priority catch-all that returns 401 for unauthenticated calls;
  copy it.
- Lower `priority` numbers win. Give specific stubs (a validation error) a lower number than the
  general one.

## 7. Contract tests

Put the schema in `src/test/resources/schemas/<domain>/` and assert against its classpath path:

```java
@Test(groups = "contract", description = "GET /orders/{id} matches the Order schema")
public void getOrderMatchesSchema() {
    assertThat(new OrdersApi(api()).get(501)).hasStatus(200).matchesSchema("schemas/orders/order.json");
}
```

The example schemas set `"additionalProperties": false`, so an undocumented field fails the contract
suite. Keep that when the API contract is strict; drop it for APIs that add fields without notice.

## 8. Retries

`TransientFailureRetryAnalyzer` is attached to every `@Test` automatically. It retries only failures
whose cause chain contains an `IOException` (connection reset, timeout), up to `retry.max` times
(default 1). Assertion failures and HTTP error statuses are never retried.

There is no per-test opt-out: `RetryAnnotationTransformer` replaces TestNG's `DisabledRetryAnalyzer`
along with an unset analyzer, so `@Test(retryAnalyzer = DisabledRetryAnalyzer.class)` has no effect.
To turn retries off, run with `-Dretry.max=0` (or `API_RETRY_MAX=0`).

## Checklist for a new domain

- [ ] Client in `example/<domain>/`, one method per endpoint, `@Step` on each, raw `Response` returned.
- [ ] Records for request and response bodies; `withX` methods for negative cases.
- [ ] Factory with seeded and unique values.
- [ ] Tests in `tests/<domain>/` extending `BaseApiTest`, each with a `description` and groups.
- [ ] Every create is followed by `onCleanup`.
- [ ] WireMock mappings so `./mvnw -B verify` passes offline.
- [ ] Schemas and `contract` tests for each response shape.
- [ ] No real data, hostnames, secrets, or company or product names.
