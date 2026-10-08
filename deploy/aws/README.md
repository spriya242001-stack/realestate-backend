# AWS deployment

Target: Elastic Beanstalk Java SE with Corretto 21, using an existing MySQL RDS database.

Build the source bundle from the repository root:

```sh
mvn clean verify
python3 deploy/aws/package.py
```

The resulting `target/aws-backend.zip` contains `application.jar` and `Procfile`.

Uploads allow images up to 10 MiB. Spring and Nginx allow a 12 MiB total request
to accommodate multipart form fields. The bundle includes the Nginx setting in
`.platform/nginx/conf.d/elasticbeanstalk/01_upload_limits.conf`; deploy the whole
bundle so the proxy limit is applied.

Postman property creation uses `POST /properties/create` with a valid
`Authorization: Bearer <token>` header and multipart form fields `title`,
`description`, `price`, `type`, `location`, and optional `imageFile`.
Validated Bearer uploads do not need a CSRF token or a session cookie.
Browser session uploads still require the dashboard form's CSRF token.
Let Postman generate the multipart Content-Type and boundary automatically.

Configure environment properties before starting the environment:

- `PORT=5000`
- `DB_URL=jdbc:mysql://YOUR_RDS_ENDPOINT:3306/realestate_backend_db`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET` (random secret of at least 32 UTF-8 bytes)
- `CLOUDINARY_CLOUD_NAME`
- `CLOUDINARY_API_KEY`
- `CLOUDINARY_API_SECRET`

Use AWS secret-backed environment properties for passwords and API secrets. Do not upload `.local` or AWS credentials. The production profile validates the existing database schema; initialize it before starting the backend. Allow database access from the application security group.

The selected database is `database-1` in `us-east-1`; see `database-1.env.example`.
Application: `realestate-backend`. Environment: `realestate-backend-prod`.
Platform: Amazon Linux 2023 with Corretto 21. Instance type: `t3.micro`.
Runtime credentials are stored in the `realestate-backend/runtime` Secrets Manager secret.
Elastic Beanstalk loads individual JSON keys into application environment variables.
The EC2 role has read access to that secret only.

The application security group `sg-07d7fd80f7c0d17d2` can connect to the database
security group on port 3306. The RDS instance remains private.

The predeployment hook checks MySQL authentication and creates only missing objects
from `schema.sql`. Existing tables are not altered. Spring Boot validates the schema.
Deployment settings are stored locally in the ignored `.local/aws-options.json` file.

Check deployment status:

```sh
aws elasticbeanstalk describe-environments --region us-east-1 --environment-names realestate-backend-prod
aws elasticbeanstalk describe-events --region us-east-1 --environment-name realestate-backend-prod --max-records 10
```

## Last verified runtime status

Bearer upload fix verified on 2026-10-08:
`backend-bearer-upload-20261008080535` is `Ready`, `Green`, `Ok`.
All 28 tests passed, including CSRF enforcement for browser/session requests
and invalid Bearer headers. A real 2,360,847-byte PNG uploaded successfully
through the public `/properties/create` route using only a valid Bearer token,
with no CSRF token or session cookie. Cloudinary image delivery passed.
The temporary verification account, listing, and image were removed.

Upload fix verified on 2026-10-08: `backend-upload-20261008073942` is
`Ready`, `Green`, `Ok`. The active Nginx configuration contains
`client_max_body_size 12m`. All 27 tests passed, and a real 2,360,847-byte PNG
uploaded through `/properties/create`, saved successfully, and was delivered
by Cloudinary. The temporary account, listing, and image were removed.

On 2026-10-08, `backend-release-20261008072453` completed deployment to
`realestate-backend-prod`. AWS reports `Ready`, `Green`, `Ok`, with no health causes.

Validation for this release:

- `./mvnw verify`: 27 tests passed; executable JAR packaged successfully.
- 44 live HTTP checks passed under the production profile against the dedicated
  test database, including web/API registration, login/logout, creation, admin
  approval/deletion, search, and access control.
- Public AWS registration, JWT login, property creation/retrieval, response-data
  checks, and pending-listing access checks passed.
- Real Cloudinary image upload and image delivery passed. The temporary image,
  listing, and verification account were removed.

Public URL: http://realestate-backend-prod.eba-kjmtmh2m.us-east-1.elasticbeanstalk.com

Deploy the verified `target/aws-backend.zip` source bundle, which includes the JAR,
explicit production `Procfile`, database schema, and predeployment database check.
Runtime secrets stay in Secrets Manager. Application configuration uses environment
placeholders; the malformed trailing braces and literal credentials were removed.
Production requires `JWT_SECRET`; preserve it across restarts.

Runtime logs are `/var/log/web.stdout.log`; deployment logs are
`/var/log/eb-engine.log`. These differ from the custom systemd service in the
direct EC2 guide. This environment currently serves HTTP; HTTPS was not verified.
