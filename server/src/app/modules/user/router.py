from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db
from app.core.security import get_current_user_id
from app.modules.user.schemas import UserResponse, UserUpdate
from app.modules.user.crud import get_user_by_id, update_user

user_router = APIRouter(prefix="/users", tags=["users"])


@user_router.get("/me", response_model=UserResponse)
async def get_current_user(
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await get_user_by_id(db, current_user_id)


@user_router.patch("/me", response_model=UserResponse)
async def update_current_user(
  user_update: UserUpdate,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await update_user(db, current_user_id, user_update)
