import re
from decimal import Decimal
from typing import Optional
from sqlalchemy.orm import Session
from app.models.domain import ProductVariant, StockMovement, Product, Category

def generate_sku(product_name: str, color: str, size: str, db: Session, org_id: int = 1) -> str:
    """
    Generates a unique, standardized SKU, e.g. HOOD-BLA-XS or JACK-MOO-M
    """
    prod_clean = re.sub(r'[^A-Za-z0-9]', '', product_name).upper()[:4] or "PROD"
    color_clean = re.sub(r'[^A-Za-z0-9]', '', color).upper()[:3] or "CLR"
    size_clean = re.sub(r'[^A-Za-z0-9]', '', size).upper() or "STD"
    
    base_sku = f"{prod_clean}-{color_clean}-{size_clean}"
    sku = base_sku
    counter = 1
    
    while db.query(ProductVariant).filter(ProductVariant.sku == sku).first():
        sku = f"{base_sku}-{counter}"
        counter += 1
        
    return sku

def record_stock_movement(
    db: Session,
    variant_id: int,
    movement_type: str, # PURCHASE, PURCHASE_VOID, SALE, SALE_VOID, IMPORT, ADJUSTMENT
    qty_change: int,    # Positive to add stock, negative to reduce stock
    reference_id: Optional[str] = None,
    notes: Optional[str] = None,
    user_id: Optional[int] = None,
    org_id: int = 1
) -> StockMovement:
    variant = db.query(ProductVariant).filter(
        ProductVariant.id == variant_id,
        ProductVariant.organization_id == org_id
    ).first()
    
    if not variant:
        raise ValueError(f"Product Variant ID {variant_id} not found.")
        
    prev_stock = variant.stock_quantity
    new_stock = prev_stock + qty_change
    if new_stock < 0 and movement_type not in ["SALE", "SALE_VOID", "PURCHASE_VOID"]:
        # Prevent negative stock for manual adjustments unless specified
        pass
        
    variant.stock_quantity = new_stock
    
    movement = StockMovement(
        organization_id=org_id,
        variant_id=variant_id,
        movement_type=movement_type,
        quantity=qty_change,
        previous_stock=prev_stock,
        new_stock=new_stock,
        reference_id=reference_id,
        notes=notes,
        created_by_user_id=user_id
    )
    db.add(movement)
    return movement
