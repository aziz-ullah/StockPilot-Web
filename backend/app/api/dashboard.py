from datetime import datetime, timedelta, time
from decimal import Decimal
from typing import Optional, List
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from sqlalchemy import func
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.domain import Sale, Expense, ProductVariant, Product, Category, User
from app.schemas.domain import (
    DashboardKPIs, TrendChartPoint, CategorySalesPoint, LowStockItemOut
)

router = APIRouter(prefix="/dashboard", tags=["Dashboard Overview"])

def get_date_range(preset: str, custom_start: Optional[datetime], custom_end: Optional[datetime]):
    now = datetime.now()
    if preset == "today":
        start = datetime.combine(now.date(), time.min)
        end = datetime.combine(now.date(), time.max)
    elif preset == "this_week":
        start = datetime.combine(now.date() - timedelta(days=now.weekday()), time.min)
        end = datetime.combine(now.date(), time.max)
    elif preset == "this_month":
        start = datetime.combine(now.date().replace(day=1), time.min)
        end = datetime.combine(now.date(), time.max)
    elif preset == "custom" and custom_start and custom_end:
        start = custom_start
        end = custom_end
    else: # all_time
        start = None
        end = None
    return start, end

@router.get("/kpis", response_model=DashboardKPIs)
def get_dashboard_kpis(
    preset: str = Query("all_time", enum=["today", "this_week", "this_month", "all_time", "custom"]),
    custom_start: Optional[datetime] = None,
    custom_end: Optional[datetime] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    org_id = current_user.organization_id
    start, end = get_date_range(preset, custom_start, custom_end)

    # Sales & Profit query
    sales_query = db.query(
        func.coalesce(func.sum(Sale.total_amount), 0).label("revenue"),
        func.coalesce(func.sum(Sale.total_cogs), 0).label("cogs"),
        func.coalesce(func.sum(Sale.gross_profit), 0).label("gross_profit")
    ).filter(
        Sale.organization_id == org_id,
        Sale.status == "Completed"
    )

    if start:
        sales_query = sales_query.filter(Sale.sale_date >= start)
    if end:
        sales_query = sales_query.filter(Sale.sale_date <= end)

    sales_res = sales_query.one()
    revenue = Decimal(str(sales_res.revenue))
    cogs = Decimal(str(sales_res.cogs))
    gross_profit = Decimal(str(sales_res.gross_profit))

    # Expense query
    expense_query = db.query(func.coalesce(func.sum(Expense.amount), 0)).filter(
        Expense.organization_id == org_id
    )
    if start:
        expense_query = expense_query.filter(Expense.expense_date >= start)
    if end:
        expense_query = expense_query.filter(Expense.expense_date <= end)

    total_expenses = Decimal(str(expense_query.scalar()))
    net_profit = gross_profit - total_expenses

    # Inventory Valuation & Low Stock (Always current inventory state)
    stock_val_res = db.query(
        func.coalesce(func.sum(ProductVariant.stock_quantity * ProductVariant.cost_price), 0)
    ).filter(ProductVariant.organization_id == org_id).scalar()
    
    current_stock_valuation = Decimal(str(stock_val_res))

    low_stock_count = db.query(ProductVariant).filter(
        ProductVariant.organization_id == org_id,
        ProductVariant.stock_quantity <= ProductVariant.min_stock_alert
    ).count()

    return DashboardKPIs(
        total_sales_revenue=revenue,
        total_cogs=cogs,
        gross_profit=gross_profit,
        total_expenses=total_expenses,
        net_profit=net_profit,
        current_stock_valuation=current_stock_valuation,
        low_stock_items_count=low_stock_count
    )

@router.get("/charts/trends", response_model=List[TrendChartPoint])
def get_sales_trends(
    preset: str = Query("all_time", enum=["today", "this_week", "this_month", "all_time", "custom"]),
    custom_start: Optional[datetime] = None,
    custom_end: Optional[datetime] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    org_id = current_user.organization_id
    start, end = get_date_range(preset, custom_start, custom_end)

    query = db.query(
        func.to_char(Sale.sale_date, 'YYYY-MM-DD').label("date_str"),
        func.sum(Sale.total_amount).label("revenue"),
        func.sum(Sale.gross_profit).label("gross_profit")
    ).filter(
        Sale.organization_id == org_id,
        Sale.status == "Completed"
    )

    if start:
        query = query.filter(Sale.sale_date >= start)
    if end:
        query = query.filter(Sale.sale_date <= end)

    query = query.group_by(func.to_char(Sale.sale_date, 'YYYY-MM-DD')).order_by(func.to_char(Sale.sale_date, 'YYYY-MM-DD'))
    rows = query.all()

    trend_data = []
    for r in rows:
        dt_str = r.date_str
        rev = Decimal(str(r.revenue or 0))
        gp = Decimal(str(r.gross_profit or 0))
        
        # Calculate expenses for that date
        exp_res = db.query(func.coalesce(func.sum(Expense.amount), 0)).filter(
            Expense.organization_id == org_id,
            func.to_char(Expense.expense_date, 'YYYY-MM-DD') == dt_str
        ).scalar()
        exp = Decimal(str(exp_res))
        np = gp - exp

        trend_data.append(TrendChartPoint(
            date=dt_str,
            sales=rev,
            gross_profit=gp,
            net_profit=np
        ))
    return trend_data

@router.get("/charts/categories", response_model=List[CategorySalesPoint])
def get_sales_by_category(
    preset: str = Query("all_time", enum=["today", "this_week", "this_month", "all_time", "custom"]),
    custom_start: Optional[datetime] = None,
    custom_end: Optional[datetime] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    org_id = current_user.organization_id
    start, end = get_date_range(preset, custom_start, custom_end)

    from app.models.domain import SaleItem
    query = db.query(
        Category.name.label("category_name"),
        func.sum(SaleItem.total_price).label("total_sales"),
        func.sum(SaleItem.quantity).label("total_qty")
    ).select_from(SaleItem)\
     .join(Sale, SaleItem.sale_id == Sale.id)\
     .join(ProductVariant, SaleItem.variant_id == ProductVariant.id)\
     .join(Product, ProductVariant.product_id == Product.id)\
     .join(Category, Product.category_id == Category.id)\
     .filter(Sale.organization_id == org_id, Sale.status == "Completed")

    if start:
        query = query.filter(Sale.sale_date >= start)
    if end:
        query = query.filter(Sale.sale_date <= end)

    query = query.group_by(Category.name)
    rows = query.all()

    return [
        CategorySalesPoint(
            category_name=r.category_name,
            sales=Decimal(str(r.total_sales or 0)),
            quantity_sold=int(r.total_qty or 0)
        )
        for r in rows
    ]

@router.get("/low-stock", response_model=List[LowStockItemOut])
def get_low_stock_table(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    variants = db.query(ProductVariant).join(Product).join(Category).filter(
        ProductVariant.organization_id == current_user.organization_id,
        ProductVariant.stock_quantity <= ProductVariant.min_stock_alert
    ).order_by(ProductVariant.stock_quantity.asc()).all()

    res = []
    for v in variants:
        badge = "OUT_OF_STOCK" if v.stock_quantity <= 0 else "LOW_STOCK"
        res.append(LowStockItemOut(
            variant_id=v.id,
            product_name=v.product.name if v.product else "N/A",
            category_name=v.product.category.name if v.product and v.product.category else "N/A",
            sku=v.sku,
            color=v.color,
            size=v.size,
            stock_quantity=v.stock_quantity,
            min_stock_alert=v.min_stock_alert,
            status_badge=badge
        ))
    return res
