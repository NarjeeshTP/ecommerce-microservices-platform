# Keycloak quickstart for local testing

This folder contains quick notes to run a local Keycloak (optional) for OAuth2/OIDC testing.

Files added in this folder:
- `docker-compose.yml` - simple Keycloak dev stack
- `get-token.sh` - small helper script to request an access token for testing

Quick docker-compose (example is provided in `docker-compose.yml`):

```yaml
version: '3.8'
services:
  keycloak:
    image: quay.io/keycloak/keycloak:21.1.0
    command: start-dev
    environment:
      KEYCLOAK_ADMIN: admin
      KEYCLOAK_ADMIN_PASSWORD: admin
    ports:
      - 8080:8080
    volumes:
      - ./realm-import:/opt/keycloak/data/import:rw
    restart: unless-stopped
```

Run (from repo root):

```bash
# start keycloak locally
cd infra/keycloak
docker compose up -d
```

Then open http://localhost:8080 and login with admin/admin.

Optional: import a preconfigured realm by placing a realm JSON file in `infra/keycloak/realm-import/` and Keycloak will auto-import it on startup when using `start-dev`.

Create a realm, client (set Access Type to 'public' or 'confidential' with client secret), and a test user. Configure the client's 'Valid Redirect URIs' and 'Web Origins' as needed.

To get a test access token (example for public client) you can use the helper script `get-token.sh`:

Usage:

```bash
# make script executable once
chmod +x infra/keycloak/get-token.sh

# arguments: <realm> <client-id> <username> <password> [<keycloak-url>]
infra/keycloak/get-token.sh myrealm myclient testuser password
```

Example raw curl if you prefer:

```bash
# replace realm, client, username, password accordingly
curl -X POST \
  "http://localhost:8080/realms/<realm-name>/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=<client-id>&username=<user>&password=<pass>"
```

The returned JSON contains `access_token` which you can use to call service endpoints:

```bash
curl -H "Authorization: Bearer <access_token>" http://localhost:8082/catalog/items
```

Configure the service to use Keycloak for JWT validation by setting in `application.yml` or env:

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8080/realms/<realm-name>
```

This README is a quickstart note — adapt versions and client settings as needed.
