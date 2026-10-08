# Real estate application

Spring Boot MVC with Thymeleaf, Spring Security, MySQL and Cloudinary. Java 21 or later is required.

## Project structure

```text
realestate-backend/
├── pom.xml
├── mvnw
├── mvnw.cmd
├── .mvn/wrapper/
├── deploy/aws/
├── docs/ENDPOINTS.md
└── src/
    ├── main/
    │   ├── java/com/example/realestate_backend/
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── entity/
    │   │   ├── exception/
    │   │   ├── repository/
    │   │   ├── service/
    │   │   └── RealestateBackendApplication.java
    │   └── resources/
    │       ├── application.properties
    │       ├── application-dev.properties
    │       ├── application-prod.properties
    │       ├── static/
    │       └── templates/
    └── test/
        ├── java/com/example/realestate_backend/
        │   ├── EndToEndFlowTest.java
        │   ├── RealestateBackendApplicationTests.java
        │   ├── controller/WebControllerTest.java
        │   └── service/
        │       ├── UserServiceTest.java
        │       ├── CloudinaryServiceTest.java
        │       └── PropertyServiceTest.java
        └── resources/
            └── application-test.properties
```

Application code and production resources belong under `src/main`. Tests and test-only configuration belong under `src/test`. Local credentials are stored separately in the ignored `.local/` directory.

The Thymeleaf frontend and Spring Boot backend run together on one server. See
[endpoint documentation and verification](docs/ENDPOINTS.md) for routes, access rules,
and the checked browser flow. JavaScript and CSS are served from `static/`; HTML
views belong in `templates/`.

## Local development

Run from the repository root:

```sh
./mvnw -Pdev spring-boot:run
```

The application defaults to the `dev` Spring profile for local runs, including IntelliJ. The Maven `dev` profile also activates `application-dev.properties`. This file uses local MySQL and optionally imports `.local/application.properties`. Set `DB_URL` to override the default `jdbc:mysql://localhost:3306/realestate_backend_db`. Database and Cloudinary credentials come from environment variables or overrides in the ignored local file; application source contains placeholders.

The development port is **8081** (production defaults to **5000**); `PORT` or a command-line `--server.port` option can override it. Stop other listeners on that port before starting. Development uses Hibernate schema updates.

## Tests

```sh
./mvnw test
```

Application tests use the `test` profile and a dedicated MySQL database named `realestate_backend_test` on `localhost:3306`. Create it before running the tests:

```sql
CREATE DATABASE IF NOT EXISTS realestate_backend_test;
```

Set `TEST_DB_USERNAME` (defaults to `root`) and `TEST_DB_PASSWORD`, or store `spring.datasource.username` and `spring.datasource.password` in the ignored `.local/application-test.properties` file. Hibernate uses `create-drop`, so all tables in this test database are recreated and dropped during tests. Use only a dedicated test database. To use a different server, override `spring.datasource.url` in that local test file.

Real Cloudinary credentials are not needed; upload operations are mocked in controller tests.

## Production and Docker

Set `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET` and `JWT_SECRET`. The production profile validates the existing schema and does not update it. Apply reviewed schema migrations before deploying; migration tooling is not configured yet.

Inside Docker, use a database service hostname in `DB_URL`, or `host.docker.internal` for a database running on the macOS host. `localhost` refers to the application container itself. Supply secrets at runtime; `.local` credentials are outside the image build inputs.

Configuration changes do not remove secrets from earlier Git history. Rotate credentials if the previous file was exposed.
