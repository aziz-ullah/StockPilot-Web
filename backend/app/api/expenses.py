from datetime import datetime
from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session
from app.core.database import get_db
from app.api.auth import get_current_user
from app.models.domain import Expense, User
from app.schemas.domain import ExpenseCreate, ExpenseOut

router = APIRouter(prefix="/expenses", tags=["Expenses"])

EXPENSE_CATEGORIES = [
    "Rent", "Utilities", "Shipping & Delivery", "Packaging",
    "Salaries & Wages", "Marketing", "Maintenance", "Taxes", "Other"
]

@router.get("/categories", response_model=List[str])
def get_expense_categories():
    return EXPENSE_CATEGORIES

@router.get("", response_model=List[ExpenseOut])
def list_expenses(
    category: Optional[str] = None,
    start_date: Optional[datetime] = None,
    end_date: Optional[datetime] = None,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    query = db.query(Expense).filter(Expense.organization_id == current_user.organization_id)
    if category:
        query = query.filter(Expense.category == category)
    if start_date:
        query = query.filter(Expense.expense_date >= start_date)
    if end_date:
        query = query.filter(Expense.expense_date <= end_date)
        
    expenses = query.order_by(Expense.expense_date.desc()).all()
    results = []
    for e in expenses:
        e_dto = ExpenseOut.model_validate(e)
        if e.created_by:
            e_dto.created_by_name = e.created_by.full_name
        results.append(e_dto)
    return results

@router.post("", response_model=ExpenseOut)
def create_expense(
    payload: ExpenseCreate,
    db: Session = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    expense = Expense(
        organization_id=current_user.organization_id,
        category=payload.category,
        description=payload.description,
        amount=payload.amount,
        expense_date=payload.expense_date or datetime.now(),
        created_by_user_id=current_user.id
    )
    db.add(expense)
    db.commit()
    db.refresh(expense)
    
    e_dto = ExpenseOut.model_validate(expense)
    if expense.created_by:
        e_dto.created_by_name = expense.created_by.full_name
    return e_dto
