# Catalog Service — Local run & auth notes

This README explains how to run the `catalog-service` locally and how authentication is configured for local dev vs. an OAuth2 provider (Keycloak).

Quick summary
- Dev/local profile: `--spring.profiles.active=local` enables a permissive `SecurityConfig` so `/catalog/**` endpoints are accessible without a token.
- Production/auth mode: set `spring.security.oauth2.resourceserver.jwt.issuer-uri` (e.g. Keycloak realm URL) and the application will validate JWTs.

Build

From repo root (preferred Dockerized Maven):

```bash
# from repo root
docker run --rm --platform linux/amd64 \
  -v "$(pwd)/services/catalog-service":/workspace -w /workspace \
  maven:3.9.6-eclipse-temurin-17 mvn -DskipTests package -B
```

Or with a local Maven installation:

```bash
cd services/catalog-service
mvn -DskipTests package
cd -
```

Run locally (two common modes)

1) Default (no `local` profile) — Spring Security will be active (useful to test real auth behavior):

```bash
JAR=services/catalog-service/target/catalog-service-0.0.1-SNAPSHOT.jar
nohup java -jar "$JAR" --server.port=8082 > /tmp/catalog.log 2>&1 & echo $! > /tmp/catalog.pid
```

Expect endpoints to require authentication unless you set `spring.security.oauth2.resourceserver.jwt.issuer-uri`.

2) Development mode (permissive):

```bash
# stop previous
if [ -f /tmp/catalog.pid ]; then kill $(cat /tmp/catalog.pid) || true; rm -f /tmp/catalog.pid; fi
nohup java -jar "$JAR" --server.port=8082 --spring.profiles.active=local > /tmp/catalog.log 2>&1 & echo $! > /tmp/catalog.pid
curl -sS http://localhost:8082/catalog/items
```

Enable Keycloak JWT validation

Set the issuer-uri to your realm, for example:

```bash
nohup java -jar "$JAR" \
  --server.port=8082 \
  --spring.security.oauth2.resourceserver.jwt.issuer-uri=http://localhost:8080/realms/myrealm \
  > /tmp/catalog.log 2>&1 & echo $! > /tmp/catalog.pid
```

Get an access token (local Keycloak)

If you followed the repo `infra/keycloak` quickstart, use `infra/keycloak/get-token.sh` to get a token:

```bash
chmod +x infra/keycloak/get-token.sh
infra/keycloak/get-token.sh myrealm myclient testuser password
```

Then call the service using the token:

```bash
curl -H "Authorization: Bearer <access_token>" http://localhost:8082/catalog/items
```

Stop and cleanup

```bash
kill $(cat /tmp/catalog.pid) && rm -f /tmp/catalog.pid /tmp/catalog.log /tmp/catalog.response
```

Notes
- The `local` profile is intentionally permissive for developer convenience. Do not enable it in CI or production.
- The app will automatically enable the OAuth2 Resource Server config when `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set.

