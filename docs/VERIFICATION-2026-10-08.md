# End-to-end verification: 2026-10-08

Backend version: `backend-bearer-upload-20261008080535`.

## Direct public IPv4 verification

Base URL: `http://54.146.117.115`.

- All 44 endpoint checks passed against the actual deployed backend using the
  IPv4 address, including registration, JWT login, browser login/logout,
  dashboard, property creation, owner listings, pending/public details, admin
  approval, search, and admin deletion. The admin was a temporary verification
  account; existing accounts and listings were not modified.
- Seven additional IPv4 error checks passed: wrong registration method (405),
  malformed JSON (400), invalid login email (400), negative price (400),
  inverted price range (400), invalid property type (400), missing listing (404).
- A real 2,360,847-byte PNG uploaded through the IPv4 address with a Bearer
  token and no CSRF token or cookies. The saved Cloudinary image was delivered
  successfully, and pending-listing visibility checks passed.
- Temporary images, listings, and all verification accounts were removed.
- Elastic Beanstalk remains Ready / Green / Ok. No application configuration
  change or redeployment was needed for IPv4 access.

## Confirmed results

- `./mvnw verify`: 28 tests passed, zero failures or errors.
- 44 HTTP checks passed with the production profile on a dedicated local test
  database. These cover every mapped controller route, Spring Security form
  login/logout, static assets, registration, JWT login, property creation,
  owner listings, pending/public details, search, admin approval and deletion.
- Error cases include invalid fields, duplicate registration, invalid login,
  invalid JWT, negative prices, unauthenticated access, customer access to
  admin routes, invalid Bearer uploads without CSRF, and missing properties.
- Live AWS registration, JWT login, owner listings, a real 2,360,847-byte PNG
  upload without CSRF or cookies, Cloudinary delivery, and pending-property
  visibility passed. Verification account, listing, and image were removed.
- Additional live AWS checks: GET registration returns 405; malformed JSON
  and invalid email return 400; negative/inverted price ranges and an invalid
  property type return 400; a missing property returns 404.
- Elastic Beanstalk reports Ready / Green / Ok.
- EC2 `i-001ab7c004ebb7fb9` is running; instance and system checks are OK.
- Nginx is active and its configuration syntax check passes. The application
  responds with HTTP 200 at `http://127.0.0.1:5000/login` on EC2.
- Root filesystem utilization is 37%. The Nginx types_hash tuning warning is
  nonfatal; configuration validation succeeds.
- Local verification server exited; dedicated test tables were cleared.

## Frontend verification remains incomplete

The Netlify site URL and source folder were not supplied or found in this
backend repository. No frontend deployment or browser flow was verified.

The backend's port 443 connection timed out; HTTP is verified. An HTTPS
Netlify page that directly uses fetch against this HTTP backend will encounter
mixed-content blocking. Inspect the actual frontend API configuration before
choosing HTTPS termination or a same-origin Netlify proxy.

References:
- https://developer.mozilla.org/en-US/docs/Web/Security/Defenses/Mixed_content
- https://docs.netlify.com/manage/routing/redirects/rewrites-proxies/
