# claude-integration-api

A Spring Boot 3 (Java 21) REST API that authenticates requests with JWT and forwards
prompts to the [Claude API](https://docs.claude.com/) via the official
[Anthropic Java SDK](https://github.com/anthropics/anthropic-sdk-java).

## Features

- `POST /api/ask` — accepts `{"prompt": "..."}`, calls Claude, returns `{"response": "..."}`
- JWT Bearer authentication on every endpoint except token issuance
- `POST /api/auth/token` — issues a test JWT (demo-only helper, see [Security notes](#security-notes))
- Centralized error handling: `400` for bad input, `401` for missing/invalid tokens, `502` for Claude API failures
- Dockerfile + docker-compose for containerized local/deployment use
- `ANTHROPIC_API_KEY` and JWT secret are read from environment variables — nothing sensitive is hardcoded for production use

## Project structure

```
claude-integration-api/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── src/
    ├── main/java/com/example/claudeintegration/
    │   ├── ClaudeIntegrationApiApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java        # JWT filter chain, stateless sessions
    │   │   └── AnthropicConfig.java        # AnthropicClient bean (reads ANTHROPIC_API_KEY)
    │   ├── controller/
    │   │   ├── AskController.java          # POST /api/ask
    │   │   └── AuthController.java         # POST /api/auth/token (demo helper)
    │   ├── dto/                            # Request/response records
    │   ├── exception/
    │   │   ├── ClaudeApiException.java
    │   │   └── GlobalExceptionHandler.java # Maps exceptions -> HTTP status + JSON body
    │   ├── security/
    │   │   ├── JwtService.java             # Issue/validate JWTs
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── RestAuthenticationEntryPoint.java # JSON 401 responses
    │   └── service/
    │       └── ClaudeService.java          # Wraps the Anthropic SDK call
    └── test/java/com/example/claudeintegration/
        ├── ClaudeIntegrationApiApplicationTests.java
        └── security/JwtServiceTest.java
```

## Prerequisites

- JDK 21
- Maven 3.9+ (or use Docker, which doesn't require a local Maven install)
- An [Anthropic API key](https://console.anthropic.com/)
- Docker + Docker Compose (optional, for containerized runs)

## Configuration

All configuration is environment-variable driven (see `src/main/resources/application.properties`):

| Variable               | Required | Default                                                              | Description                              |
|-------------------------|----------|------------------------------------------------------------------------|-------------------------------------------|
| `ANTHROPIC_API_KEY`     | **Yes**  | *(none)*                                                               | Your Claude API key                       |
| `ANTHROPIC_MODEL`       | No       | `claude-opus-4-8`                                                      | Claude model used for `/api/ask`          |
| `ANTHROPIC_MAX_TOKENS`  | No       | `2048`                                                                  | Max output tokens per request             |
| `JWT_SECRET`            | No*      | `this-is-a-hardcoded-test-secret-key-for-dev-only-change-me-32bytes`   | HMAC signing key for JWTs (≥32 bytes)     |
| `JWT_EXPIRATION_MS`     | No       | `3600000` (1 hour)                                                      | Issued-token lifetime in milliseconds     |
| `SERVER_PORT`           | No       | `8080`                                                                  | HTTP port                                 |

\* The default `JWT_SECRET` is fine for local testing but **must** be overridden with a
real, secret value before this service is exposed anywhere beyond your own machine.

Copy `.env.example` to `.env` and fill in your key if you want to use `docker compose --env-file`,
or just export the variables directly in your shell.

## Running locally with Maven

```bash
export ANTHROPIC_API_KEY=sk-ant-your-key-here
mvn spring-boot:run
```

The API is now listening on `http://localhost:8080`.

### Build and run the jar directly

```bash
mvn clean package
java -jar target/claude-integration-api.jar
```

## Running with Docker Compose

```bash
export ANTHROPIC_API_KEY=sk-ant-your-key-here
docker compose up --build
```

Or with an `.env` file (copied from `.env.example`):

```bash
docker compose --env-file .env up --build
```

## Running with plain Docker

```bash
docker build -t claude-integration-api .
docker run -p 8080:8080 \
  -e ANTHROPIC_API_KEY=sk-ant-your-key-here \
  -e JWT_SECRET=a-real-secret-at-least-32-bytes-long \
  claude-integration-api
```

## Using the API

### 1. Get a test JWT

```bash
curl -s -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "demo-user"}'
```

Response:

```json
{"token": "eyJhbGciOiJIUzI1NiJ9..."}
```

### 2. Call `/api/ask` with the token

```bash
TOKEN="eyJhbGciOiJIUzI1NiJ9..."

curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Explain what a REST API is in one sentence."}'
```

Response:

```json
{"response": "A REST API is an interface that lets applications communicate over HTTP using standard methods like GET and POST to access and manipulate resources."}
```

### Error responses

| Scenario                          | Status | Body                                                                 |
|------------------------------------|--------|-----------------------------------------------------------------------|
| Missing/invalid/expired JWT        | `401`  | `{"error": "Unauthorized", "message": "Missing or invalid JWT Bearer token", ...}` |
| Blank/missing `prompt` field       | `400`  | `{"error": "Bad Request", "message": "prompt: prompt must not be blank", ...}` |
| Malformed JSON body                | `400`  | `{"error": "Bad Request", "message": "Malformed or missing JSON request body", ...}` |
| Claude API call fails              | `502`  | `{"error": "Bad Gateway", "message": "...", ...}`                   |

## Running tests

```bash
mvn test
```

## Security notes

- **`POST /api/auth/token` is a demo/testing convenience only.** It issues a valid JWT for
  *any* username with no credential check, which is appropriate for exercising this sample
  project end-to-end but not for a real deployment. Before using this in production, replace
  it with a proper authentication flow (e.g. validating credentials against a user store, or
  delegating to an OAuth2/OIDC provider) — or remove it entirely and issue tokens from your
  existing auth system using the same `JWT_SECRET`.
- Change `JWT_SECRET` to a strong, randomly generated value (≥32 bytes) outside of local
  development, and supply it only via environment variables or a secrets manager — never
  commit it to source control.
- `ANTHROPIC_API_KEY` is read from the environment and is never logged or returned in any
  API response.
- All endpoints run over HTTP in this sample; put a TLS-terminating proxy/load balancer in
  front of it for anything beyond local development.
