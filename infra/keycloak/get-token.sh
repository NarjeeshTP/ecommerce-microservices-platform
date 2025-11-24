#!/usr/bin/env bash
set -euo pipefail

REALM=${1:-myrealm}
CLIENT_ID=${2:-myclient}
USERNAME=${3:-testuser}
PASSWORD=${4:-password}
KEYCLOAK_URL=${5:-http://localhost:8080}

TOKEN_ENDPOINT="$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token"

curl -s -X POST "$TOKEN_ENDPOINT" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=$CLIENT_ID&username=$USERNAME&password=$PASSWORD" | jq '.'

