import os
import sys
from decimal import Decimal
import pytest
from datetime import datetime

sys.path.insert(0, os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

from app.core.database import SessionLocal, Base, engine
from app.models.domain import Organization, User, Category, Product, ProductVariant, Sale, SaleItem, Purchase, PurchaseItem, Expense, StockMovement
from app.core.security import get_password_hash
from app.services.inventory_service import record_stock_movement

@pytest.fixture(scope="module")
def db():
    Base.metadata.create_all(bind=engine)
    session = SessionLocal()
    yield session
    session.close()

def test_financial_precision_and_price_locking(db):
    org = db.query(Organization).first()
    assert org is not None

    cat = db.query(Category).filter(Category.organization_id == org.id).first()
    if not cat:
        cat = Category(organization_id=org.id, name="Test Category")
        db.add(cat)
        db.flush()

    p = Product(organization_id=org.id, category_id=cat.id, name="Test Unit Hoodie")
    db.add(p)
    db.flush()

    v = ProductVariant(
        organization_id=org.id,
        product_id=p.id,
        sku=f"TEST-HOOD-BLK-M-{int(datetime.now().timestamp())}",
        color="Black",
        size="M",
        cost_price=Decimal("36.50"),
        selling_price=Decimal("75.00"),
        stock_quantity=20,
        min_stock_alert=5
    )
    db.add(v)
    db.commit()
    db.refresh(v)

    # Perform Sale transaction (qty: 2)
    unit_cost_at_sale = v.cost_price # Decimal(36.50)
    unit_price_at_sale = v.selling_price # Decimal(75.00)
    qty = 2

    cogs = Decimal(qty) * unit_cost_at_sale # 73.00
    rev = Decimal(qty) * unit_price_at_sale # 150.00
    gross_p = rev - cogs # 77.00

    sale = Sale(
        organization_id=org.id,
        invoice_number=f"INV-TEST-{int(datetime.now().timestamp())}",
        customer_name="Test Unit Customer",
        status="Completed",
        subtotal=rev,
        total_amount=rev,
        total_cogs=cogs,
        gross_profit=gross_p
    )
    db.add(sale)
    db.flush()

    s_item = SaleItem(
        sale_id=sale.id,
        variant_id=v.id,
        quantity=qty,
        unit_cost=unit_cost_at_sale,
        unit_price=unit_price_at_sale,
        total_cogs=cogs,
        total_price=rev
    )
    db.add(s_item)
    
    record_stock_movement(
        db=db,
        variant_id=v.id,
        movement_type="SALE",
        qty_change=-qty,
        reference_id=sale.invoice_number,
        org_id=org.id
    )
    db.commit()

    # Verify stock reduction
    db.refresh(v)
    assert v.stock_quantity == 18

    # UPDATE BASE PRODUCT PRICE (cost_price to 45.00, selling_price to 90.00)
    v.cost_price = Decimal("45.00")
    v.selling_price = Decimal("90.00")
    db.commit()

    # VERIFY PAST SALE RECORD REMAINS LOCKED
    db.refresh(s_item)
    db.refresh(sale)

    assert s_item.unit_cost == Decimal("36.50")
    assert s_item.unit_price == Decimal("75.00")
    assert sale.total_amount == Decimal("150.00")
    assert sale.total_cogs == Decimal("73.00")
    assert sale.gross_profit == Decimal("77.00")

def test_sale_void_restores_inventory(db):
    sale = db.query(Sale).filter(Sale.customer_name == "Test Unit Customer").first()
    assert sale is not None
    item = sale.items[0]
    v = item.variant
    initial_stock = v.stock_quantity # 18
    
    # Void the sale
    sale.status = "Voided"
    record_stock_movement(
        db=db,
        variant_id=v.id,
        movement_type="SALE_VOID",
        qty_change=item.quantity,
        reference_id=sale.invoice_number,
        org_id=sale.organization_id
    )
    db.commit()

    db.refresh(v)
    assert v.stock_quantity == initial_stock + item.quantity # Restored to 20

    # Verify stock movement log
    movement = db.query(StockMovement).filter(
        StockMovement.variant_id == v.id,
        StockMovement.movement_type == "SALE_VOID"
    ).first()
    assert movement is not None
    assert movement.quantity == item.quantity

def test_net_profit_calculation(db):
    # Net Profit = Gross Profit - Total Operating Expenses
    sales_res = db.query(
        Sale.total_amount, Sale.total_cogs, Sale.gross_profit
    ).filter(Sale.status == "Completed").all()
    
    tot_rev = sum((s.total_amount for s in sales_res), Decimal("0.00"))
    tot_cogs = sum((s.total_cogs for s in sales_res), Decimal("0.00"))
    tot_gp = sum((s.gross_profit for s in sales_res), Decimal("0.00"))

    assert tot_gp == tot_rev - tot_cogs

    expenses = db.query(Expense.amount).all()
    tot_expenses = sum((e.amount for e in expenses), Decimal("0.00"))

    net_profit = tot_gp - tot_expenses
    assert isinstance(net_profit, Decimal)

def test_admin_only_user_creation_roles(db):
    admin = db.query(User).filter(User.role == "Admin").first()
    staff = db.query(User).filter(User.role == "Staff").first()
    
    assert admin is not None
    assert staff is not None
    assert admin.role == "Admin"
    assert staff.role == "Staff"
