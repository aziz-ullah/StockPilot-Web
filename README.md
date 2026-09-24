# 🚀 StockPilot Web - Clothing Business & Inventory Management System (ERP/POS)

StockPilot Web is a modern, secure, and production-ready **Clothing Business Management Web Application** (ERP/Inventory/POS) tailored for apparel retail & wholesale businesses. Built with **FastAPI**, **React (TypeScript + Tailwind CSS)**, and **PostgreSQL**.

---

## 🛠️ Tech Stack & Architecture

- **Backend:** FastAPI (Python 3.10+) with SQLAlchemy 2.0 ORM & Alembic migrations.
- **Frontend:** React 18 with TypeScript, Vite, Tailwind CSS, Lucide Icons, and Recharts.
- **Database:** PostgreSQL (with `NUMERIC(12,2)` for all monetary columns).
- **Authentication:** OAuth2 with JWT Bearer tokens & `bcrypt` password hashing.
- **Reporting & Utilities:** `openpyxl` & `xlsxwriter` for generic Excel import/export, `reportlab` for printable PDF sales receipts.

```text
React SPA (Web) ──(REST API + JWT)──> FastAPI Backend ──> PostgreSQL Database (stockpilot_db)
```

---

## 🏢 Features & Business Domain Logic

### 1. Products & Variants (Clothing Catalog)
- Category management (Hoodies, Pants, Jackets, Tops).
- Auto-generated SKUs (e.g., `HOOD-BLA-XS`).
- Price & Stock tracking with custom low-stock alert thresholds.
- Audit trail for all inventory movements (`PURCHASE`, `PURCHASE_VOID`, `SALE`, `SALE_VOID`, `IMPORT`, `ADJUSTMENT`).

### 2. Generic Excel Importer Engine & Template System
- Bulk import products, variants (Color, Size), stock levels, and historical sales from multi-sheet Excel workbooks (e.g. `hoodies - new order 240 pieces.xlsx`).
- Automatically differentiates between unsold stock items and historical sold items with dates.
- Downloadable standard Excel import template for future bulk uploads.

### 3. Purchasing & Supplier Management (Stock In)
- Record stock purchases from suppliers with unit cost and payment status (`Paid`, `Pending`).
- **Stock Adjustment:** Saving a purchase automatically increases product stock and logs a `PURCHASE` movement.
- **Voiding PO:** Cancelling a purchase order deducts the purchased quantity from stock and logs a `PURCHASE_VOID` movement.

### 4. Sales & POS Checkout (Stock Out)
- Fast POS register checkout with walk-in or custom customer details, payment method (`Cash`, `Card`, `Bank Transfer`, `Other`), discounts, and taxes.
- **Historical Price Locking:** Each `SaleItem` stores the exact `unit_cost` and `unit_price` at transaction time. Updating base product prices later NEVER alters historical sales records.
- **Non-Destructive Voids:** Voiding an invoice marks status `Voided`, restores item quantities back into product stock, and logs a `SALE_VOID` movement.
- **Printable PDF Receipt:** Downloadable/Printable styled PDF receipt per invoice.

### 5. Admin-Only User Accounts & Access Control
- Admins create accounts for themselves and their staff (`Admin`, `Manager`, `Staff` roles).
- Strict backend dependency enforcement (`Depends(require_role(["Admin"]))`) on `POST /api/v1/auth/users` and `PUT /api/v1/auth/users/{id}`.
- Dedicated **User Accounts** management tab in React frontend visible strictly to Admin users.
- Non-admin users (Staff/Cashiers) cannot create accounts or view the User Accounts management tab.

### 6. Operating Expenses & Financial Accounting
- Log business operating expenses under categories: `Rent`, `Utilities`, `Shipping & Delivery`, `Packaging`, `Salaries & Wages`, `Marketing`, `Maintenance`, `Taxes`, `Other`.
- **Net Profit Formula:**
  $$\text{Net Profit} = \text{Gross Profit} - \text{Total Operating Expenses}$$
  $$\text{Gross Profit} = \text{Total Revenue} - \text{Cost of Goods Sold (COGS)}$$

### 6. Interactive Dashboard & Date Presets
- Date Presets Filter Bar: `Today`, `This Week`, `This Month`, `All Time`, `Custom Date Range`.
- **4 Primary KPIs:** Total Sales (Revenue), Gross Profit, Operating Expenses, Net Profit.
- **2 Inventory KPIs:** Current Stock Valuation ($), Low Stock Items Count.
- **Analytics Charts:** Recharts Line Chart (Sales & Profit Trends), Bar Chart (Sales by Category).
- **Exact Low-Stock Products Table:** Reorder alerts for items below threshold.

---

## ⚡ Quick Start (Local Setup)

### 1. Database Setup (PostgreSQL)
Ensure PostgreSQL 15+ is running locally. The initial migration script automatically sets up `stockpilot_db`.

```bash
python scripts/init_db.py
python scripts/seed_and_test.py
```

### 2. Start FastAPI Backend Server
```bash
cd backend
python -m uvicorn app.main:app --host 127.0.0.1 --port 8000 --reload
```
- API Documentation: `http://127.0.0.1:8000/docs`

### 3. Start React Frontend Server
```bash
cd frontend
npm install
npm run dev
```
- Web Application: `http://localhost:3000`

---

## 🔑 Default Login Credentials

| Role | Username | Password | Email |
| :--- | :--- | :--- | :--- |
| **Admin** | `admin` | `admin123` | `admin@stockpilot.com` |
| **Staff Cashier** | `staff` | `staff123` | `staff@stockpilot.com` |

---

## 🧪 Testing Verification

Run the pytest integration suite verifying monetary precision, price locking, voiding atomicity, and net profit formulas:

```bash
python -m pytest backend/tests/test_business_logic.py -v
```

---

## 🐳 Docker Container Deployment

Deploy the entire stack (PostgreSQL + FastAPI + React Nginx) using Docker Compose:

```bash
docker-compose up --build -d
```

- Web App: `http://localhost:80`
- API Backend: `http://localhost:8000`

---

## 🌐 Sharing Live Preview Link with Client (Without Cloud Deployment)

To generate a instant, secure, public HTTPS link that your client can open on their phone or computer from anywhere:

### Method 1: Python Tunnel Helper (Recommended)
Run this command in the project folder:
```bash
python scripts/start_cloudflare_tunnel.py
```
It will print your live `https://<random-name>.trycloudflare.com` URL in the terminal.

### Method 2: Direct Command Line
Run this command in PowerShell or Command Prompt:
```powershell
C:\Users\User\cloudflared.exe tunnel --url http://localhost:3000
```
Look for `https://<random-name>.trycloudflare.com` in the terminal logs and send it to your client!
