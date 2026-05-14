from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update
from sqlalchemy.orm import selectinload

from app.modules.map.models import Map, ControlPoint
from app.modules.map.schemas import MapCreate, MapUpdate
from app.core.exceptions import NotFoundException, ForbiddenException
from app.core import r2


async def create_map(db: AsyncSession, user_id: int, data: MapCreate) -> Map:
  map_obj = Map(
    user_id=user_id,
    name=data.name,
    description=data.description,
    is_snapshot=False
  )
  
  db.add(map_obj)
  await db.flush()
  
  for cp_data in data.control_points:
    control_point = ControlPoint(
      map_id=map_obj.id,
      lat=cp_data.lat,
      lon=cp_data.lon,
      name=cp_data.name,
      sequence=cp_data.sequence
    )
    db.add(control_point)
  
  await db.commit()
  await db.refresh(map_obj)
  
  result = await db.execute(
    select(Map)
    .where(Map.id == map_obj.id)
    .options(selectinload(Map.control_points))
  )
  return result.scalar_one()


async def get_map_by_id(db: AsyncSession, map_id: int, user_id: int) -> Map:
  result = await db.execute(
    select(Map)
    .where(Map.id == map_id)
    .options(selectinload(Map.control_points))
  )
  map_obj = result.scalar_one_or_none()
  
  if not map_obj:
    raise NotFoundException(f"Map with id {map_id} not found")
  
  if map_obj.user_id != user_id:
    raise ForbiddenException("You don't have access to this map")
  
  return map_obj


async def get_user_maps(db: AsyncSession, user_id: int) -> list[Map]:
  result = await db.execute(
    select(Map)
    .where(Map.user_id == user_id)
    .where(Map.is_snapshot == False)
    .options(selectinload(Map.control_points))
    .order_by(Map.created_at.desc())
  )
  return list(result.scalars().all())


async def update_map(db: AsyncSession, map_id: int, user_id: int, data: MapUpdate) -> Map:
  map_obj = await get_map_by_id(db, map_id, user_id)
  
  if map_obj.is_snapshot:
    raise ForbiddenException("Cannot update snapshot maps")
  
  map_obj.name = data.name
  map_obj.description = data.description

  for cp in map_obj.control_points:
    await db.delete(cp)
  
  for cp_data in data.control_points:
    control_point = ControlPoint(
      map_id=map_obj.id,
      lat=cp_data.lat,
      lon=cp_data.lon,
      name=cp_data.name,
      sequence=cp_data.sequence
    )
    db.add(control_point)
  
  await db.commit()
  await db.refresh(map_obj)
  
  result = await db.execute(
    select(Map)
    .where(Map.id == map_obj.id)
    .options(selectinload(Map.control_points))
  )
  return result.scalar_one()


async def delete_map(db: AsyncSession, map_id: int, user_id: int) -> None:
  map_obj = await get_map_by_id(db, map_id, user_id)
  
  if map_obj.is_snapshot:
    raise ForbiddenException("Cannot delete snapshot maps")
  
  image_key = map_obj.image_key

  await db.execute(
    update(Map)
    .where(Map.original_map_id == map_id)
    .values(original_map_id=None)
  )
  
  await db.delete(map_obj)
  await db.commit()

  if image_key and r2.is_r2_configured():
    r2.delete_object(image_key)


async def set_map_image_key(db: AsyncSession, map_id: int, user_id: int, object_key: str) -> Map:
  map_obj = await get_map_by_id(db, map_id, user_id)
  map_obj.image_key = object_key
  await db.commit()
  await db.refresh(map_obj)
  result = await db.execute(
    select(Map)
    .where(Map.id == map_obj.id)
    .options(selectinload(Map.control_points))
  )
  return result.scalar_one()


async def clear_map_image_key(db: AsyncSession, map_id: int, user_id: int) -> Map:
  map_obj = await get_map_by_id(db, map_id, user_id)
  old_key = map_obj.image_key

  map_obj.image_key = None
  await db.commit()
  await db.refresh(map_obj)

  if old_key and r2.is_r2_configured():
    r2.delete_object(old_key)

  result = await db.execute(
    select(Map)
    .where(Map.id == map_obj.id)
    .options(selectinload(Map.control_points))
  )
  return result.scalar_one()
