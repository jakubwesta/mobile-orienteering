from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db
from app.core.security import get_current_user_id
from app.modules.run.schemas import RunCreate, RunResponse
from app.modules.run.crud import create_run, get_user_runs, delete_run

run_router = APIRouter(prefix="/runs", tags=["runs"])


@run_router.post("/", response_model=RunResponse, status_code=status.HTTP_201_CREATED)
async def create_run_endpoint(
  data: RunCreate,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await create_run(db, current_user_id, data)


@run_router.get("/", response_model=list[RunResponse])
async def get_runs(
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await get_user_runs(db, current_user_id)


@run_router.delete("/{run_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_run_endpoint(
  run_id: int,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  await delete_run(db, run_id, current_user_id)
