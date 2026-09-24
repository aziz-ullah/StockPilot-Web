from datetime import datetime
from decimal import Decimal
from typing import Optional, List
from pydantic import BaseModel, ConfigDict, Field, EmailStr

# Auth & User Schemas
class Token(BaseModel):
    access_token: str
    token_type: str
    user: "UserOut"

class TokenData(BaseModel):
    user_id: Optional[int] = None

class LoginRequest(BaseModel):
    username_or_email: str
    password: str

class UserCreate(BaseModel):
    email: str
    username: str
    password: str
    full_name: str
    role: str = "Staff" # Admin, Manager, Staff

class UserOut(BaseModel):
    id: int
    email: str
    username: str
    full_name: str
    role: str
    is_active: bool
    created_at: datetime
    model_config = ConfigDict(from_attributes=True)

# Category Schemas
class CategoryBase(BaseModel):
    name: str
    description: Optional[str] = None

class CategoryCreate(CategoryBase):
    pass

class CategoryOut(CategoryBase):
    id: int
    model_config = ConfigDict(from_attributes=True)

# Product & Variant Schemas
class VariantBase(BaseModel):
    sku: Optional[str] = None # Auto-generated if not provided
    color: str
    size: str
    cost_price: Decimal = Field(..., max_digits=12, decimal_places=2)
    selling_price: Decimal = Field(..., max_digits=12, decimal_places=2)
    stock_quantity: int = 0
    min_stock_alert: int = 5

class VariantCreate(VariantBase):
    pass

class VariantUpdate(BaseModel):
    sku: Optional[str] = None
    color: Optional[str] = None
    size: Optional[str] = None
    cost_price: Optional[Decimal] = None
    selling_price: Optional[Decimal] = None
    stock_quantity: Optional[int] = None
    min_stock_alert: Optional[int] = None

class VariantOut(VariantBase):
    id: int
    product_id: int
    product_name: Optional[str] = None
    category_name: Optional[str] = None
    created_at: datetime
    model_config = ConfigDict(from_attributes=True)

class ProductCreate(BaseModel):
    category_id: int
    name: str
    description: Optional[str] = None
    variants: List[VariantCreate] = []

class ProductOut(BaseModel):
    id: int
    category_id: int
    category_name: Optional[str] = None
    name: str
    description: Optional[str] = None
    variants: List[VariantOut] = []
    created_at: datetime
    model_config = ConfigDict(from_attributes=True)

# Stock Movement Audit Schema
class StockMovementOut(BaseModel):
    id: int
    variant_id: int
    sku: Optional[str] = None
    product_name: Optional[str] = None
    color: Optional[str] = None
    size: Optional[str] = None
    movement_type: str
    quantity: int
    previous_stock: int
    new_stock: int
    reference_id: Optional[str] = None
    notes: Optional[str] = None
    created_at: datetime
    created_by_name: Optional[str] = None
    model_config = ConfigDict(from_attributes=True)

# Supplier Schemas
class SupplierCreate(BaseModel):
    name: str
    contact_person: Optional[str] = None
    phone: Optional[str] = None
    email: Optional[str] = None
    address: Optional[str] = None

class SupplierOut(SupplierCreate):
    id: int
    model_config = ConfigDict(from_attributes=True)

# Purchase (Stock In) Schemas
class PurchaseItemCreate(BaseModel):
    variant_id: int
    quantity: int
    unit_cost: Decimal = Field(..., max_digits=12, decimal_places=2)

class PurchaseCreate(BaseModel):
    supplier_id: Optional[int] = None
    supplier_name: Optional[str] = None
    purchase_date: Optional[datetime] = None
    payment_status: str = "Paid" # Paid, Pending
    notes: Optional[str] = None
    items: List[PurchaseItemCreate]

class PurchaseItemOut(BaseModel):
    id: int
    variant_id: int
    product_name: Optional[str] = None
    color: Optional[str] = None
    size: Optional[str] = None
    sku: Optional[str] = None
    quantity: int
    unit_cost: Decimal
    total_cost: Decimal
    model_config = ConfigDict(from_attributes=True)

class PurchaseOut(BaseModel):
    id: int
    purchase_number: str
    supplier_id: Optional[int] = None
    supplier_name: Optional[str] = None
    purchase_date: datetime
    status: str
    payment_status: str
    total_amount: Decimal
    notes: Optional[str] = None
    items: List[PurchaseItemOut] = []
    created_at: datetime
    created_by_name: Optional[str] = None
    model_config = ConfigDict(from_attributes=True)

# Sale (POS Checkout) Schemas
class SaleItemCreate(BaseModel):
    variant_id: int
    quantity: int
    unit_price: Optional[Decimal] = None # Defaults to variant.selling_price if omitted

class SaleCreate(BaseModel):
    customer_name: Optional[str] = "Walk-in Customer"
    customer_phone: Optional[str] = None
    sale_date: Optional[datetime] = None
    payment_method: str = "Cash" # Cash, Card, Bank Transfer, Other
    discount: Decimal = Field(default=Decimal("0.00"), max_digits=12, decimal_places=2)
    tax: Decimal = Field(default=Decimal("0.00"), max_digits=12, decimal_places=2)
    notes: Optional[str] = None
    items: List[SaleItemCreate]

class SaleItemOut(BaseModel):
    id: int
    variant_id: int
    product_name: Optional[str] = None
    color: Optional[str] = None
    size: Optional[str] = None
    sku: Optional[str] = None
    quantity: int
    unit_cost: Decimal
    unit_price: Decimal
    total_cogs: Decimal
    total_price: Decimal
    model_config = ConfigDict(from_attributes=True)

class SaleOut(BaseModel):
    id: int
    invoice_number: str
    customer_name: Optional[str]
    customer_phone: Optional[str]
    sale_date: datetime
    payment_method: str
    status: str # Completed, Voided
    subtotal: Decimal
    discount: Decimal
    tax: Decimal
    total_amount: Decimal # Total Revenue
    total_cogs: Decimal   # Total Cost of Goods Sold
    gross_profit: Decimal # Revenue - COGS
    notes: Optional[str] = None
    items: List[SaleItemOut] = []
    created_at: datetime
    created_by_name: Optional[str] = None
    model_config = ConfigDict(from_attributes=True)

# Expense Schemas
class ExpenseCreate(BaseModel):
    category: str
    description: str
    amount: Decimal = Field(..., max_digits=12, decimal_places=2)
    expense_date: Optional[datetime] = None

class ExpenseOut(ExpenseCreate):
    id: int
    expense_date: datetime
    created_at: datetime
    created_by_name: Optional[str] = None
    model_config = ConfigDict(from_attributes=True)

# Dashboard & Analytics Schemas
class DashboardKPIs(BaseModel):
    total_sales_revenue: Decimal
    total_cogs: Decimal
    gross_profit: Decimal
    total_expenses: Decimal
    net_profit: Decimal
    current_stock_valuation: Decimal
    low_stock_items_count: int

class TrendChartPoint(BaseModel):
    date: str
    sales: Decimal
    gross_profit: Decimal
    net_profit: Decimal

class CategorySalesPoint(BaseModel):
    category_name: str
    sales: Decimal
    quantity_sold: int

class LowStockItemOut(BaseModel):
    variant_id: int
    product_name: str
    category_name: str
    sku: str
    color: str
    size: str
    stock_quantity: int
    min_stock_alert: int
    status_badge: str # "OUT_OF_STOCK" or "LOW_STOCK"
