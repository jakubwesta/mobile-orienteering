from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db
from app.core.security import get_current_user_id
from app.modules.map.schemas import MapCreate, MapUpdate, MapResponse
from app.modules.map.crud import create_map, get_user_maps, get_map_by_id, update_map, delete_map

map_router = APIRouter(prefix="/maps", tags=["maps"])


@map_router.post("/", response_model=MapResponse, status_code=status.HTTP_201_CREATED)
async def create_map_endpoint(
  data: MapCreate,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await create_map(db, current_user_id, data)


@map_router.get("/", response_model=list[MapResponse])
async def get_maps(
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await get_user_maps(db, current_user_id)


@map_router.put("/{map_id}", response_model=MapResponse)
async def update_map_endpoint(
  map_id: int,
  data: MapUpdate,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  return await update_map(db, map_id, current_user_id, data)


@map_router.delete("/{map_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_map_endpoint(
  map_id: int,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  await delete_map(db, map_id, current_user_id)
