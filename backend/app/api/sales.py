from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Response
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.domain import Sale, SaleItem, ProductVariant, User
from app.schemas.domain import SaleCreate, SaleOut
from app.services.inventory_service import record_stock_movement
from app.services.pdf_engine import generate_sales_receipt_pdf

router = APIRouter(prefix="/sales", tags=["Sales & POS Checkout"])

@router.get("", response_model=List[SaleOut])
def list_sales(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    sales = db.query(Sale).filter(
        Sale.organization_id == current_user.organization_id
    ).order_by(Sale.sale_date.desc()).all()
    
    results = []
    for s in sales:
        s_dto = SaleOut.model_validate(s)
        if s.created_by:
            s_dto.created_by_name = s.created_by.full_name
        for item in s_dto.items:
            v = db.query(ProductVariant).filter(ProductVariant.id == item.variant_id).first()
            if v:
                item.sku = v.sku
                item.color = v.color
                item.size = v.size
                item.product_name = v.product.name if v.product else None
        results.append(s_dto)
    return results

@router.get("/{sale_id}", response_model=SaleOut)
def get_sale_by_id(
    sale_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    sale = db.query(Sale).filter(
        Sale.id == sale_id,
        Sale.organization_id == current_user.organization_id
    ).first()
    if not sale:
        raise HTTPException(status_code=404, detail="Sale invoice not found")
        
    s_dto = SaleOut.model_validate(sale)
    if sale.created_by:
        s_dto.created_by_name = sale.created_by.full_name
    for item in s_dto.items:
        v = db.query(ProductVariant).filter(ProductVariant.id == item.variant_id).first()
        if v:
            item.sku = v.sku
            item.color = v.color
            item.size = v.size
            item.product_name = v.product.name if v.product else None
    return s_dto

@router.post("", response_model=SaleOut)
def create_sale(
    payload: SaleCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if not payload.items:
        raise HTTPException(status_code=400, detail="POS sale must contain at least one item")
        
    invoice_num = f"INV-{int(datetime.now().timestamp())}"
    
    subtotal = Decimal("0.00")
    total_cogs = Decimal("0.00")
    sale_items_to_create = []

    for item_in in payload.items:
        variant = db.query(ProductVariant).filter(
            ProductVariant.id == item_in.variant_id,
            ProductVariant.organization_id == current_user.organization_id
        ).first()
        if not variant:
            raise HTTPException(status_code=400, detail=f"Variant ID {item_in.variant_id} not found")
            
        if variant.stock_quantity < item_in.quantity:
            raise HTTPException(
                status_code=400,
                detail=f"Insufficient stock for variant '{variant.sku}' ({variant.product.name} {variant.color} {variant.size}). Available: {variant.stock_quantity}, Requested: {item_in.quantity}"
            )
            
        unit_cost = variant.cost_price # HISTORICAL PRICE LOCKING: Lock current unit cost
        unit_price = item_in.unit_price if item_in.unit_price is not None else variant.selling_price # Lock selling price
        
        line_price = Decimal(item_in.quantity) * unit_price
        line_cogs = Decimal(item_in.quantity) * unit_cost
        
        subtotal += line_price
        total_cogs += line_cogs
        
        sale_items_to_create.append({
            "variant": variant,
            "quantity": item_in.quantity,
            "unit_cost": unit_cost,
            "unit_price": unit_price,
            "total_cogs": line_cogs,
            "total_price": line_price
        })

    total_amount = subtotal - payload.discount + payload.tax
    gross_profit = total_amount - total_cogs

    sale = Sale(
        organization_id=current_user.organization_id,
        invoice_number=invoice_num,
        customer_name=payload.customer_name or "Walk-in Customer",
        customer_phone=payload.customer_phone,
        sale_date=payload.sale_date or datetime.now(),
        payment_method=payload.payment_method,
        status="Completed",
        subtotal=subtotal,
        discount=payload.discount,
        tax=payload.tax,
        total_amount=total_amount,
        total_cogs=total_cogs,
        gross_profit=gross_profit,
        notes=payload.notes,
        created_by_user_id=current_user.id
    )
    db.add(sale)
    db.flush()

    for item_data in sale_items_to_create:
        variant = item_data["variant"]
        s_item = SaleItem(
            sale_id=sale.id,
            variant_id=variant.id,
            quantity=item_data["quantity"],
            unit_cost=item_data["unit_cost"],
            unit_price=item_data["unit_price"],
            total_cogs=item_data["total_cogs"],
            total_price=item_data["total_price"]
        )
        db.add(s_item)
        
        # Deduct stock & log SALE movement
        record_stock_movement(
            db=db,
            variant_id=variant.id,
            movement_type="SALE",
            qty_change=-item_data["quantity"],
            reference_id=invoice_num,
            notes=f"POS Sale Invoice #{invoice_num}",
            user_id=current_user.id,
            org_id=current_user.organization_id
        )

    db.commit()
    db.refresh(sale)
    
    s_dto = SaleOut.model_validate(sale)
    if sale.created_by:
        s_dto.created_by_name = sale.created_by.full_name
    for item in s_dto.items:
        v = db.query(ProductVariant).filter(ProductVariant.id == item.variant_id).first()
        if v:
            item.sku = v.sku
            item.color = v.color
            item.size = v.size
            item.product_name = v.product.name if v.product else None
    return s_dto

@router.post("/{sale_id}/void", response_model=SaleOut)
def void_sale(
    sale_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    sale = db.query(Sale).filter(
        Sale.id == sale_id,
        Sale.organization_id == current_user.organization_id
    ).first()
    if not sale:
        raise HTTPException(status_code=404, detail="Sale invoice not found")
        
    if sale.status == "Voided":
        raise HTTPException(status_code=400, detail="Sale invoice is already voided")
        
    sale.status = "Voided"
    
    # Restore stock & log SALE_VOID movement
    for item in sale.items:
        record_stock_movement(
            db=db,
            variant_id=item.variant_id,
            movement_type="SALE_VOID",
            qty_change=item.quantity,
            reference_id=sale.invoice_number,
            notes=f"Voided POS Sale Invoice #{sale.invoice_number}",
            user_id=current_user.id,
            org_id=current_user.organization_id
        )
        
    db.commit()
    db.refresh(sale)
    
    s_dto = SaleOut.model_validate(sale)
    if sale.created_by:
        s_dto.created_by_name = sale.created_by.full_name
    return s_dto

@router.get("/{sale_id}/pdf")
def download_invoice_pdf(
    sale_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    sale = db.query(Sale).filter(
        Sale.id == sale_id,
        Sale.organization_id == current_user.organization_id
    ).first()
    if not sale:
        raise HTTPException(status_code=404, detail="Sale invoice not found")
        
    pdf_bytes = generate_sales_receipt_pdf(sale)
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f"attachment; filename=Invoice_{sale.invoice_number}.pdf"}
    )
