from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from typing import Optional

from app.modules.user.models import User
from app.modules.user.schemas import UserUpdate
from app.core.exceptions import NotFoundException, BadRequestException, ConflictException
from app.core.security import verify_password, hash_password


async def get_user_by_id(db: AsyncSession, user_id: int) -> User:
  result = await db.execute(select(User).where(User.id == user_id))
  user = result.scalar_one_or_none()
  if not user:
    raise NotFoundException(f"User with id {user_id} not found")
  return user


async def get_user_by_username(db: AsyncSession, username: str) -> Optional[User]:
  result = await db.execute(select(User).where(User.username == username))
  return result.scalar_one_or_none()


async def get_user_by_email(db: AsyncSession, email: str) -> Optional[User]:
  result = await db.execute(select(User).where(User.email == email))
  return result.scalar_one_or_none()


async def update_user(db: AsyncSession, user_id: int, user_update: UserUpdate) -> User:
  user = await get_user_by_id(db, user_id)

  if user_update.new_password is not None:
    if user_update.old_password is None:
      raise BadRequestException("old_password is required to change password")
    if not user.password_hash or not verify_password(user_update.old_password, user.password_hash):
      raise BadRequestException("Incorrect old password")

  if user_update.username is not None and user_update.username != user.username:
    existing = await get_user_by_username(db, user_update.username)
    if existing:
      raise ConflictException("Username already taken")
    user.username = user_update.username

  if user_update.email is not None and user_update.email != user.email:
    existing = await get_user_by_email(db, user_update.email)
    if existing:
      raise ConflictException("Email already taken")
    user.email = user_update.email

  if user_update.full_name is not None:
    user.full_name = user_update.full_name

  if user_update.phone_number is not None:
    user.phone_number = user_update.phone_number

  if user_update.new_password is not None:
    user.password_hash = hash_password(user_update.new_password)

  await db.commit()
  await db.refresh(user)
  return user
