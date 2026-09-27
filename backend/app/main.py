import os
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy import text
from app.core.config import settings
from app.core.database import engine, Base, SessionLocal
from app.core.security import get_password_hash
from app.models.domain import Organization, User, Category
from app.api import (
    auth, categories, products, purchases, sales, expenses, dashboard, excel_import, reports
)

app = FastAPI(
    title=settings.PROJECT_NAME,
    version=settings.VERSION,
    openapi_url=f"{settings.API_V1_STR}/openapi.json"
)

# CORS Middleware configuration
raw_origins = os.getenv("ALLOWED_ORIGINS", "*")
origins = [origin.strip() for origin in raw_origins.split(",") if origin.strip()]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins if origins else ["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Register API Routers
app.include_router(auth.router, prefix=settings.API_V1_STR)
app.include_router(categories.router, prefix=settings.API_V1_STR)
app.include_router(products.router, prefix=settings.API_V1_STR)
app.include_router(purchases.router, prefix=settings.API_V1_STR)
app.include_router(sales.router, prefix=settings.API_V1_STR)
app.include_router(expenses.router, prefix=settings.API_V1_STR)
app.include_router(dashboard.router, prefix=settings.API_V1_STR)
app.include_router(excel_import.router, prefix=settings.API_V1_STR)
app.include_router(reports.router, prefix=settings.API_V1_STR)

@app.on_event("startup")
def on_startup():
    Base.metadata.create_all(bind=engine)
    
    # Initialize Seed Data if missing
    db = SessionLocal()
    try:
        org = db.query(Organization).first()
        if not org:
            org = Organization(name="StockPilot Clothing Business", code="STOCKPILOT_DEFAULT")
            db.add(org)
            db.flush()
            
        admin = db.query(User).filter(User.username == "admin").first()
        if not admin:
            admin = User(
                organization_id=org.id,
                email="admin@stockpilot.com",
                username="admin",
                hashed_password=get_password_hash("admin123"),
                full_name="Admin User",
                role="Admin"
            )
            db.add(admin)

        staff = db.query(User).filter(User.username == "staff").first()
        if not staff:
            staff = User(
                organization_id=org.id,
                email="staff@stockpilot.com",
                username="staff",
                hashed_password=get_password_hash("staff123"),
                full_name="Staff Cashier",
                role="Staff"
            )
            db.add(staff)

        # Default Categories
        for cat_name in ["Hoodies", "Pants", "Jackets", "Tops"]:
            cat = db.query(Category).filter(Category.name == cat_name).first()
            if not cat:
                db.add(Category(organization_id=org.id, name=cat_name))
                
        db.commit()
    finally:
        db.close()

@app.get("/")
def root():
    return {
        "message": f"Welcome to {settings.PROJECT_NAME} API",
        "version": settings.VERSION,
        "docs": "/docs"
    }

@app.get("/health")
def health_check():
    db_status = "healthy"
    try:
        db = SessionLocal()
        db.execute(text("SELECT 1"))
        db.close()
    except Exception as e:
        db_status = f"unhealthy: {str(e)}"

    return {
        "status": "ok" if db_status == "healthy" else "degraded",
        "database": db_status,
        "version": settings.VERSION
    }
