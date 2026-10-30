# ai-integration-api

A Spring Boot 3 (Java 21) REST API that authenticates requests with JWT and forwards
prompts to Claude (via the official [Anthropic Java SDK](https://github.com/anthropics/anthropic-sdk-java))
Gemini (via the [Google Gen AI SDK](https://github.com/googleapis/java-genai)) or OpenAI (via the
official [OpenAI Java SDK](https://github.com/openai/openai-java)), selectable per request. It also offers a retrieval-augmented generation (RAG) endpoint that answers questions
from a PostgreSQL product catalogue.

## Features

- `POST /api/ask` — accepts `{"prompt": "...", "provider": "claude"|"gemini"|"openai"}`, calls Claude, Gemini or OpenAI (default set by `DEFAULT_PROVIDER`, Claude out of the box), returns the answer plus provider, model, token counts and timing
- `POST /api/ask/rag` — answers customer questions from a PostgreSQL product catalogue using retrieval-augmented generation (see [RAG endpoint](#rag-endpoint))
- JWT Bearer authentication on every endpoint except token issuance
- `POST /api/auth/token` — issues a test JWT (demo-only helper, see [Security notes](#security-notes))
- Centralized error handling: `400` for bad input, `401` for missing/invalid tokens, `502` for Claude/Gemini/OpenAI API failures, `503` when the product database is unreachable
- Dockerfile + docker-compose for containerized local/deployment use
- `ANTHROPIC_API_KEY`, `GEMINI_API_KEY`, `OPENAI_API_KEY` and JWT secret are read from environment variables — nothing sensitive is hardcoded for production use

## Project structure

```
ai-integration-api/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .env.example
└── src/
    ├── main/java/com/example/claudeintegration/
    │   ├── ClaudeIntegrationApiApplication.java
    │   ├── config/
    │   │   ├── SecurityConfig.java        # JWT filter chain, stateless sessions
    │   │   ├── AnthropicConfig.java        # AnthropicClient bean (reads ANTHROPIC_API_KEY)
    │   │   ├── GeminiConfig.java           # Gemini Client bean (reads GEMINI_API_KEY)
    │   │   └── OpenAiConfig.java           # OpenAIClient bean (reads OPENAI_API_KEY)
    │   ├── controller/
    │   │   ├── AskController.java          # POST /api/ask
    │   │   ├── RagController.java          # POST /api/ask/rag
    │   │   └── AuthController.java         # POST /api/auth/token (demo helper)
    │   ├── dto/                            # Request/response records (incl. Product)
    │   ├── exception/
    │   │   ├── ClaudeApiException.java
    │   │   ├── GeminiApiException.java
    │   │   ├── OpenAiApiException.java
    │   │   └── GlobalExceptionHandler.java # Maps exceptions -> HTTP status + JSON body
    │   ├── security/
    │   │   ├── JwtService.java             # Issue/validate JWTs
    │   │   ├── JwtAuthenticationFilter.java
    │   │   └── RestAuthenticationEntryPoint.java # JSON 401 responses
    │   └── service/
    │       ├── ClaudeService.java          # Wraps the Anthropic SDK call (optional system prompt)
    │       ├── GeminiService.java          # Wraps the Google Gen AI SDK call (optional system prompt)
    │       ├── OpenAiService.java          # Wraps the OpenAI SDK Chat Completions call (optional system prompt)
    │       ├── ProviderResolver.java       # Request provider, else DEFAULT_PROVIDER
    │       ├── RetrievalService.java       # PostgreSQL full-text product search
    │       └── RagService.java             # Retrieve -> build system prompt -> ask Claude/Gemini/OpenAI
    ├── main/resources/
    │   ├── application.properties
    │   ├── schema.sql                      # products table + full-text index
    │   └── data.sql                        # 15 seed products (prices in BDT)
    └── test/java/com/example/claudeintegration/
        ├── ClaudeIntegrationApiApplicationTests.java
        ├── controller/AskControllerTest.java
        ├── controller/RagControllerTest.java
        ├── service/OpenAiServiceTest.java
        ├── service/RagServiceTest.java
        ├── service/ProviderResolverTest.java
        └── security/JwtServiceTest.java
```

## Prerequisites

- JDK 21
- Maven 3.9+ (or use Docker, which doesn't require a local Maven install)
- An [Anthropic API key](https://console.anthropic.com/)
- A [Gemini API key](https://aistudio.google.com/apikey) (only needed for `"provider": "gemini"`)
- An [OpenAI API key](https://platform.openai.com/api-keys) (only needed for `"provider": "openai"`)
- PostgreSQL 12+ with a `shop` database — the app connects to it on startup. `docker compose` starts one for you
- Docker + Docker Compose (optional, for containerized runs)

## Configuration

All configuration is environment-variable driven (see `src/main/resources/application.properties`):

| Variable               | Required | Default                                                              | Description                              |
|-------------------------|----------|------------------------------------------------------------------------|-------------------------------------------|
| `DEFAULT_PROVIDER`      | No       | `claude`                                                               | Provider for `/api/ask` and `/api/ask/rag` when a request has no `provider`: `claude`, `gemini` or `openai`. Any other value stops the app at startup |
| `ANTHROPIC_API_KEY`     | **Yes**  | *(none)*                                                               | Your Claude API key                       |
| `ANTHROPIC_MODEL`       | No       | `claude-opus-4-8`                                                      | Claude model used for `/api/ask`          |
| `ANTHROPIC_MAX_TOKENS`  | No       | `2048`                                                                  | Max output tokens per request             |
| `GEMINI_API_KEY`        | For Gemini | *(none)*                                                             | Your Gemini API key                       |
| `GEMINI_MODEL`          | No       | `gemini-3.8-flash`                                                     | Gemini model used for `provider: gemini`. Use `gemini-3.5-flash-lite` for faster, lighter responses |
| `GEMINI_MAX_TOKENS`     | No       | `2048`                                                                  | Max output tokens per Gemini request      |
| `OPENAI_API_KEY`        | For OpenAI | *(none)*                                                             | Your OpenAI API key                       |
| `OPENAI_MODEL`          | No       | `gpt-5.4-mini-2026-03-17`                                                           | OpenAI model used for `provider: openai`. Check the [OpenAI models page](https://platform.openai.com/docs/models) for a current model name |
| `OPENAI_MAX_TOKENS`     | No       | `4096`                                                                  | Max completion tokens per OpenAI request (sent as `max_completion_tokens`), see the note below |
| `DB_URL`                | No       | `jdbc:postgresql://localhost:5432/shop`                                | JDBC URL of the product database (RAG)    |
| `DB_USERNAME`           | No       | `postgres`                                                             | Database user                             |
| `DB_PASSWORD`           | No       | `secret`                                                               | Database password                         |
| `DB_HOST_PORT`          | No       | `5432`                                                                  | Host port docker compose publishes PostgreSQL on (change if 5432 is taken) |
| `JWT_SECRET`            | No*      | `this-is-a-hardcoded-test-secret-key-for-dev-only-change-me-32bytes`   | HMAC signing key for JWTs (≥32 bytes)     |
| `JWT_EXPIRATION_MS`     | No       | `3600000` (1 hour)                                                      | Issued-token lifetime in milliseconds     |
| `SERVER_PORT`           | No       | `8080`                                                                  | HTTP port                                 |

**OpenAI reasoning models and `OPENAI_MAX_TOKENS`:** some OpenAI models (the reasoning models) spend
part of the output token budget on hidden reasoning before writing the visible answer. If the budget runs
out first, the answer comes back empty and the API returns `502` with
`OpenAI returned an empty response (finish reason: length)`. Raise `OPENAI_MAX_TOKENS` if that happens.
The reported `outputTokens` include these reasoning tokens, since OpenAI bills them as output.

\* The default `JWT_SECRET` is fine for local testing but **must** be overridden with a
real, secret value before this service is exposed anywhere beyond your own machine.

For local runs, copy `.env.example` to `.env` and fill in your keys. Both `mvn spring-boot:run`
(via `spring.config.import`) and `docker compose` read `.env` from the project root automatically.
Variables exported in your shell take precedence over `.env`. `.env` is git-ignored — never commit it.

## Running locally with Maven

The app needs PostgreSQL at `DB_URL` on startup (it creates and seeds the `products` table). The easiest
way is to start only the database from docker compose:

```bash
docker compose up -d postgres
```

If another PostgreSQL already uses port 5432 on your machine, set `DB_HOST_PORT=5433` and
`DB_URL=jdbc:postgresql://localhost:5433/shop` in `.env` first.

```bash
export ANTHROPIC_API_KEY=sk-ant-your-key-here
export GEMINI_API_KEY=your-gemini-api-key-here
export OPENAI_API_KEY=your-openai-api-key-here
mvn spring-boot:run
```

The API is now listening on `http://localhost:8080`.

### Build and run the jar directly

```bash
mvn clean package
java -jar target/ai-integration-api.jar
```

## Running with Docker Compose

Both Docker options read your keys from `.env`, so create it first:

```bash
cp .env.example .env   # then edit .env and fill in your keys
```

```bash
docker compose up --build
```

This starts two containers: `postgres` (PostgreSQL 16 with a persistent `postgres-data` volume and a
`pg_isready` healthcheck) and the app, which waits until the database is healthy. Inside compose the
app always connects to `jdbc:postgresql://postgres:5432/shop`, whatever `DB_URL` says in `.env`.
To wipe the database and re-seed from scratch: `docker compose down -v`.

Compose loads `.env` via `env_file:` in `docker-compose.yml`, so it fails with
`env file .env not found` if the file is missing. After editing `.env`, restart with
`docker compose up -d --force-recreate` (no rebuild needed). Stop with `docker compose down`.

## Running with plain Docker

```bash
docker build -t ai-integration-api .
docker run --rm -p 8080:8080 --env-file .env ai-integration-api
```

`--env-file` passes every `KEY=value` line from `.env` into the container. Write values without
quotes: Docker passes quotes through literally. Override a single value with `-e`, e.g.
`-e GEMINI_MODEL=gemini-3.8-flash-lite`.

`.env` is listed in `.dockerignore`, so it is never copied into the image — keys are only
supplied when the container starts.

## Using the API

### 1. Get a test JWT

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "demo-user"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

echo "$TOKEN"
```

### 2. Call `/api/ask` with the token

```bash

curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Explain what a REST API is in one sentence."}'
```

Response:

```json
{
  "response": "A REST API is an interface that lets applications communicate over HTTP using standard methods like GET and POST to access and manipulate resources.",
  "provider": "claude",
  "model": "claude-opus-4-8",
  "inputTokens": 18,
  "outputTokens": 31,
  "durationMs": 1840
}
```

| Field          | Description                                                                                  |
|----------------|----------------------------------------------------------------------------------------------|
| `response`     | The model's text answer                                                                       |
| `provider`     | Which provider answered: `claude`, `gemini` or `openai`                                       |
| `model`        | The model that produced the answer (as reported by the provider)                              |
| `inputTokens`  | Tokens in your prompt                                                                         |
| `outputTokens` | Tokens generated. For Gemini and OpenAI reasoning models this includes internal "thinking"/reasoning tokens, which are billed as output |
| `durationMs`   | Time spent waiting for the provider, in milliseconds                                          |

To pick the provider per request, add `"provider": "gemini"`, `"provider": "openai"` or `"provider": "claude"`
(case-insensitive). Without it, the request goes to `DEFAULT_PROVIDER` (`claude` unless you set
`DEFAULT_PROVIDER=gemini` or `DEFAULT_PROVIDER=openai` in `.env`):

```bash
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Explain what a REST API is in one sentence.", "provider": "gemini"}'
```

OpenAI:

```bash
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Explain what a REST API is in one sentence.", "provider": "openai"}'
```

### Error responses

| Scenario                          | Status | Body                                                                 |
|------------------------------------|--------|-----------------------------------------------------------------------|
| Missing/invalid/expired JWT        | `401`  | `{"error": "Unauthorized", "message": "Missing or invalid JWT Bearer token", ...}` |
| Blank/missing `prompt` field       | `400`  | `{"error": "Bad Request", "message": "prompt: prompt must not be blank", ...}` |
| Malformed JSON body                | `400`  | `{"error": "Bad Request", "message": "Malformed or missing JSON request body", ...}` |
| Invalid `provider` value           | `400`  | `{"error": "Bad Request", "message": "provider: provider must be one of \"claude\", \"gemini\" or \"openai\"", ...}` |
| Claude/Gemini/OpenAI API call fails (incl. empty response) | `502`  | `{"error": "Bad Gateway", "message": "...", ...}`                   |
| Product database unreachable (`/api/ask/rag`) | `503`  | `{"error": "Service Unavailable", "message": "Product database is unavailable", ...}` |

## RAG endpoint

`POST /api/ask/rag` answers customer questions using **only** the products stored in PostgreSQL, instead
of the model's general knowledge. It takes the same request body and returns the same response shape as
`/api/ask`, and also requires a JWT. Provider selection works the same way too: the request's `provider`
if given, otherwise `DEFAULT_PROVIDER` (Claude by default).

```bash
curl -s -X POST http://localhost:8080/api/ask/rag \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Do you have waterproof backpacks for travel? How much are they?"}'
```

Response (example):

```json
{
  "response": "Yes — the Waterproof Travel Backpack (35 litres, USB charging port, laptop sleeve) costs 3290.00 BDT and is in stock.",
  "provider": "claude",
  "model": "claude-opus-4-8",
  "inputTokens": 412,
  "outputTokens": 38,
  "durationMs": 2150
}
```

Ask about something the store doesn't sell (e.g. `"Do you sell watches?"`) and it says it doesn't know and
suggests contacting support. `durationMs` here covers both the database search and the model call.

### How it works: retrieve → augment → generate

1. **Retrieve** — `RetrievalService` runs a PostgreSQL full-text search over each product's name,
   description and category (`to_tsvector` / `plainto_tsquery`, ranked by `ts_rank`) and returns the
   top 5 matches. Any matching word counts, so natural questions like *"Do you have running shoes?"*
   still find products; products matching more of the words rank higher.
2. **Augment** — `RagService` builds a system prompt: fixed instructions (*"You are the assistant for an
   online store. Answer ONLY using the product information below…"*) followed by the retrieved
   products' name, category, price in BDT, stock status and description. If nothing matched, it says
   so, which makes the model answer that it doesn't know.
3. **Generate** — the system prompt and the customer's question go to the chosen provider:
   `ClaudeService.ask(systemPrompt, prompt)` (Claude system prompt),
   `GeminiService.ask(systemPrompt, prompt)` (Gemini system instruction) or
   `OpenAiService.ask(systemPrompt, prompt)` (OpenAI system message), and the answer is returned.

The catalogue lives in `src/main/resources/schema.sql` (table + full-text index) and `data.sql`
(15 seed products: shoes, shirts and bags). Both run on every startup and are safe to re-run; to add
products, insert rows into the `products` table — the search index updates automatically.

## Full local test with curl

A copy-paste walkthrough. Start the app first (`mvn spring-boot:run` or `docker compose up --build`),
then run these in a second terminal.

### 1. Check the app is up

```bash
curl -s http://localhost:8080/actuator/health
```

Expected: `{"status":"UP",...}`

### 2. Get a JWT and save it in `$TOKEN`

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/token \
  -H "Content-Type: application/json" \
  -d '{"username": "demo-user"}' | sed -E 's/.*"token":"([^"]+)".*/\1/')

echo "$TOKEN"
```

The token is valid for 1 hour (`JWT_EXPIRATION_MS`). If later requests return `401`, run this step again.

### 3. Ask Claude (default provider)

```bash
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Say hello in one sentence."}'
```

### 4. Ask Gemini

```bash
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Say hello in one sentence.", "provider": "gemini"}'
```

### 4b. Ask about products (RAG)

```bash
curl -s -X POST http://localhost:8080/api/ask/rag \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Do you have running shoes under 4000 BDT?"}'
```

Expected for all three: `{"response":"...","provider":"...","model":"...","inputTokens":...,"outputTokens":...,"durationMs":...}`

To see just the stats without the long answer text (requires [`jq`](https://jqlang.org/)):

```bash
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "What is Docker?", "provider": "gemini"}' \
  | jq 'del(.response)'
```

### 5. Check the error cases

```bash
# No token -> 401
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Hello"}'

# Blank prompt -> 400
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": ""}'

# Unknown provider -> 400
curl -s -X POST http://localhost:8080/api/ask \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"prompt": "Hello", "provider": "unknown"}'
```

Add `-w '\nHTTP %{http_code}\n'` to any command to also print the status code.

### 6. List the Gemini models your key can use

This calls Google directly (not this app) and does not count against your generation quota.
The first line loads the variables from `.env` into your shell so `$GEMINI_API_KEY` is set:

```bash
set -a; . ./.env; set +a

curl -s -H "x-goog-api-key: $GEMINI_API_KEY" \
  "https://generativelanguage.googleapis.com/v1beta/models?pageSize=100" \
  | grep -oE '"name": "models/[^"]+"' | sed 's/.*models\///; s/"$//'
```

To switch models, set `GEMINI_MODEL` (or `ANTHROPIC_MODEL` for Claude) in `.env` to one of the listed
names (without the `models/` prefix) and restart the app. A model variable exported in your shell
overrides `.env`, so run `unset GEMINI_MODEL` if the change doesn't take effect.

### Free-tier note

The Gemini free tier is small (roughly 20 requests/day for `gemini-3.8-flash`; see your project's
limits in Google AI Studio). Once exceeded, `/api/ask` returns `502` with a `429 RESOURCE_EXHAUSTED`
message from Google — that's quota, not a bug.

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
- `ANTHROPIC_API_KEY`, `GEMINI_API_KEY` and `OPENAI_API_KEY` are read from the environment and are never logged or returned in any
  API response.
- All endpoints run over HTTP in this sample; put a TLS-terminating proxy/load balancer in
  front of it for anything beyond local development.
