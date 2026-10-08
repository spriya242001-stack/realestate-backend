#!/usr/bin/env bash
set -euo pipefail

# Secrets are injected into platform hooks by Elastic Beanstalk.
# Verify authentication before initializing only missing database objects.
dnf install -y mariadb105
export MYSQL_PWD="${DB_PASSWORD:?Database password is required}"
mysql --connect-timeout=15 --ssl \
    --host="${DB_HOST:?Database host is required}" \
    --user="${DB_USERNAME:?Database username is required}" \
    --execute='SELECT 1' >/dev/null
mysql --connect-timeout=15 --ssl \
    --host="$DB_HOST" --user="$DB_USERNAME" < deploy-schema.sql
unset MYSQL_PWD
