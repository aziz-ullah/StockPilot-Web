from datetime import datetime
from decimal import Decimal
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.domain import Purchase, PurchaseItem, ProductVariant, User, Supplier
from app.schemas.domain import PurchaseCreate, PurchaseOut, SupplierCreate, SupplierOut
from app.services.inventory_service import record_stock_movement

router = APIRouter(prefix="/purchases", tags=["Purchases & Stock In"])

@router.get("/suppliers", response_model=List[SupplierOut])
def list_suppliers(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    return db.query(Supplier).filter(Supplier.organization_id == current_user.organization_id).all()

@router.post("/suppliers", response_model=SupplierOut)
def create_supplier(
    payload: SupplierCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    supplier = Supplier(
        organization_id=current_user.organization_id,
        name=payload.name,
        contact_person=payload.contact_person,
        phone=payload.phone,
        email=payload.email,
        address=payload.address
    )
    db.add(supplier)
    db.commit()
    db.refresh(supplier)
    return supplier

@router.get("", response_model=List[PurchaseOut])
def list_purchases(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    purchases = db.query(Purchase).filter(
        Purchase.organization_id == current_user.organization_id
    ).order_by(Purchase.purchase_date.desc()).all()
    
    results = []
    for p in purchases:
        p_dto = PurchaseOut.model_validate(p)
        if p.created_by:
            p_dto.created_by_name = p.created_by.full_name
        for item in p_dto.items:
            v = db.query(ProductVariant).filter(ProductVariant.id == item.variant_id).first()
            if v:
                item.sku = v.sku
                item.color = v.color
                item.size = v.size
                item.product_name = v.product.name if v.product else None
        results.append(p_dto)
    return results

@router.post("", response_model=PurchaseOut)
def create_purchase(
    payload: PurchaseCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    if not payload.items:
        raise HTTPException(status_code=400, detail="Purchase must contain at least one item")
        
    po_number = f"PO-{int(datetime.now().timestamp())}"
    total_amount = Decimal("0.00")
    
    purchase = Purchase(
        organization_id=current_user.organization_id,
        purchase_number=po_number,
        supplier_id=payload.supplier_id,
        supplier_name=payload.supplier_name,
        purchase_date=payload.purchase_date or datetime.now(),
        status="Completed",
        payment_status=payload.payment_status,
        total_amount=Decimal("0.00"),
        notes=payload.notes,
        created_by_user_id=current_user.id
    )
    db.add(purchase)
    db.flush()
    
    for item_in in payload.items:
        variant = db.query(ProductVariant).filter(
            ProductVariant.id == item_in.variant_id,
            ProductVariant.organization_id == current_user.organization_id
        ).first()
        if not variant:
            raise HTTPException(status_code=400, detail=f"Variant ID {item_in.variant_id} not found")
            
        line_total = Decimal(item_in.quantity) * item_in.unit_cost
        total_amount += line_total
        
        p_item = PurchaseItem(
            purchase_id=purchase.id,
            variant_id=variant.id,
            quantity=item_in.quantity,
            unit_cost=item_in.unit_cost,
            total_cost=line_total
        )
        db.add(p_item)
        
        # Increase product stock & log PURCHASE movement
        record_stock_movement(
            db=db,
            variant_id=variant.id,
            movement_type="PURCHASE",
            qty_change=item_in.quantity,
            reference_id=po_number,
            notes=f"Stock Purchase PO #{po_number}",
            user_id=current_user.id,
            org_id=current_user.organization_id
        )

    purchase.total_amount = total_amount
    db.commit()
    db.refresh(purchase)
    
    p_dto = PurchaseOut.model_validate(purchase)
    if purchase.created_by:
        p_dto.created_by_name = purchase.created_by.full_name
    for item in p_dto.items:
        v = db.query(ProductVariant).filter(ProductVariant.id == item.variant_id).first()
        if v:
            item.sku = v.sku
            item.color = v.color
            item.size = v.size
            item.product_name = v.product.name if v.product else None
    return p_dto

@router.post("/{purchase_id}/cancel", response_model=PurchaseOut)
def cancel_purchase(
    purchase_id: int,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    purchase = db.query(Purchase).filter(
        Purchase.id == purchase_id,
        Purchase.organization_id == current_user.organization_id
    ).first()
    if not purchase:
        raise HTTPException(status_code=404, detail="Purchase order not found")
        
    if purchase.status == "Cancelled":
        raise HTTPException(status_code=400, detail="Purchase order is already cancelled")
        
    purchase.status = "Cancelled"
    
    # Deduct stock and log PURCHASE_VOID movement
    for item in purchase.items:
        record_stock_movement(
            db=db,
            variant_id=item.variant_id,
            movement_type="PURCHASE_VOID",
            qty_change=-item.quantity,
            reference_id=purchase.purchase_number,
            notes=f"Cancelled Stock Purchase PO #{purchase.purchase_number}",
            user_id=current_user.id,
            org_id=current_user.organization_id
        )
        
    db.commit()
    db.refresh(purchase)
    
    p_dto = PurchaseOut.model_validate(purchase)
    if purchase.created_by:
        p_dto.created_by_name = purchase.created_by.full_name
    return p_dto
