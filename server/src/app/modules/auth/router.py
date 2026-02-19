from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from google.oauth2 import id_token
from google.auth.transport import requests

from app.core.db import get_db
from app.modules.auth.schemas import (
  RegisterRequest,
  LoginRequest,
  GoogleLoginRequest,
  TokenResponse,
  RefreshRequest
)
from app.modules.auth.crud import (
  register_user,
  authenticate_user,
  create_tokens_for_user,
  refresh_access_token,
  get_or_create_user_by_google
)
from app.core.exceptions import UnauthorizedException

auth_router = APIRouter(prefix="/auth", tags=["auth"])


@auth_router.post("/register", response_model=TokenResponse)
async def register(
  data: RegisterRequest,
  db: AsyncSession = Depends(get_db)
):
  user = await register_user(db, data)
  access_token, refresh_token = await create_tokens_for_user(db, user.id)
  
  return TokenResponse(
    access_token=access_token,
    refresh_token=refresh_token
  )


@auth_router.post("/login", response_model=TokenResponse)
async def login(
  data: LoginRequest,
  db: AsyncSession = Depends(get_db)
):
  user = await authenticate_user(db, data.username, data.password)
  access_token, refresh_token = await create_tokens_for_user(db, user.id)
  
  return TokenResponse(
    access_token=access_token,
    refresh_token=refresh_token
  )


@auth_router.post("/login/google", response_model=TokenResponse)
async def login_google(
  data: GoogleLoginRequest,
  db: AsyncSession = Depends(get_db)
):
  try:
    idinfo = id_token.verify_oauth2_token(
      data.id_token,
      requests.Request(),
      None
    )
    
    google_sub = idinfo.get("sub")
    email = idinfo.get("email")
    name = idinfo.get("name")
    
    if not google_sub or not email:
      raise UnauthorizedException("Invalid Google token")
    
    user = await get_or_create_user_by_google(db, google_sub, email, name)
    access_token, refresh_token = await create_tokens_for_user(db, user.id)
    
    return TokenResponse(
      access_token=access_token,
      refresh_token=refresh_token
    )
  except ValueError:
    raise UnauthorizedException("Invalid Google token")


@auth_router.post("/refresh", response_model=TokenResponse)
async def refresh(
  data: RefreshRequest,
  db: AsyncSession = Depends(get_db)
):
  access_token = await refresh_access_token(db, data.refresh_token)
  
  return TokenResponse(
    access_token=access_token,
    refresh_token=data.refresh_token
  )
