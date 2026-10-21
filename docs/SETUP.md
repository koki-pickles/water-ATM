# 🛠️ Setup: run the backend on your laptop

## 1. Install once

| Tool | Check it works |
|---|---|
| Java 17 or newer | `java -version` |
| Docker | `docker ps` (no error) |

Maven comes with the project (`./mvnw`), so you don't need to install it.

## 2. Run the tests

```bash
cd backend
./mvnw verify
```

The first run downloads Postgres for Docker (~110 MB), so it's slow once. You should see
`Tests run: 71, Failures: 0` and `BUILD SUCCESS`.

## 3. Start the app

```bash
cd backend
./mvnw spring-boot:run
```

Spring Boot starts the Postgres database by itself (from `compose.yaml`) and creates the tables
(Flyway, `src/main/resources/db/migration`). Then open:

👉 http://localhost:8080/actuator/health → `{"status":"UP"}`

Stop with `Ctrl+C`.

### Try it with demo data

Start with the `dev` profile, which adds a demo shop in Nairobi CBD:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Place an order (in a second terminal):

```bash
curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
  -d '{"phone":"0712345678","jerrycans":2,"lat":-1.2870,"lng":36.8180}'
```

## 4. M-Pesa sandbox (test money)

1. Create an account at https://developer.safaricom.co.ke, then **My Apps → Add a new app** and tick
   **M-Pesa Sandbox**. Copy the **Consumer Key** and **Consumer Secret**.
2. M-Pesa must be able to reach your laptop for the callback. Install a tunnel such as
   [ngrok](https://ngrok.com) and run `ngrok http 8080`. It gives you a URL like `https://abc123.ngrok.app`.
3. Start the app with your keys. Use environment variables so the keys **never go into git**:

   ```bash
   export MPESA_CONSUMER_KEY=...your key...
   export MPESA_CONSUMER_SECRET=...your secret...
   export MPESA_CALLBACK_BASE_URL=https://abc123.ngrok.app
   export MPESA_CALLBACK_SECRET=$(openssl rand -hex 16)
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
   ```

   The sandbox shortcode `174379` and its passkey are already the defaults. Check that they still match
   the "Lipa Na M-Pesa Online" test credentials shown in the Daraja portal.
4. Place an order (see above), then ask for payment with the order `id` it returns:

   ```bash
   curl -X POST localhost:8080/api/orders/<order-id>/payment
   curl localhost:8080/api/orders/<order-id>      # status becomes PAID after the callback
   ```

   In the sandbox, the prompt goes to the phone number in the order only if it is a Safaricom test-enabled
   number. Use the test MSISDN shown in the Daraja portal's simulator if your own number gets no prompt.

> ⚠️ The orders API has **no login yet**. Never run it on a public server until Phase 3b adds login.

## 5. In IntelliJ

Open the `backend/pom.xml` file and choose **Open as Project**. Run `WaterAtmBackendApplication`.
To run tests, right-click `src/test/java` → **Run 'All Tests'**.

## How the code is organised (hexagonal architecture)

```
backend/src/main/java/ke/wateratm/
├── domain/          💧 the business rules, plain Java (orders, prices, statuses)
│   ├── model/
│   ├── service/     rules that involve several things (e.g. nearest shop)
│   └── exception/
├── application/     🎯 use cases ("place an order", "mark paid")
│   ├── port/in/     what the outside world can ask us to do
│   ├── port/out/    what we need from the outside world (storage, M-Pesa, WhatsApp...)
│   └── service/     the code that runs the use cases
├── adapter/         🔌 plugs that connect ports to real technology
│   ├── in/web/            REST API + M-Pesa callback
│   ├── in/scheduler/      timers (expire unpaid payments/orders)
│   ├── out/persistence/   Postgres (JPA)
│   └── out/mpesa/         Safaricom Daraja (STK push)
└── config/          🧩 wires everything together + transactions
```

**The one rule:** arrows only point inward. `domain` knows nothing else. `application` only knows
`domain`. Adapters know `application` + `domain`. `HexagonalArchitectureTest` fails the build if
this is broken.

**Where does new stuff go?**
- New business rule → `domain`
- New thing a user can do → a port in `application/port/in` + code in `application/service`
- New outside system (M-Pesa, WhatsApp) → a port in `application/port/out` + an adapter in `adapter/out/...`
- New way in (WhatsApp webhook, REST API for the app) → an adapter in `adapter/in/...`
