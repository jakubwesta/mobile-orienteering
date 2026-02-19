from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from typing import Optional

from app.modules.user.models import User
from app.modules.user.schemas import UserUpdate
from app.core.exceptions import NotFoundException


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
  
  update_data = user_update.model_dump(exclude_unset=True)
  for field, value in update_data.items():
    setattr(user, field, value)
  
  await db.commit()
  await db.refresh(user)
  return user
