from fastapi import APIRouter, Depends, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.db import get_db
from app.core.security import get_current_user_id
from app.core import r2
from app.core.exceptions import BadRequestException, InternalServerException
from app.modules.map.schemas import MapCreate, MapUpdate, MapResponse, MapImageUploadUrlResponse
from app.modules.map.crud import (
  create_map,
  get_user_maps,
  get_map_by_id,
  update_map,
  delete_map,
  set_map_image_key,
  clear_map_image_key,
)

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


@map_router.get("/{map_id}/image/upload-url", response_model=MapImageUploadUrlResponse)
async def get_image_upload_url(
  map_id: int,
  content_type: str = Query(default="image/jpeg", alias="contentType"),
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  if not r2.is_r2_configured():
    raise InternalServerException("Storage is not configured")

  await get_map_by_id(db, map_id, current_user_id)

  try:
    result = r2.generate_presigned_upload_url(map_id, content_type)
  except ValueError as exc:
    raise BadRequestException(str(exc))
  except Exception:
    raise InternalServerException("Failed to generate upload URL")

  return MapImageUploadUrlResponse(**result)


@map_router.post("/{map_id}/image/confirm", response_model=MapResponse)
async def confirm_image_upload(
  map_id: int,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  object_key = r2.map_image_key(map_id)
  return await set_map_image_key(db, map_id, current_user_id, object_key)


@map_router.delete("/{map_id}/image", status_code=status.HTTP_204_NO_CONTENT)
async def delete_image(
  map_id: int,
  current_user_id: int = Depends(get_current_user_id),
  db: AsyncSession = Depends(get_db)
):
  await clear_map_image_key(db, map_id, current_user_id)
