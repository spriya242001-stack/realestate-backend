# End-to-end verification: 2026-10-08

Backend version: `backend-bearer-upload-20261008080535`.

## Direct public IPv4 verification

Base URL: `http://54.146.117.115`.

Frontend connection verified separately by parsing and submitting the actual
rendered HTML forms: registration created an account; form login established
a session; the dashboard property form saved a listing; refreshed dashboard
HTML displayed it; the backend API returned the same listing; and the rendered
logout form ended the session. CSS and JavaScript assets returned 200. This is
HTTP form-flow verification, not a browser JavaScript execution test. No
connection code change was needed. The temporary account and listing were
removed. This applies to the EC2-hosted frontend, not an unprovided Netlify URL.

Latest repeated verification: all 44 deployed endpoint checks, seven additional
error checks, and the 2,360,847-byte image-upload flow passed again. Test data
was removed. EC2 instance/system checks remain OK and the application returns
200 locally. AWS currently reports Ready / Yellow / Warning, with instance
cause `93 % of memory is in use.` The latest health request sample reported
zero 5xx responses. This is a remaining capacity warning, so the current
environment must not be described as Green. The instance has approximately
916 MiB total memory, 79 MiB available, and no swap in the observed sample.

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

## Netlify frontend deployment

Frontend: https://realestate-gokul-20261008.netlify.app

Netlify site ID: `2e02cd2e-ed1c-4545-8cda-1dc7a637e00f`.
Configuration: `deploy/netlify/netlify.toml`.

The existing Spring-rendered frontend is served through a Netlify proxy to
the EC2 backend. Pages, assets, forms, cookies, and APIs share the Netlify
origin. Custom forwarded-host/protocol/port headers keep Spring's redirects
on the HTTPS Netlify URL instead of sending users back to the backend host.

The actual HTML-form registration, session login, dashboard property creation,
listing display, API retrieval, and logout passed through Netlify. Every
redirect in that flow was checked to stay on the Netlify hostname and HTTPS.
CSS and JavaScript assets returned 200. This is HTTP form-flow verification,
not browser JavaScript execution testing. Verification data was removed.

The backend remains hosted on EC2; Java/Thymeleaf execution has not moved
to Netlify. Backend port 443 was previously unreachable; frontend requests
use Netlify's same-origin proxy rather than a direct browser fetch to HTTP.

References:
- https://developer.mozilla.org/en-US/docs/Web/Security/Defenses/Mixed_content
- https://docs.netlify.com/manage/routing/redirects/rewrites-proxies/
