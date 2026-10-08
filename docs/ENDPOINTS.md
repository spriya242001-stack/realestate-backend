# Frontend and backend integration

The frontend is rendered by Thymeleaf in the same Spring Boot application as the
backend. Pages and form actions share one origin; no separate frontend API URL or
CORS configuration is required.

Development URL: `http://localhost:8081`.

| Method | Path | Access | Expected behavior |
| --- | --- | --- | --- |
| GET | `/` | Public | Home and approved-property search; supports keyword, location, type, minPrice, maxPrice |
| GET | `/register` | Public | Registration form with CSRF token |
| POST | `/register` | Public with CSRF | Registers a customer and redirects to login; invalid or duplicate details redisplay errors |
| GET | `/login` | Public | Login form |
| POST | `/login` | Public with CSRF | Authenticates and redirects to dashboard; incorrect credentials redirect to login with an error |
| POST | `/logout` | Authenticated with CSRF | Logs out and redirects to login |
| GET | `/dashboard` | Authenticated | Customer listings and property form |
| GET | `/properties/create` | Authenticated | Redirects to dashboard |
| POST | `/properties/create` | Authenticated with CSRF | Validates and creates a pending listing, with an optional image |
| GET | `/properties/{id}` | Public for approved listings | Pending listings are visible to their owner or admin; missing or hidden listings return 404 |
| GET | `/admin` | Admin | Pending listings and user management |
| POST | `/admin/approve/{id}` | Admin with CSRF | Approves a listing |
| POST | `/admin/users/delete/{id}` | Admin with CSRF | Deletes a user and their listings |
| GET | `/css.css`, `/js.js` | Public | Frontend styles and scripts |

Invalid parameters return 400, denied operations return 403, missing resources
return 404, and unsupported methods return 405 where the caller has access to the
route. These are expected responses, not server failures. Anonymous requests to
protected pages redirect to login. Image provider failures redisplay the property
form with a retry message.

## Verification

- `mvn clean verify`: 27 tests passed, including controller security checks and the
  registration, login, creation, approval, search, logout, and deletion flow.
- Chrome browser check against local MySQL: registration, login, custom assets,
  image preview, real Cloudinary upload, pending listing visibility, admin approval,
  public search/detail, and deletion passed.
- No browser JavaScript errors or backend HTTP 5xx responses occurred in that flow.
- Temporary verification users, listings, and the uploaded image were removed.

AWS environment `realestate-backend-prod` now runs `backend-release-20261008072453`.
On 2026-10-08, AWS reported Ready / Green / Ok. Live registration, JWT login,
creation/retrieval with a real image upload, response-data exclusion, and pending
visibility checks passed. Verification data was removed. Runtime credentials are
in Secrets Manager. The deployed HTTP endpoints were verified; HTTPS was not.

## REST APIs

| Method | Path | Access | Response |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | Public | Validated name/email/password; 201 on success, 400 for invalid or duplicate details |
| POST | `/api/auth/login` | Public | JWT and email; 400 for invalid input, 401 for incorrect credentials |
| GET | `/api/properties/my-properties` | Authenticated | Listing fields only; excludes owner entities and password hashes |

REST authentication failures return JSON with 401; controller failures return JSON
with the appropriate status. Browser pages continue to use login redirects and HTML errors.

Set `JWT_SECRET` to a random secret of at least 32 UTF-8 bytes in deployment, shared
by every application instance. Without it, an instance generates a signing key at
startup, and its tokens stop working when it restarts. The previous hardcoded key
is no longer accepted; clients must log in again after this update. To generate a
secret locally, use `openssl rand -base64 48` and store it in deployment secrets.
