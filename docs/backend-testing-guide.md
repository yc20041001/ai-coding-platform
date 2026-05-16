# Backend Testing Guide

## 1. Technology Stack

- **JUnit 5** — test framework
- **Spring Boot Test** — `@SpringBootTest(webEnvironment = RANDOM_PORT)` for integration tests
- **TestRestTemplate** — real HTTP requests for API integration tests
- **AssertJ** — fluent assertions
- **Maven Surefire** — test execution
- **Mock Model Gateway** — MOCK provider, no real API keys needed

## 2. Test Profile

Configuration: [`backend/src/test/resources/application-test.yml`](../backend/src/test/resources/application-test.yml)

Key settings:
- `app.model-gateway.default-provider: MOCK` — all model calls use mock provider
- `app.jwt.secret` — test-only secret
- `spring.flyway.enabled: false` — schema managed by `spring.sql.init`
- `app.prompt-safety-enabled: true` — safety checks active in tests

## 3. Test Categories

### Unit Tests

Pure Java tests — no Spring context needed. Use for:
- Service business logic
- Domain value objects
- Utility methods
- State machines

Pattern:
```java
class MyServiceTest {
    private final MyService service = new MyService(/* dependencies or nulls */);
    
    @Test
    void shouldDoSomething() {
        var result = service.method(input);
        assertThat(result).isNotNull();
    }
}
```

### Integration Tests

Extend `IntegrationTestBase` for full Spring context + HTTP:
- Access `restTemplate` for HTTP calls
- Use `post()`, `get()`, `put()`, `delete()` — auto-auth with admin token
- Use `getNoAuth()` for unauthenticated requests
- Use `assertOk()`, `assertCode()` for response validation
- Use unique data names: `"IT-Entity-" + System.currentTimeMillis()`

Pattern:
```java
class MyIntegrationTest extends IntegrationTestBase {
    @Test
    void shouldWork() {
        ResponseEntity<String> res = post("/api/endpoint", Map.of("key", "value"));
        assertOk(res);
    }
}
```

## 4. Test Data Rules

1. **Unique names**: Always use `System.currentTimeMillis()` suffix
2. **No Demo data dependency**: Tests create their own data
3. **No order dependency**: Each test is self-contained
4. **No external APIs**: MOCK provider for model calls, no real tokens
5. **No production data**: Tests use the `test` profile with test database

## 5. Assertion Patterns

```java
// Status codes
assertOk(res);                              // code == "OK"
assertCode(res, "UNAUTHORIZED");            // specific error code
assertCode(res, "CONFLICT");                // business logic error

// JSON path extraction
String id = TestJsonHelper.getString(root, "data.id");
long total = TestJsonHelper.getLong(root, "data.total");
boolean flag = TestJsonHelper.getBool(root, "data.enabled");

// AssertJ
assertThat(value).isNotEmpty();
assertThat(value).isEqualTo("expected");
assertThat(value).isIn("A", "B");
assertThat(number).isGreaterThanOrEqualTo(1);
assertThat(content).contains("keyword");
```

## 6. Running Tests

```bash
# All tests
cd backend && mvn test

# Clean + test
cd backend && mvn clean test

# Single test class
cd backend && mvn test -Dtest=JwtTokenProviderTest

# Single test method
cd backend && mvn test -Dtest=JwtTokenProviderTest#shouldGenerateAccessToken

# Via script
bash scripts/run-backend-checks.sh
```

## 7. Adding New Tests

1. Place unit tests in the same package as the class under test (or `application/` sub-package)
2. Place integration tests in a module-level package
3. Extend `IntegrationTestBase` for API-level integration tests
4. Use `TestJsonHelper` for response parsing
5. Ensure test profile activates MOCK provider
6. Verify `mvn test` passes before committing

## 8. Troubleshooting

| Symptom | Likely Cause | Fix |
|---------|-------------|-----|
| `mvn test` fails with DB connection | MySQL not running | Start MySQL: `docker compose -f deploy/docker-compose.app.yml up -d mysql` |
| Test fails with UNAUTHORIZED | Token cached from test startup | Tests auto-login via `adminToken()` — check admin credentials in `IntegrationTestBase` |
| Model call hangs | MOCK provider not active | Check `application-test.yml` has `default-provider: MOCK` |
| Duplicate key error | Test data collision | Ensure unique `System.currentTimeMillis()` suffix in names |
