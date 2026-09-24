import os
import sys
sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'backend')))

from app.core.database import engine, Base, SessionLocal
from app.models.domain import Organization, User, Category, Product, ProductVariant, Sale, Purchase, Expense
from app.core.security import get_password_hash
from app.services.excel_engine import process_excel_import

def seed_database():
    print("Creating tables in PostgreSQL...")
    Base.metadata.create_all(bind=engine)
    
    db = SessionLocal()
    try:
        org = db.query(Organization).first()
        if not org:
            org = Organization(name="StockPilot Apparel Org", code="STOCKPILOT_MAIN")
            db.add(org)
            db.flush()
            
        admin = db.query(User).filter(User.username == "admin").first()
        if not admin:
            admin = User(
                organization_id=org.id,
                email="admin@stockpilot.com",
                username="admin",
                hashed_password=get_password_hash("admin123"),
                full_name="Admin Manager",
                role="Admin"
            )
            db.add(admin)
            db.flush()

        print(f"Organization ID: {org.id}, Admin User ID: {admin.id}")

        excel_path = os.path.abspath("hoodies - new order 240 pieces.xlsx")
        if os.path.exists(excel_path):
            print(f"Importing Excel file: {excel_path}...")
            with open(excel_path, "rb") as f:
                file_bytes = f.read()
            summary = process_excel_import(file_bytes=file_bytes, db=db, user_id=admin.id, org_id=org.id)
            print("Import Summary:", summary)
        else:
            print("No initial excel file found.")

        # Seed sample operating expenses
        if db.query(Expense).count() == 0:
            expenses = [
                Expense(organization_id=org.id, category="Rent", description="Store Showroom Monthly Rent", amount=1200.00, created_by_user_id=admin.id),
                Expense(organization_id=org.id, category="Utilities", description="Electricity & Water Bill", amount=180.50, created_by_user_id=admin.id),
                Expense(organization_id=org.id, category="Packaging", description="Custom Shipping Bags & Hangers", amount=250.00, created_by_user_id=admin.id),
                Expense(organization_id=org.id, category="Marketing", description="Instagram & Facebook Ads", amount=350.00, created_by_user_id=admin.id)
            ]
            db.add_all(expenses)
            db.commit()
            print("Seeded sample operating expenses.")

        print("--- DATABASE STATS ---")
        print("Categories:", db.query(Category).count())
        print("Products:", db.query(Product).count())
        print("Product Variants:", db.query(ProductVariant).count())
        print("Historical Sales:", db.query(Sale).count())
        print("Expenses:", db.query(Expense).count())
        
    finally:
        db.close()

if __name__ == '__main__':
    seed_database()
