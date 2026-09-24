from typing import List
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.domain import Category, User
from app.schemas.domain import CategoryCreate, CategoryOut

router = APIRouter(prefix="/categories", tags=["Categories"])

@router.get("", response_model=List[CategoryOut])
def list_categories(
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    return db.query(Category).filter(Category.organization_id == current_user.organization_id).all()

@router.post("", response_model=CategoryOut)
def create_category(
    payload: CategoryCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    existing = db.query(Category).filter(
        Category.organization_id == current_user.organization_id,
        Category.name.ilike(payload.name)
    ).first()
    if existing:
        return existing
        
    cat = Category(
        organization_id=current_user.organization_id,
        name=payload.name,
        description=payload.description
    )
    db.add(cat)
    db.commit()
    db.refresh(cat)
    return cat
