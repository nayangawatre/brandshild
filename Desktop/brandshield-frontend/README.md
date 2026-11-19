# BrandShield Frontend

React 19 + Vite + Tailwind CSS v4 frontend for the **BrandShield** Brand Protection & Monitoring Platform.

## 🚀 Features

- **Authentication & Security:** JWT-based login and registration, automatic token injection via Axios interceptor, route guards (`ProtectedRoute`), and auto-logout on token expiration.
- **Executive Dashboard:** Live statistics on protected brands, threat severities, average threat scores, and unread security alert indicators.
- **Brand Management:** Full CRUD operations for registered brands, official domain whitelists, and monitoring keywords.
- **Threat Detection & Triage:**
  - Automated typosquatting and phishing scans.
  - On-demand target inspection (score 0–100, composite severity calculation).
  - Multi-status triage workflow (`DETECTED`, `INVESTIGATING`, `CONFIRMED`, `FALSE_POSITIVE`, `MITIGATED`).
- **Alert Center:** Real-time security notifications for critical/high threats with in-app read/dismiss workflows.
- **Admin Console (RBAC):** Platform-wide metrics, user account enablement/disablement toggles, and immutable audit logs.

## 🛠️ Tech Stack

- **React 19**
- **Vite 8**
- **Tailwind CSS v4** (`@tailwindcss/vite`)
- **React Router 7**
- **Axios**
- **React Icons** (`react-icons/hi2`)
- **React Hot Toast**

## 🏃 Getting Started

### 1. Start the Spring Boot Backend

Ensure PostgreSQL is running on `localhost:5432` with database `brandshield_db`.

```powershell
cd C:\Users\mayurigawatre\Desktop\brandshield-backend
.\mvnw.cmd spring-boot:run
```

The backend starts at `http://localhost:8080`.

### 2. Start the Frontend Development Server

```powershell
cd C:\Users\mayurigawatre\Desktop\brandshield-frontend
npm run dev
```

Open your browser at `http://localhost:5173`.
All `/api/*` requests are automatically proxied to `http://localhost:8080`.

### 3. Build for Production

```powershell
npm run build
```
Outputs static bundle to the `dist/` directory.
