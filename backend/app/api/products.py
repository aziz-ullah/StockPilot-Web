from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.orm import Session
from sqlalchemy import or_
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.domain import Product, ProductVariant, StockMovement, Category, User
from app.schemas.domain import (
    ProductCreate, ProductOut, VariantCreate, VariantUpdate, VariantOut, StockMovementOut
)
from app.services.inventory_service import generate_sku, record_stock_movement

router = APIRouter(prefix="/products", tags=["Products & Inventory"])

@router.get("", response_model=List[ProductOut])
def list_products(
    category_id: Optional[int] = None,
    search: Optional[str] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    query = db.query(Product).filter(Product.organization_id == current_user.organization_id)
    if category_id:
        query = query.filter(Product.category_id == category_id)
    if search:
        search_fmt = f"%{search}%"
        query = query.filter(
            or_(
                Product.name.ilike(search_fmt),
                Product.description.ilike(search_fmt),
                Product.variants.any(ProductVariant.sku.ilike(search_fmt)),
                Product.variants.any(ProductVariant.color.ilike(search_fmt))
            )
        )
    
    products = query.order_by(Product.name).all()
    
    # Enrich DTO response
    results = []
    for p in products:
        p_dto = ProductOut.model_validate(p)
        p_dto.category_name = p.category.name if p.category else None
        for v in p_dto.variants:
            v.product_name = p.name
            v.category_name = p_dto.category_name
        results.append(p_dto)
    return results

@router.get("/variants", response_model=List[VariantOut])
def list_variants(
    search: Optional[str] = None,
    low_stock_only: bool = False,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    query = db.query(ProductVariant).join(Product).filter(
        ProductVariant.organization_id == current_user.organization_id
    )
    if search:
        search_fmt = f"%{search}%"
        query = query.filter(
            or_(
                ProductVariant.sku.ilike(search_fmt),
                ProductVariant.color.ilike(search_fmt),
                ProductVariant.size.ilike(search_fmt),
                Product.name.ilike(search_fmt)
            )
        )
    if low_stock_only:
        query = query.filter(ProductVariant.stock_quantity <= ProductVariant.min_stock_alert)
        
    variants = query.all()
    results = []
    for v in variants:
        v_dto = VariantOut.model_validate(v)
        v_dto.product_name = v.product.name if v.product else None
        v_dto.category_name = v.product.category.name if v.product and v.product.category else None
        results.append(v_dto)
    return results

@router.post("", response_model=ProductOut)
def create_product(
    payload: ProductCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    cat = db.query(Category).filter(
        Category.id == payload.category_id,
        Category.organization_id == current_user.organization_id
    ).first()
    if not cat:
        raise HTTPException(status_code=400, detail="Invalid Category ID")
        
    product = Product(
        organization_id=current_user.organization_id,
        category_id=payload.category_id,
        name=payload.name,
        description=payload.description
    )
    db.add(product)
    db.flush()
    
    for v_data in payload.variants:
        sku = v_data.sku or generate_sku(product.name, v_data.color, v_data.size, db, current_user.organization_id)
        variant = ProductVariant(
            organization_id=current_user.organization_id,
            product_id=product.id,
            sku=sku,
            color=v_data.color,
            size=v_data.size,
            cost_price=v_data.cost_price,
            selling_price=v_data.selling_price,
            stock_quantity=v_data.stock_quantity,
            min_stock_alert=v_data.min_stock_alert
        )
        db.add(variant)
        db.flush()
        
        if v_data.stock_quantity > 0:
            record_stock_movement(
                db=db,
                variant_id=variant.id,
                movement_type="ADJUSTMENT",
                qty_change=v_data.stock_quantity,
                notes="Initial product creation stock level",
                user_id=current_user.id,
                org_id=current_user.organization_id
            )

    db.commit()
    db.refresh(product)
    
    p_dto = ProductOut.model_validate(product)
    p_dto.category_name = cat.name
    for v in p_dto.variants:
        v.product_name = product.name
        v.category_name = cat.name
    return p_dto

@router.put("/variants/{variant_id}", response_model=VariantOut)
def update_variant(
    variant_id: int,
    payload: VariantUpdate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    variant = db.query(ProductVariant).filter(
        ProductVariant.id == variant_id,
        ProductVariant.organization_id == current_user.organization_id
    ).first()
    if not variant:
        raise HTTPException(status_code=404, detail="Product variant not found")
        
    if payload.cost_price is not None:
        variant.cost_price = payload.cost_price
    if payload.selling_price is not None:
        variant.selling_price = payload.selling_price
    if payload.color is not None:
        variant.color = payload.color
    if payload.size is not None:
        variant.size = payload.size
    if payload.sku is not None:
        variant.sku = payload.sku
    if payload.min_stock_alert is not None:
        variant.min_stock_alert = payload.min_stock_alert
        
    if payload.stock_quantity is not None and payload.stock_quantity != variant.stock_quantity:
        diff = payload.stock_quantity - variant.stock_quantity
        record_stock_movement(
            db=db,
            variant_id=variant.id,
            movement_type="ADJUSTMENT",
            qty_change=diff,
            notes="Manual inventory quantity update",
            user_id=current_user.id,
            org_id=current_user.organization_id
        )
        
    db.commit()
    db.refresh(variant)
    
    v_dto = VariantOut.model_validate(variant)
    v_dto.product_name = variant.product.name if variant.product else None
    v_dto.category_name = variant.product.category.name if variant.product and variant.product.category else None
    return v_dto

@router.get("/movements", response_model=List[StockMovementOut])
def list_stock_movements(
    limit: int = 100,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    movements = db.query(StockMovement).filter(
        StockMovement.organization_id == current_user.organization_id
    ).order_by(StockMovement.created_at.desc()).limit(limit).all()
    
    results = []
    for m in movements:
        m_dto = StockMovementOut.model_validate(m)
        v = m.variant
        if v:
            m_dto.sku = v.sku
            m_dto.color = v.color
            m_dto.size = v.size
            m_dto.product_name = v.product.name if v.product else None
        if m.user:
            m_dto.created_by_name = m.user.full_name
        results.append(m_dto)
    return results
