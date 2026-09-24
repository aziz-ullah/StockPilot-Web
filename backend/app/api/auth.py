from datetime import timedelta
from typing import List
from fastapi import APIRouter, Depends, HTTPException, status
from fastapi.security import OAuth2PasswordBearer, OAuth2PasswordRequestForm
from sqlalchemy.orm import Session
import jwt
from app.core.config import settings
from app.core.database import get_db
from app.core.security import verify_password, create_access_token, get_password_hash
from app.models.domain import User, Organization
from app.schemas.domain import Token, UserOut, UserCreate, LoginRequest

router = APIRouter(prefix="/auth", tags=["Authentication & User Management"])

from typing import Optional, List
from fastapi import APIRouter, Depends, HTTPException, status, Query

oauth2_scheme = OAuth2PasswordBearer(
    tokenUrl=f"{settings.API_V1_STR}/auth/login-form",
    auto_error=False
)

def get_current_user(
    token_header: Optional[str] = Depends(oauth2_scheme),
    token_query: Optional[str] = Query(None, alias="token"),
    db: Session = Depends(get_db)
) -> User:
    credentials_exception = HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail="Not authenticated",
        headers={"WWW-Authenticate": "Bearer"},
    )
    token = token_header or token_query
    if not token:
        raise credentials_exception
    try:
        payload = jwt.decode(token, settings.SECRET_KEY, algorithms=[settings.ALGORITHM])
        user_id_str: str = payload.get("sub")
        if user_id_str is None:
            raise credentials_exception
        user_id = int(user_id_str)
    except (jwt.PyJWTError, ValueError):
        raise credentials_exception
        
    user = db.query(User).filter(User.id == user_id, User.is_active == True).first()
    if user is None:
        raise credentials_exception
    return user

def require_role(roles: list[str]):
    def role_checker(current_user: User = Depends(get_current_user)):
        if current_user.role not in roles:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail=f"Operation not permitted. Required role: {', '.join(roles)}"
            )
        return current_user
    return role_checker

@router.post("/login", response_model=Token)
def login_json(payload: LoginRequest, db: Session = Depends(get_db)):
    user = db.query(User).filter(
        (User.username == payload.username_or_email) | (User.email == payload.username_or_email)
    ).first()
    
    if not user or not verify_password(payload.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username/email or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
        
    access_token = create_access_token(subject=user.id)
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user": user
    }

@router.post("/login-form", response_model=Token)
def login_form(form_data: OAuth2PasswordRequestForm = Depends(), db: Session = Depends(get_db)):
    user = db.query(User).filter(
        (User.username == form_data.username) | (User.email == form_data.username)
    ).first()
    
    if not user or not verify_password(form_data.password, user.hashed_password):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Incorrect username or password",
            headers={"WWW-Authenticate": "Bearer"},
        )
        
    access_token = create_access_token(subject=user.id)
    return {
        "access_token": access_token,
        "token_type": "bearer",
        "user": user
    }

@router.get("/me", response_model=UserOut)
def get_me(current_user: User = Depends(get_current_user)):
    return current_user

@router.get("/users", response_model=List[UserOut])
def list_users(
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_role(["Admin"]))
):
    """
    List all internal user accounts for the admin's organization. Only Admins can access.
    """
    return db.query(User).filter(User.organization_id == admin_user.organization_id).order_by(User.created_at.desc()).all()

@router.post("/users", response_model=UserOut)
def create_user(
    payload: UserCreate,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_role(["Admin"]))
):
    """
    Creates a user account (Staff, Manager, or Admin). Strictly restricted to Admin users only.
    """
    existing = db.query(User).filter(
        (User.username == payload.username) | (User.email == payload.email)
    ).first()
    if existing:
        raise HTTPException(status_code=400, detail="User with this email or username already exists")
        
    user = User(
        organization_id=admin_user.organization_id,
        email=payload.email,
        username=payload.username,
        hashed_password=get_password_hash(payload.password),
        full_name=payload.full_name,
        role=payload.role
    )
    db.add(user)
    db.commit()
    db.refresh(user)
    return user

@router.put("/users/{user_id}", response_model=UserOut)
def update_user_status(
    user_id: int,
    role: str = None,
    is_active: bool = None,
    db: Session = Depends(get_db),
    admin_user: User = Depends(require_role(["Admin"]))
):
    """
    Updates user role or active status. Strictly restricted to Admin users only.
    """
    target_user = db.query(User).filter(
        User.id == user_id,
        User.organization_id == admin_user.organization_id
    ).first()
    if not target_user:
        raise HTTPException(status_code=404, detail="User not found")
        
    if role is not None:
        target_user.role = role
    if is_active is not None:
        target_user.is_active = is_active
        
    db.commit()
    db.refresh(target_user)
    return target_user
