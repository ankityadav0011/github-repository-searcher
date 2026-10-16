# GitHub Repository Searcher

Spring Boot (Java 17) REST API that searches GitHub repositories, stores them in PostgreSQL (upsert) and lets you query the stored data with filters.

## Endpoints

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/github/search` | Fetch from GitHub, upsert into PostgreSQL, return the repositories |
| GET | `/api/github/repositories?language=&minStars=&sort=` | Read stored repositories (all params optional, `sort` = `stars` (default) / `forks` / `updated`, always descending) |
| GET | `/swagger-ui.html` | Swagger UI |
| GET | `/v3/api-docs` | OpenAPI JSON |

### POST /api/github/search
```json
{ "query": "spring boot", "language": "Java", "sort": "stars" }
```
`query` is required; `language` and `sort` are optional. Response:
```json
{
  "message": "Repositories fetched and saved successfully",
  "repositories": [
    { "id": 123456, "name": "spring-boot-example", "fullName": "user123/spring-boot-example",
      "description": "...", "owner": "user123", "language": "Java", "stars": 450, "forks": 120,
      "watchers": 450, "openIssues": 7, "sizeKb": 900, "url": "https://github.com/user123/spring-boot-example",
      "createdAt": "2020-01-01T00:00:00Z", "lastUpdated": "2024-01-01T12:00:00Z", "pushedAt": "2024-01-02T12:00:00Z" }
  ]
}
```
No matches -> `200` with `"message": "No repositories found for the given criteria"` and an empty list.

### Errors (same JSON shape everywhere)
```json
{ "timestamp": "...", "status": 400, "error": "Bad Request", "message": "Validation failed", "path": "/api/github/search", "details": ["query: query must not be blank"] }
```
| Status | When |
|---|---|
| 400 | blank query, bad JSON, invalid `sort`, negative/non-numeric `minStars`, query rejected by GitHub (422) |
| 429 | GitHub rate limit hit (`Retry-After` header is set when GitHub tells us the wait time) |
| 502 | GitHub down, timeout, bad/unreadable response, bad token |
| 500 | anything unexpected |

## Design notes
- **Layers:** `controller` -> `service` -> `repository`, GitHub access isolated behind the `GithubApiClient` interface (easy to mock).
- **Upsert:** the primary key is GitHub's repo id. One `SELECT ... WHERE id IN (...)` loads existing rows, they are updated in place, new ones inserted, then one batched `saveAll`. The entity implements `Persistable` so new rows are `INSERT`ed without an extra select per row.
- **Filters:** JPA `Specification`s (`RepositorySpecifications`) – a missing filter adds no condition. Language match is case-insensitive.
- **Patterns used:** layered architecture, Repository, DTO, Adapter/Port (`GithubApiClient`), Specification, factory method (`GithubRepository.from`), enum strategy for sorting (`SortField`), centralised `@RestControllerAdvice`.
- GitHub query built as `<query> in:name language:<language>`, sorted by `stars|forks|updated`, desc, 30 results per call (`github.per-page`, max 100).

## Run locally

Prerequisites: JDK 17, Maven 3.9+, and a PostgreSQL database.

**Database (pick one):**
- *No install:* create a free database on neon.tech and copy its connection string, e.g. `postgresql://myuser:mypass@host/neondb?sslmode=require`. Set these (PowerShell shown, use `export` on macOS/Linux):
  ```
  $env:SPRING_DATASOURCE_URL="jdbc:postgresql://host/neondb?sslmode=require"
  $env:SPRING_DATASOURCE_USERNAME="myuser"
  $env:SPRING_DATASOURCE_PASSWORD="mypass"
  ```
- *Local PostgreSQL:* run `CREATE DATABASE github_searcher;` and make sure the username/password in `src/main/resources/application.yml` match yours (default `postgres` / `postgres`).

**Start the API:** `mvn spring-boot:run` (http://localhost:8080, the table is created automatically).
Open http://localhost:8080/swagger-ui.html or import `postman_collection.json` into Postman.

Optional: `GITHUB_TOKEN` (any GitHub personal access token, no scopes needed) raises the search limit from 10 to 30 requests/minute.
Windows PowerShell: `$env:GITHUB_TOKEN="ghp_xxx"; mvn spring-boot:run`  ·  macOS/Linux: `GITHUB_TOKEN=ghp_xxx mvn spring-boot:run`

### Environment variables
| Variable | Default |
|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/github_searcher` |
| `SPRING_DATASOURCE_USERNAME` | `postgres` |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` |
| `GITHUB_TOKEN` | empty (unauthenticated) |
| `PORT` | `8080` |

## Tests
```bash
mvn test
```
Unit tests (Mockito), controller tests (`@WebMvcTest`), repository tests (`@DataJpaTest`, H2) and one end-to-end test (`@SpringBootTest`, H2, GitHub mocked). Coverage report: `target/site/jacoco/index.html`. No real PostgreSQL or internet needed for tests.

## Deploy (for the live URL)
Use a host that builds Maven projects directly (e.g. Railway) and add its PostgreSQL database. Set `SPRING_DATASOURCE_URL` (as `jdbc:postgresql://host:port/db`), `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` and optionally `GITHUB_TOKEN`. Start command: `java -jar target/github-repository-searcher-1.0.0.jar`. Swagger will be at `<your-url>/swagger-ui.html`.
