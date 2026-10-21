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

1. **Keys:** at https://developer.safaricom.co.ke, open **My Apps** and pick your app. Copy the
   **Consumer Key** and **Consumer Secret**.
2. **Private settings file:** copy the example and fill in your keys. `backend/.env` is ignored by git,
   so your keys never leave your laptop.

   ```bash
   cd backend
   cp .env.example .env
   ```

   For `MPESA_CALLBACK_SECRET`, paste the output of `openssl rand -hex 16`.
3. **Tunnel**, so Safaricom can reach your laptop. In a separate terminal:

   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```

   It prints a URL like `https://something-random.trycloudflare.com`. Put it in `.env` as
   `MPESA_CALLBACK_BASE_URL` (no slash at the end). The URL changes each time you restart cloudflared,
   so update `.env` when that happens.
4. **Start the app** with the demo shop: `./mvnw spring-boot:run -Dspring-boot.run.profiles=dev`
5. **Place an order and pay** (use the phone that should get the PIN prompt):

   ```bash
   curl -X POST localhost:8080/api/orders -H 'Content-Type: application/json' \
     -d '{"phone":"07XXXXXXXX","jerrycans":2,"lat":-1.2870,"lng":36.8180}'
   curl -X POST localhost:8080/api/orders/<order-id>/payment
   curl localhost:8080/api/orders/<order-id>      # PAID after you enter the PIN
   ```

   The sandbox can send real prompts to Safaricom numbers, but **no real money moves**. Without a
   Safaricom line, the prompt never arrives. M-Pesa then calls back with a timeout, and the payment
   shows as FAILED. That still proves the whole round trip works.

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
