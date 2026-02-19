from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db
from app.core.security import get_current_user_id
from app.modules.user.schemas import UserResponse, UserUpdate
from app.modules.user.crud import get_user_by_id, update_user
from app.core.exceptions import ForbiddenException

user_router = APIRouter(prefix="/users", tags=["users"])


@user_router.get("/me", response_model=UserResponse)
async def get_current_user(
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await get_user_by_id(db, current_user_id)


@user_router.patch("/{user_id}", response_model=UserResponse)
async def update_user_endpoint(
  user_id: int,
  user_update: UserUpdate,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  if current_user_id != user_id:
    raise ForbiddenException("You can only update your own profile")
  
  return await update_user(db, user_id, user_update)
