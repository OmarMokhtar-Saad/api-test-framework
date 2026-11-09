# Configuration

All configuration is declared in `ConfigKeys`, loaded by `ConfigProvider`, validated by `ConfigLoader`
and exposed as the immutable `FrameworkConfig` record (package `framework.config`). It is loaded once per
JVM on first use. Tests read it through `config()` in `BaseApiTest`.

## Choosing the environment

The environment name picks which files are loaded. It comes from, in order:

1. `-Denv=<name>`
2. the `API_ENV` environment variable
3. the default, `mock`

The name must match `[a-z0-9][a-z0-9-]{0,30}`, for example `qa`, `staging` or `perf-2`.
A blank value counts as unset, so Maven's empty default `<env></env>` is harmless.

## Precedence

For every key, the first source with a non-blank value wins:

| # | Source | Example | May hold secrets? |
|---|---|---|---|
| 1 | JVM system property | `-DbaseUrl=https://...` | **no** |
| 2 | Environment variable | `API_BASE_URL=https://...` | yes |
| 3 | `env/<env>.secrets.properties` (git-ignored) | `auth.token=...` | yes |
| 4 | `env/<env>.properties` (committed) | `basePath=/v1` | **no** |
| 5 | `env/common.properties` (committed) | `timeout.read.ms=15000` | **no** |

Files are read from the test classpath, so they live in `src/test/resources/env/`. A missing file is
treated as empty.

**Environment variable names** are `API_` plus the key, with camelCase boundaries, `.` and `-` turned
into `_`, upper-cased: `baseUrl` → `API_BASE_URL`, `auth.oauth2.clientSecret` →
`API_AUTH_OAUTH2_CLIENT_SECRET`, `http.log.maxBodyChars` → `API_HTTP_LOG_MAX_BODY_CHARS`.

## Key reference

| Key | Env variable | Default | Values and rules |
|---|---|---|---|
| `env` | `API_ENV` | `mock` | Selects the files. Not a `FrameworkConfig` field. |
| `baseUrl` | `API_BASE_URL` | **required** | Absolute `http`/`https` URL with a host and no `user:password@`. |
| `basePath` | `API_BASE_PATH` | empty | Prefix for every path. A leading `/` is added and a trailing `/` removed; `/` means empty. |
| `timeout.connect.ms` | `API_TIMEOUT_CONNECT_MS` | `5000` | Integer > 0. |
| `timeout.read.ms` | `API_TIMEOUT_READ_MS` | `15000` | Integer > 0. |
| `auth.type` | `API_AUTH_TYPE` | `none` | `none`, `bearer`, `apikey`, `basic`, `oauth2` (case-insensitive). |
| `auth.token` | `API_AUTH_TOKEN` | — | **Secret.** Required for `bearer`. |
| `auth.apikey.header` | `API_AUTH_APIKEY_HEADER` | `X-API-Key` | Header name for `apikey`. Also redacted automatically. |
| `auth.apikey.value` | `API_AUTH_APIKEY_VALUE` | — | **Secret.** Required for `apikey`. |
| `auth.basic.username` | `API_AUTH_BASIC_USERNAME` | — | Required for `basic`. |
| `auth.basic.password` | `API_AUTH_BASIC_PASSWORD` | — | **Secret.** Required for `basic`. |
| `auth.oauth2.tokenUrl` | `API_AUTH_OAUTH2_TOKEN_URL` | — | `http`/`https` URL. Required for `oauth2`. |
| `auth.oauth2.clientId` | `API_AUTH_OAUTH2_CLIENT_ID` | — | Required for `oauth2`. |
| `auth.oauth2.clientSecret` | `API_AUTH_OAUTH2_CLIENT_SECRET` | — | **Secret.** Required for `oauth2`. |
| `auth.oauth2.scope` | `API_AUTH_OAUTH2_SCOPE` | — | Optional, sent as `scope`. |
| `http.log` | `API_HTTP_LOG` | `FAILURES` | `FAILURES`, `ALL`, `NONE`: which tests get HTTP exchange attachments. See [REPORTING.md](REPORTING.md). |
| `http.log.maxBodyChars` | `API_HTTP_LOG_MAX_BODY_CHARS` | `20000` | Integer > 0. Bodies longer than this are truncated in captures. |
| `redact.extraKeys` | `API_REDACT_EXTRA_KEYS` | empty | Comma-separated extra header or field names to mask. |
| `retry.max` | `API_RETRY_MAX` | `1` | Integer ≥ 0. Retries for transport failures only; `0` disables. |
| `data.seed` | `API_DATA_SEED` | random | Long. Seeds test data. The value used is logged at start-up. |
| `threads` | `API_THREADS` | suite value | Integer ≥ 1. Overrides `thread-count` and `data-provider-thread-count` of every suite. |

Only the keys needed by the chosen `auth.type` are read. For example, with `auth.type=none` an
`auth.token` is ignored.

## Secrets

The four secret keys are `auth.token`, `auth.apikey.value`, `auth.basic.password` and
`auth.oauth2.clientSecret`. Rules, enforced by `LayeredConfig.secret`:

- A secret is accepted only from an **environment variable** or the git-ignored
  **`env/<env>.secrets.properties`** file.
- If the winning value comes from a `-D` property or a committed file, the run stops with:
  `Secret '<key>' was supplied via system property, which is not allowed because it leaks the value.`
  There is no fallback to a lower source: remove the bad value.
  (`-D` values show up in process lists and CI logs; committed files are in git history forever.)
- Secret values are wrapped in `Secret`, whose `toString()` is masked. Error messages never echo them.
- `.gitignore` ignores `*.secrets.properties`.

**The one exception.** `src/test/resources/env/mock.secrets.properties` is committed on purpose so the
offline example runs with no setup. Its only value, `auth.oauth2.clientSecret=mock-secret`, is accepted
by nothing but the local WireMock stub. Never put a real value in a committed file, and never add another
`!` exception to `.gitignore`.

In CI, store secrets in the CI secret store and expose them as `API_*` variables:

```yaml
- run: ./mvnw -B verify -Denv=staging -Dsuite=smoke
  env:
    API_AUTH_OAUTH2_CLIENT_SECRET: ${{ secrets.STAGING_CLIENT_SECRET }}
```

## Adding an environment

Example for `staging`:

1. `cp src/test/resources/env/example.properties.template src/test/resources/env/staging.properties`
2. Edit `baseUrl`, `basePath`, `auth.type` and the non-secret auth keys. Commit this file.
3. Provide the secret, either locally in `src/test/resources/env/staging.secrets.properties`
   (git-ignored), or as an environment variable such as `API_AUTH_OAUTH2_CLIENT_SECRET`.
4. Run: `./mvnw verify -Denv=staging -Dsuite=smoke`.
5. Check the start-up log line `Configuration loaded: env=staging baseUrl=...`.

Only `mock` starts WireMock. In `mock`, `MockApiServerListener` sets `baseUrl` and
`auth.oauth2.tokenUrl` itself, so `-DbaseUrl` has no effect there.

## Validation messages

Configuration errors fail fast with a `ConfigurationException` that names the key and how to fix it:

```
Required configuration 'baseUrl' is not set for environment 'staging'. Set it in one of ...
'timeout.read.ms' must be greater than zero. Got: 0
'baseUrl' must not embed credentials (user:password@host). Use the auth.* keys.
'auth.type' must be one of none, bearer, apikey, basic, oauth2. Got: 'kerberos'
```

More in [TROUBLESHOOTING.md](TROUBLESHOOTING.md).

## Adding a configuration key

1. Add the constant to `ConfigKeys`, the field to `FrameworkConfig`, and parsing plus validation to
   `ConfigLoader` (use the existing helpers so the error messages stay consistent).
2. If it is a secret, read it with `requiredSecret` so the secret gate applies.
3. Add a default to `common.properties` or `ConfigLoader` where one makes sense.
4. Add unit tests to `ConfigLoaderTest`: the default, a valid override and each invalid value.
5. Add the row to the key reference above and, if users set it per environment, to
   `example.properties.template`.

Do not add `System.getProperty` or `System.getenv` calls anywhere else; all configuration goes through
this package.
