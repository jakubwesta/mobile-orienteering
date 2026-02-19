from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from datetime import datetime, timedelta, timezone
from typing import Optional

from app.modules.user.models import User
from app.modules.auth.models import RefreshToken, UserIdentity, ProviderEnum
from app.modules.auth.schemas import RegisterRequest
from app.core.security import hash_password, verify_password, create_access_token, create_refresh_token
from app.core.exceptions import UnauthorizedException, ConflictException, NotFoundException
from app.core.config import config


async def register_user(db: AsyncSession, data: RegisterRequest) -> User:
  existing_username = await db.execute(select(User).where(User.username == data.username))
  if existing_username.scalar_one_or_none():
    raise ConflictException("Username already exists")
  
  existing_email = await db.execute(select(User).where(User.email == data.email))
  if existing_email.scalar_one_or_none():
    raise ConflictException("Email already exists")
  
  user = User(
    username=data.username,
    email=data.email,
    full_name=data.full_name,
    phone_number=data.phone_number,
    password_hash=hash_password(data.password)
  )
  
  db.add(user)
  await db.commit()
  await db.refresh(user)
  return user


async def authenticate_user(db: AsyncSession, username: str, password: str) -> User:
  result = await db.execute(select(User).where(User.username == username))
  user = result.scalar_one_or_none()
  
  if not user or not user.password_hash:
    raise UnauthorizedException("Invalid username or password")
  
  if not verify_password(password, user.password_hash):
    raise UnauthorizedException("Invalid username or password")
  
  return user


async def create_tokens_for_user(db: AsyncSession, user_id: int) -> tuple[str, str]:
  access_token = create_access_token(user_id)
  refresh_token_str = create_refresh_token(user_id)
  
  refresh_token = RefreshToken(
    user_id=user_id,
    token_hash=hash_password(refresh_token_str),
    expires_at=datetime.now(timezone.utc) + timedelta(days=config.REFRESH_TOKEN_EXPIRE_DAYS)
  )
  
  db.add(refresh_token)
  await db.commit()
  
  return access_token, refresh_token_str


async def refresh_access_token(db: AsyncSession, refresh_token_str: str) -> str:
  from app.core.security import get_user_id_from_token
  
  try:
    user_id = get_user_id_from_token(refresh_token_str)
  except Exception:
    raise UnauthorizedException("Invalid refresh token")
  
  result = await db.execute(
    select(RefreshToken)
    .where(RefreshToken.user_id == user_id)
    .where(RefreshToken.revoked_at.is_(None))
    .where(RefreshToken.expires_at > datetime.now(timezone.utc))
  )
  
  stored_tokens = result.scalars().all()
  
  valid_token = None
  for token in stored_tokens:
    if verify_password(refresh_token_str, token.token_hash):
      valid_token = token
      break
  
  if not valid_token:
    raise UnauthorizedException("Invalid or expired refresh token")
  
  access_token = create_access_token(user_id)
  return access_token


async def get_or_create_user_by_google(db: AsyncSession, google_sub: str, email: str, name: Optional[str]) -> User:
  result = await db.execute(
    select(UserIdentity)
    .where(UserIdentity.provider == ProviderEnum.GOOGLE)
    .where(UserIdentity.provider_subject == google_sub)
  )
  identity = result.scalar_one_or_none()
  
  if identity:
    user_result = await db.execute(select(User).where(User.id == identity.user_id))
    user = user_result.scalar_one_or_none()
    if not user:
      raise NotFoundException("User not found for identity")
    return user
  
  existing_email = await db.execute(select(User).where(User.email == email))
  if existing_email.scalar_one_or_none():
    raise ConflictException("Email already registered with different provider")
  
  username = email.split("@")[0]
  counter = 1
  original_username = username
  while True:
    check = await db.execute(select(User).where(User.username == username))
    if not check.scalar_one_or_none():
      break
    username = f"{original_username}{counter}"
    counter += 1
  
  user = User(
    username=username,
    email=email,
    full_name=name
  )
  db.add(user)
  await db.flush()
  
  identity = UserIdentity(
    user_id=user.id,
    provider=ProviderEnum.GOOGLE,
    provider_subject=google_sub
  )
  db.add(identity)
  
  await db.commit()
  await db.refresh(user)
  return user
