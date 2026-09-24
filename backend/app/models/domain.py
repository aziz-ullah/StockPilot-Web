from datetime import datetime, timezone
from decimal import Decimal
from sqlalchemy import (
    Column, Integer, String, Boolean, DateTime, Numeric, ForeignKey, Text
)
from sqlalchemy.orm import relationship
from app.core.database import Base

def utc_now():
    return datetime.now(timezone.utc)

class Organization(Base):
    __tablename__ = "organizations"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String(255), nullable=False, default="StockPilot Apparel Org")
    code = Column(String(100), unique=True, nullable=False, default="DEFAULT_ORG")
    created_at = Column(DateTime(timezone=True), default=utc_now)

    users = relationship("User", back_populates="organization")
    categories = relationship("Category", back_populates="organization")
    products = relationship("Product", back_populates="organization")
    variants = relationship("ProductVariant", back_populates="organization")
    purchases = relationship("Purchase", back_populates="organization")
    sales = relationship("Sale", back_populates="organization")
    expenses = relationship("Expense", back_populates="organization")

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    email = Column(String(255), unique=True, index=True, nullable=False)
    username = Column(String(100), unique=True, index=True, nullable=False)
    hashed_password = Column(String(255), nullable=False)
    full_name = Column(String(255), nullable=False)
    role = Column(String(50), nullable=False, default="Staff") # Admin, Manager, Staff
    is_active = Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), default=utc_now)

    organization = relationship("Organization", back_populates="users")

class Category(Base):
    __tablename__ = "categories"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    name = Column(String(100), nullable=False)
    description = Column(Text, nullable=True)

    organization = relationship("Organization", back_populates="categories")
    products = relationship("Product", back_populates="category")

class Product(Base):
    __tablename__ = "products"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    category_id = Column(Integer, ForeignKey("categories.id"), nullable=False)
    name = Column(String(255), nullable=False, index=True)
    description = Column(Text, nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now)

    organization = relationship("Organization", back_populates="products")
    category = relationship("Category", back_populates="products")
    variants = relationship("ProductVariant", back_populates="product", cascade="all, delete-orphan")

class ProductVariant(Base):
    __tablename__ = "product_variants"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    product_id = Column(Integer, ForeignKey("products.id"), nullable=False)
    sku = Column(String(100), unique=True, nullable=False, index=True)
    color = Column(String(100), nullable=False)
    size = Column(String(50), nullable=False)
    cost_price = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00"))
    selling_price = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00"))
    stock_quantity = Column(Integer, nullable=False, default=0)
    min_stock_alert = Column(Integer, nullable=False, default=5)
    created_at = Column(DateTime(timezone=True), default=utc_now)

    organization = relationship("Organization", back_populates="variants")
    product = relationship("Product", back_populates="variants")
    movements = relationship("StockMovement", back_populates="variant", cascade="all, delete-orphan")

class StockMovement(Base):
    __tablename__ = "stock_movements"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    variant_id = Column(Integer, ForeignKey("product_variants.id"), nullable=False)
    movement_type = Column(String(50), nullable=False) # PURCHASE, PURCHASE_VOID, SALE, SALE_VOID, IMPORT, ADJUSTMENT
    quantity = Column(Integer, nullable=False)
    previous_stock = Column(Integer, nullable=False)
    new_stock = Column(Integer, nullable=False)
    reference_id = Column(String(100), nullable=True) # e.g. Invoice / PO #
    notes = Column(Text, nullable=True)
    created_by_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now)

    variant = relationship("ProductVariant", back_populates="movements")
    user = relationship("User")

class Supplier(Base):
    __tablename__ = "suppliers"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    name = Column(String(255), nullable=False)
    contact_person = Column(String(255), nullable=True)
    phone = Column(String(100), nullable=True)
    email = Column(String(255), nullable=True)
    address = Column(Text, nullable=True)

class Purchase(Base):
    __tablename__ = "purchases"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    purchase_number = Column(String(100), unique=True, nullable=False, index=True)
    supplier_id = Column(Integer, ForeignKey("suppliers.id"), nullable=True)
    supplier_name = Column(String(255), nullable=True)
    purchase_date = Column(DateTime(timezone=True), default=utc_now)
    status = Column(String(50), nullable=False, default="Completed") # Completed, Cancelled
    payment_status = Column(String(50), nullable=False, default="Paid") # Paid, Pending
    total_amount = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00"))
    notes = Column(Text, nullable=True)
    created_by_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now)

    organization = relationship("Organization", back_populates="purchases")
    supplier = relationship("Supplier")
    items = relationship("PurchaseItem", back_populates="purchase", cascade="all, delete-orphan")
    created_by = relationship("User")

class PurchaseItem(Base):
    __tablename__ = "purchase_items"

    id = Column(Integer, primary_key=True, index=True)
    purchase_id = Column(Integer, ForeignKey("purchases.id"), nullable=False)
    variant_id = Column(Integer, ForeignKey("product_variants.id"), nullable=False)
    quantity = Column(Integer, nullable=False)
    unit_cost = Column(Numeric(12, 2), nullable=False)
    total_cost = Column(Numeric(12, 2), nullable=False)

    purchase = relationship("Purchase", back_populates="items")
    variant = relationship("ProductVariant")

class Sale(Base):
    __tablename__ = "sales"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    invoice_number = Column(String(100), unique=True, nullable=False, index=True)
    customer_name = Column(String(255), nullable=True, default="Walk-in Customer")
    customer_phone = Column(String(100), nullable=True)
    sale_date = Column(DateTime(timezone=True), default=utc_now)
    payment_method = Column(String(50), nullable=False, default="Cash") # Cash, Card, Bank Transfer, Other
    status = Column(String(50), nullable=False, default="Completed") # Completed, Voided
    subtotal = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00"))
    discount = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00"))
    tax = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00"))
    total_amount = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00")) # Total Revenue
    total_cogs = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00"))   # Total Cost of Goods Sold
    gross_profit = Column(Numeric(12, 2), nullable=False, default=Decimal("0.00")) # Revenue - COGS
    notes = Column(Text, nullable=True)
    created_by_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now)

    organization = relationship("Organization", back_populates="sales")
    items = relationship("SaleItem", back_populates="sale", cascade="all, delete-orphan")
    created_by = relationship("User")

class SaleItem(Base):
    __tablename__ = "sale_items"

    id = Column(Integer, primary_key=True, index=True)
    sale_id = Column(Integer, ForeignKey("sales.id"), nullable=False)
    variant_id = Column(Integer, ForeignKey("product_variants.id"), nullable=False)
    quantity = Column(Integer, nullable=False)
    unit_cost = Column(Numeric(12, 2), nullable=False)  # LOCKED COST AT TIME OF SALE
    unit_price = Column(Numeric(12, 2), nullable=False) # LOCKED PRICE AT TIME OF SALE
    total_cogs = Column(Numeric(12, 2), nullable=False)  # quantity * unit_cost
    total_price = Column(Numeric(12, 2), nullable=False) # quantity * unit_price

    sale = relationship("Sale", back_populates="items")
    variant = relationship("ProductVariant")

class Expense(Base):
    __tablename__ = "expenses"

    id = Column(Integer, primary_key=True, index=True)
    organization_id = Column(Integer, ForeignKey("organizations.id"), nullable=False, default=1)
    category = Column(String(100), nullable=False) # Rent, Utilities, Shipping & Delivery, Packaging, Salaries & Wages, Marketing, Maintenance, Taxes, Other
    description = Column(Text, nullable=False)
    amount = Column(Numeric(12, 2), nullable=False)
    expense_date = Column(DateTime(timezone=True), default=utc_now)
    created_by_user_id = Column(Integer, ForeignKey("users.id"), nullable=True)
    created_at = Column(DateTime(timezone=True), default=utc_now)

    organization = relationship("Organization", back_populates="expenses")
    created_by = relationship("User")
