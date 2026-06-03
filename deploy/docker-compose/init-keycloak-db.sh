#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" \
  -c "CREATE DATABASE keycloak OWNER $POSTGRES_USER;"

psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d keycloak \
  -c "GRANT ALL ON SCHEMA public TO $POSTGRES_USER;"
