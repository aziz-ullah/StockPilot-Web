# StockPilot - Complete 24/7 Free Deployment Guide

This guide walks you through deploying StockPilot completely for **FREE** with **24/7 availability** using modern cloud hosting platforms.

---

## Architecture Overview

```
+-------------------+        +-----------------------------------+
|  Vercel Frontend  | -----> |  Render FastAPI Backend Service   |
|  (React SPA)      |        |  (Kept awake 24/7 by UptimeRobot) |
+-------------------+        +-----------------------------------+
                                               |
+-------------------+                          |
|    Android App    | -------------------------+
| (Jetpack Compose) |                          v
+-------------------+        +-----------------------------------+
                             |  Neon.tech Managed PostgreSQL     |
                             +-----------------------------------+
```

- **Database**: Neon.tech (Free Tier PostgreSQL)
- **Backend API**: Render.com (Free Web Service)
- **24/7 Keep-Alive**: UptimeRobot (Free HTTP monitor pinging `/health` every 5 mins)
- **Frontend SPA**: Vercel (Free React + Vite hosting)
- **Mobile App**: Android App pointing to live Render API URL

---

## Step 1: Set Up Free PostgreSQL Database (Neon.tech)

1. Go to [Neon.tech](https://neon.tech) and sign up for a free account.
2. Create a new project named **`stockpilot-db`**.
3. Under Dashboard, copy your **PostgreSQL Connection String**. It will look like:
   ```text
   postgresql://alex:password@ep-cool-cloud-123456.us-east-2.aws.neon.tech/neondb?sslmode=require
   ```
4. Keep this connection string ready for Step 2.

---

## Step 2: Deploy FastAPI Backend on Render.com

1. Push your StockPilot repository to **GitHub**.
2. Sign up / Log in to [Render.com](https://render.com).
3. Click **New +** -> **Web Service**.
4. Connect your GitHub repository.
5. Configure the service settings:
   - **Name**: `stockpilot-backend`
   - **Region**: Choose closest to you (e.g. Frankfurt / Oregon / Singapore)
   - **Branch**: `main` (or `master`)
   - **Root Directory**: Leave blank (or `backend` if deploying only backend)
   - **Runtime**: `Python 3`
   - **Build Command**: `pip install -r backend/requirements.txt`
   - **Start Command**: `cd backend && uvicorn app.main:app --host 0.0.0.0 --port $PORT`
6. Scroll down to **Environment Variables** and add:
   - `DATABASE_URL` = *(Paste your Neon connection string from Step 1)*
   - `SECRET_KEY` = `stockpilot_super_secret_jwt_key_2026_production` *(Or any secure random string)*
   - `ALLOWED_ORIGINS` = `*`
7. Click **Create Web Service**.
8. Once deployed, copy your backend live URL (e.g. `https://stockpilot-backend.onrender.com`).
9. Verify the backend health endpoint by opening in browser:
   `https://stockpilot-backend.onrender.com/health`
   You should see: `{"status":"ok","database":"healthy","version":"1.0.0"}`

---

## Step 3: Prevent Sleeping (24/7 Availability via UptimeRobot)

Render free instances go to sleep after 15 minutes of inactivity. We keep it active 24/7 for free using **UptimeRobot**.

1. Sign up for a free account at [UptimeRobot.com](https://uptimerobot.com).
2. Click **+ Add New Monitor**.
3. Fill in the monitor settings:
   - **Monitor Type**: `HTTP(s)`
   - **Friendly Name**: `StockPilot Backend Keep Alive`
   - **URL (or IP)**: `https://stockpilot-backend.onrender.com/health`
   - **Monitoring Interval**: `Every 5 minutes`
4. Click **Create Monitor**.
5. UptimeRobot will now ping your backend every 5 minutes, preventing Render from going to sleep. Your API is now live 24/7!

---

## Step 4: Deploy React Frontend on Vercel

1. Log in to [Vercel.com](https://vercel.com).
2. Click **Add New...** -> **Project**.
3. Import your GitHub repository.
4. Configure Project Settings:
   - **Framework Preset**: `Vite`
   - **Root Directory**: Edit and set to `frontend`
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
5. Expand **Environment Variables** and add:
   - **Key**: `VITE_API_BASE_URL`
   - **Value**: `https://stockpilot-backend.onrender.com` *(Your Render URL from Step 2)*
6. Click **Deploy**.
7. In ~1 minute, your frontend will be live at `https://stockpilot-web.vercel.app` (or custom Vercel domain).

---

## Step 5: Connect Android App to Live Server

### Option A: Change via Android App Settings UI (No Rebuild Required)
1. Open StockPilot app on your phone / emulator.
2. Go to Settings / Server Config screen.
3. Update Server URL to: `https://stockpilot-backend.onrender.com/api/v1/`
4. Tap **Save**.

### Option B: Update Default URL in Code & Rebuild APK
1. Open `android/app/src/main/java/com/stockpilot/app/data/local/TokenManager.kt`.
2. Update `DEFAULT_BASE_URL`:
   ```kotlin
   const val DEFAULT_BASE_URL = "https://stockpilot-backend.onrender.com/api/v1/"
   ```
3. Build the release APK via `./gradlew assembleRelease` or Android Studio.

---

## Verification Checklist

- [x] Backend responds to `GET /health` with `{"status": "ok"}`
- [x] Database tables auto-generated and admin credentials seeded (`admin` / `admin123`)
- [x] CORS allowed for Vercel frontend domain
- [x] UptimeRobot monitor active (pinging every 5 mins)
- [x] Frontend SPA routes reload cleanly without 404s (handled by `vercel.json`)
- [x] Android App connects to backend over HTTPS
