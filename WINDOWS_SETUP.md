# 🪟 Complete Windows 10/11 Setup, Build & Run Guide

This guide walks you step-by-step from a **brand-new Windows 10/11 machine** to building, testing, and running the **Bussin' Bus Ticket Booking Platform** (Spring Boot 3.3.4 Backend + React 18 / Vite 5 Frontend).

---

## ⚡ TL;DR — Fastest Way to Launch on Windows

Once JDK 17 and Node.js 18+ are installed, open **PowerShell** in the project root (`antigravity_bus_booking_starter`) and run:

```powershell
powershell -ExecutionPolicy Bypass -File .\run-windows.ps1
```

This single script:
1. Automatically locates your **JDK 17+** (`JAVA_HOME`) and **Node.js** installation (even if an older Oracle Java 8 `javapath` is ahead of it on your system `PATH`).
2. Automatically uses the local `.m2` Maven cache if present (offline ZIP distribution) or your user `$HOME\.m2` cache (Git clone distribution), and runs `npm install` automatically if `frontend/node_modules` is not yet installed.
3. Starts the **Spring Boot Backend** on `http://localhost:8080` (defaulting to the zero-config `local` H2 in-memory profile with pre-seeded stations, routes, buses, users, and 3 demo trips on `2026-11-01`) and waits for `/api/v1/health` to report `UP`.
4. Starts the **React/Vite Frontend** on `http://localhost:5173`.

---

## 🛠️ Step 1: Install Prerequisites on a Fresh Windows Machine

Open **PowerShell as Administrator** and install the required toolchains using Windows Package Manager (`winget`):

### 1.1 Install JDK 17 (Required)
```powershell
winget install --id EclipseAdoptium.Temurin.17.JDK --source winget
```
*(Alternatively, Oracle JDK 17 (`Oracle.JDK.17`) or Microsoft Build of OpenJDK 17 (`Microsoft.OpenJDK.17`) work identically).*

### 1.2 Install Node.js LTS (Required for Frontend)
```powershell
winget install --id OpenJS.NodeJS.LTS --source winget
```

### 1.3 (Optional) Install PostgreSQL 16 or Docker Desktop
You **do not** need PostgreSQL or Docker to run or test the app—the built-in `local` profile runs in PostgreSQL-compatibility mode using an in-memory H2 database with automatic data seeding. However, if you want persistent storage with PostgreSQL:
```powershell
# Option A: Docker Desktop
winget install --id Docker.DockerDesktop --source winget

# Option B: Native Windows PostgreSQL 16
winget install --id PostgreSQL.PostgreSQL.16 --source winget
```

> **Important:** Close and reopen PowerShell after installing via `winget` so your updated `PATH` environment variables take effect.

---

## 🔧 Step 2: Configure Environment & PowerShell Execution Policy

Open a new **PowerShell** window and navigate to the project directory (note that if you extracted a zip archive, the project files may be inside a nested `antigravity_bus_booking_starter` folder—make sure you are in the directory containing `pom.xml` and `mvnw.cmd`):

```powershell
cd "C:\path\to\antigravity_bus_booking_starter"
```

### 2.1 Allow Local PowerShell Scripts for Current Session
Windows blocks unsigned `.ps1` scripts by default (`PSSecurityException`). Run this command to allow scripts in your current terminal window without changing system-wide security settings:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
```

### 2.2 Verify `JAVA_HOME` Points to JDK 17+
Many Windows machines have an old Oracle Java 8 runtime at `C:\Program Files\Common Files\Oracle\Java\javapath` that intercepts `java` on the `PATH`. Point `JAVA_HOME` explicitly to your JDK 17 installation:

```powershell
# Example if installed at C:\Program Files\Java\jdk-17:
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Verify Java 17 is active:
java -version
```

*(If you installed Eclipse Adoptium Temurin 17, look under `C:\Program Files\Eclipse Adoptium\jdk-17*` instead. Note that `.\run-windows.ps1` and `.\scripts\verify-clean-setup.ps1` detect these paths automatically!)*

### 2.3 (Optional) Offline Maven Dependencies (`.m2`)
- **If you extracted the offline starter ZIP:** A pre-populated `.m2` directory is included in the project root. You can point Maven to it so nothing needs to be downloaded:
  ```powershell
  $env:MAVEN_USER_HOME = "$PWD\.m2"
  ```
- **If you cloned via Git:** `.m2/` and `frontend/node_modules/` are excluded by `.gitignore`. Leave `MAVEN_USER_HOME` unset so Maven automatically downloads dependencies to your standard `$env:USERPROFILE\.m2` directory on the first run. (`.\run-windows.ps1` and `.\scripts\verify-clean-setup.ps1` detect both cases automatically).

---

## 🚀 Step 3: Run the Backend Server

You can run the Spring Boot backend in **two modes**:

### Mode A: Zero-Config In-Memory Mode (`local` Profile — Recommended for Quick Start)
Requires **no database installation or Docker container**. Starts an in-memory H2 database in PostgreSQL compatibility mode and automatically runs [`DevDataInitializer.java`](file:///C:/Users/Parijaat%20Gaur/Downloads/antigravity_bus_booking_starter/antigravity_bus_booking_starter/src/main/java/com/busbooking/common/config/DevDataInitializer.java) to seed:
- **2 Users:** `admin@busbooking.local` (`Password@123`) and `customer@demo.local` (`Password@123`)
- **3 Bus Categories:** AC Seater (2x2), Luxury Sleeper (2x1), Volvo Multi-Axle AC
- **3 Buses:** `MH-12-AB-1001` (40 seats), `KA-01-XY-9999` (24 berths), `DL-01-ZZ-5050` (30 berths)
- **8 Stations:** Mumbai (`BOM`), Pune (`PNQ`), Solapur (`SSE`), Hyderabad (`HYD`), Bengaluru (`BLR`), Chennai (`MAA`), Delhi (`DEL`), Jaipur (`JAI`)
- **3 Routes:** `R-WEST-01` (BOM -> PNQ -> SSE -> HYD), `R-SOUTH-01` (HYD -> BLR -> MAA), `R-WEST-EXP` (BOM -> PNQ)
- **3 Scheduled Trips on `2026-11-01`** (including both direct trips and a connecting journey from Mumbai to Bengaluru via Hyderabad)

Run:
```powershell
$env:SPRING_PROFILES_ACTIVE = "local"
.\mvnw.cmd spring-boot:run
```

### Mode B: Full PostgreSQL Mode (`default` Profile)
Uses PostgreSQL on `localhost:5432` (database: `busbooking`, user: `postgres`, password: `postgres`), executes Flyway migrations (`V1__init_schema.sql`, `V2__seed_reference_data.sql`), and automatically seeds the 3 demo trips on `2026-11-01` if no trips exist yet.

1. Start PostgreSQL via Docker Compose:
   ```powershell
   docker compose up -d
   ```
   *(Or ensure your local Windows PostgreSQL service is running on port `5432` with a database named `busbooking` and user/password `postgres`/`postgres`, or override via `$env:DB_URL`, `$env:DB_USERNAME`, `$env:DB_PASSWORD`).*

2. Start Spring Boot:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

Once started, verify the backend is healthy at:
- **Health Check:** `http://localhost:8080/api/v1/health`

---

## 💻 Step 4: Run the React Frontend Dev Server

Open a **second PowerShell window** in the project root and run:

```powershell
cd frontend
npm install
npm run dev
```

*(If `node_modules` is already present and you only have `node.exe` on your PATH without `npm.cmd`, you can launch Vite directly with:)*
```powershell
cd frontend
node .\node_modules\vite\bin\vite.js --host 127.0.0.1 --port 5173
```

Open your browser to:
- **Web App:** `http://localhost:5173`

> **Note:** Vite automatically proxies all `/api/*` requests from `http://localhost:5173` to `http://localhost:8080` (configured in [`frontend/vite.config.js`](file:///C:/Users/Parijaat%20Gaur/Downloads/antigravity_bus_booking_starter/antigravity_bus_booking_starter/frontend/vite.config.js)).

---

## 🧪 Step 5: Run Automated Tests & Verification Scripts

### 5.1 One-Click Clean Setup Verification (Backend Tests + Frontend Build)
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-clean-setup.ps1
```

### 5.2 Run Backend JUnit 5 Suite Directly
On Windows PowerShell (especially in non-interactive shells or backgrounded terminals), Maven Surefire's forked JVM pipe can hang waiting on standard I/O. Always pass `-DforkCount=0` on Windows for reliable, fast execution:

```powershell
# Run full test suite (93 tests across unit, slice, integration, and 20-thread concurrency):
.\mvnw.cmd test -DforkCount=0

# Run a single test class:
.\mvnw.cmd test -Dtest=TripControllerIntegrationTest -DforkCount=0
```

### 5.3 Run Live End-to-End API Walkthrough (`R16` / `R17` Flow)
While the backend is running on port `8080`, run:
```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\demo-walkthrough.ps1
```

---

## 🩺 Step 6: Troubleshooting Common Windows Issues

| Symptom | Cause | Fix |
|---|---|---|
| `cannot be loaded because running scripts is disabled on this system` | Windows PowerShell default `Restricted` execution policy | Run with `powershell -ExecutionPolicy Bypass -File .\script.ps1` or run `Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass` |
| `JAVA_HOME is set to an invalid directory: C:\Program Files\Common Files\Oracle\Java` | Oracle Java 8 `javapath` shim is on the Windows `PATH` ahead of JDK 17 | Set `$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"` (or use `.\run-windows.ps1` which auto-detects real JDK 17 folders) |
| `mvnw.cmd test` hangs after `Started AuthControllerTest` | Maven Surefire 3.2.5 forked JVM stdout pipe deadlock on Windows | Pass `-DforkCount=0` to `.\mvnw.cmd test -DforkCount=0` |
| `Web server failed to start. Port 8080 was already in use` | A previous Java process is still listening on port 8080 | Find and stop the process: `Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080 -State Listen).OwningProcess -Force` |
| `Connection to localhost:5432 refused` when running `.\mvnw.cmd spring-boot:run` | Default profile expects PostgreSQL on port `5432` | Either start Postgres (`docker compose up -d`) or run in zero-setup mode: `$env:SPRING_PROFILES_ACTIVE = "local"; .\mvnw.cmd spring-boot:run` |
| `Unknown lifecycle phase ".run.profiles=local"` | PowerShell splits unquoted `-Dspring-boot.run.profiles=local` on the `.` character | Use `$env:SPRING_PROFILES_ACTIVE = "local"; .\mvnw.cmd spring-boot:run` instead |
