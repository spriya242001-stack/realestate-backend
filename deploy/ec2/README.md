# Direct EC2 deployment

## Current AWS environment and troubleshooting

The existing `realestate-backend-prod` environment is managed by Elastic Beanstalk.
Use [the Elastic Beanstalk deployment guide](../aws/README.md) for this environment;
the manual systemd instructions below apply to a separate, directly managed EC2 host.
Do not install a second backend service on the Elastic Beanstalk instance.

Verified on 2026-10-08: `backend-release-20261008072453` is `Ready`, `Green`, and `Ok`.
Public pages/assets return 200 and the anonymous property API returns JSON 401.
Live registration, JWT login, property creation/retrieval, image upload/delivery,
and pending-listing access checks passed. Temporary verification data was removed.

The earlier RDS authentication failure was repaired by an authorized credential
reset and a matching Secrets Manager update. The schema hook initializes missing
objects. The environment now uses its AWS service-linked role and reports healthy
Enhanced monitoring. Credentials and the persistent JWT key are injected at runtime;
do not copy them into `application.properties`.

For future authentication failures, verify the RDS credential, update its matching
`DB_PASSWORD` field in Secrets Manager, and redeploy to reload it. Preserve the
other secret fields. Do not hardcode a Hibernate dialect to bypass authentication.

On Amazon Linux 2023, install Java 21:

```sh
sudo dnf install -y java-21-amazon-corretto-headless
sudo useradd --system --home-dir /opt/realestate-backend --shell /sbin/nologin realestate
sudo install -d -o realestate -g realestate /opt/realestate-backend
```

Upload the JAR and files in this directory using SSH or Systems Manager. Install
the JAR as `/opt/realestate-backend/application.jar`, owned by `realestate`.
Install `realestate-backend.service` into `/etc/systemd/system/`.
Copy `backend.env.example` to `/etc/realestate-backend.env`, fill in the actual
MySQL and Cloudinary credentials, and run `sudo chmod 600 /etc/realestate-backend.env`.
Keep this credential file on the server, outside Git. Generate `JWT_SECRET` with
`openssl rand -base64 48`, put its output in the environment file, and preserve it
across restarts. Use the same secret on every instance.

Use the production MySQL database, not `realestate_backend_test`. For a new empty
database, initialize it with `deploy/aws/schema.sql` before startup. Production
Hibernate validates the schema; it does not create or update tables. Allow MySQL
port 3306 from the backend security group to the database security group.

```sh
sudo systemctl daemon-reload
sudo systemctl enable --now realestate-backend
sudo systemctl status realestate-backend
curl --fail http://localhost:5000/login
```

Use `sudo journalctl -u realestate-backend -n 100 --no-pager` to troubleshoot.
Configure an HTTPS reverse proxy or load balancer for public access, forwarding
to port 5000. Limit direct access to that port to the proxy/load balancer.


## Upload and install

Run from the repository root on your development machine, replacing the SSH key
path and `EC2_HOST` with your instance public address:

```sh
scp -i /path/to/key.pem target/realestate-backend-0.0.1-SNAPSHOT.jar deploy/ec2/realestate-backend.service deploy/ec2/backend.env.example deploy/aws/schema.sql ec2-user@EC2_HOST:~/
ssh -i /path/to/key.pem ec2-user@EC2_HOST
```

After creating the service user and directory as described above, run on EC2:

```sh
sudo install -o realestate -g realestate -m 644 ~/realestate-backend-0.0.1-SNAPSHOT.jar /opt/realestate-backend/application.jar
sudo install -m 644 ~/realestate-backend.service /etc/systemd/system/realestate-backend.service
sudo install -m 600 ~/backend.env.example /etc/realestate-backend.env
sudoedit /etc/realestate-backend.env
```

On updates, edit the existing environment file instead of overwriting its secrets.
For a fresh database, apply `schema.sql` with a MySQL client before starting:

```sh
mysql -h YOUR_MYSQL_HOST -u YOUR_DB_USERNAME -p < ~/schema.sql
```

The database user running this script needs permission to create the database and
tables. Existing databases need reviewed migrations rather than reinitialization.

## Verify after deployment

Run on EC2 after starting the service:

```sh
sudo systemctl is-active realestate-backend
curl --fail --silent --show-error http://localhost:5000/login > /dev/null
curl -i http://localhost:5000/api/properties/my-properties
sudo journalctl -u realestate-backend -n 100 --no-pager
```

Expected: service `active`, login HTTP 200, anonymous property API HTTP 401 with
JSON and no login redirect. After configuring HTTPS, repeat these checks against
the public HTTPS URL and test registration, login, property creation, and retrieval
with a verification account. Check that responses never include owner passwords.
`WebControllerTest` uses mocked services and does not establish remote database,
Cloudinary, network, or HTTPS availability; the integration suite and public smoke
checks verify those layers separately.

Security groups: restrict SSH (22) to your IP, allow database MySQL (3306) from the
backend security group, and restrict backend port 5000 to your load balancer/proxy.
For an HTTPS Application Load Balancer, allow public 443 on the load balancer and
configure a target group on port 5000 with `/login` as its health-check path (200).
Ensure the EC2 host has outbound connectivity to the database and Cloudinary.
