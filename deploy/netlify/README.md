# Netlify frontend gateway

Live URL: https://realestate-gokul-20261008.netlify.app
Site ID: `2e02cd2e-ed1c-4545-8cda-1dc7a637e00f`.
Registration, session login, creation, dashboard listing display, API listing
retrieval, logout, and HTTPS same-host redirects have been verified.

The existing frontend is rendered by Spring/Thymeleaf on EC2. Netlify's proxy
rule serves those pages through a Netlify URL and forwards forms, session
cookies, assets, and API requests to the same backend. This does not move the
Java application onto Netlify.

From this directory, authenticate and deploy:

```sh
npx --yes netlify-cli login
npx --yes netlify-cli deploy --prod --no-build --dir public --site 2e02cd2e-ed1c-4545-8cda-1dc7a637e00f
```

The forced proxy rule takes precedence over the fallback index.html. Verify
registration, browser login, property creation, dashboard, and logout using
the new Netlify URL before declaring this deployment successful. Check that
redirects stay on the Netlify URL and that session cookies survive the proxy.

No database passwords, API keys, or JWT secrets belong in this directory.

Reference: https://docs.netlify.com/manage/routing/redirects/rewrites-proxies/
