from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select
from sqlalchemy.orm import selectinload

from app.modules.run.models import Run, RunSettings, PathPoint
from app.modules.map.models import Map, ControlPoint
from app.modules.run.schemas import RunCreate
from app.core.exceptions import NotFoundException, ForbiddenException


async def create_map_snapshot(db: AsyncSession, original_map_id: int, user_id: int) -> Map:
  result = await db.execute(
    select(Map)
    .where(Map.id == original_map_id)
    .options(selectinload(Map.control_points))
  )
  original_map = result.scalar_one_or_none()
  
  if not original_map:
    raise NotFoundException(f"Map with id {original_map_id} not found")
  
  if original_map.user_id != user_id:
    raise ForbiddenException("You don't have access to this map")
  
  snapshot = Map(
    user_id=user_id,
    name=original_map.name,
    description=original_map.description,
    is_snapshot=True,
    original_map_id=original_map_id
  )
  
  db.add(snapshot)
  await db.flush()
  
  for cp in original_map.control_points:
    snapshot_cp = ControlPoint(
      map_id=snapshot.id,
      lat=cp.lat,
      lon=cp.lon,
      name=cp.name,
      sequence=cp.sequence
    )
    db.add(snapshot_cp)
  
  await db.flush()
  return snapshot


async def create_run(db: AsyncSession, user_id: int, data: RunCreate) -> Run:
  snapshot = await create_map_snapshot(db, data.map_id, user_id)
  
  run_settings = RunSettings(detection_radius=data.detection_radius)
  db.add(run_settings)
  await db.flush()
  
  run = Run(
    user_id=user_id,
    map_id=snapshot.id,
    run_settings_id=run_settings.id,
    name=data.name,
    started_at=data.started_at
  )
  
  db.add(run)
  await db.commit()
  await db.refresh(run)
  
  result = await db.execute(
    select(Run)
    .where(Run.id == run.id)
    .options(selectinload(Run.run_settings))
    .options(selectinload(Run.path_points))
  )
  return result.scalar_one()


async def get_run_by_id(db: AsyncSession, run_id: int, user_id: int) -> Run:
  result = await db.execute(
    select(Run)
    .where(Run.id == run_id)
    .options(selectinload(Run.run_settings))
    .options(selectinload(Run.path_points))
  )
  run = result.scalar_one_or_none()
  
  if not run:
    raise NotFoundException(f"Run with id {run_id} not found")
  
  if run.user_id != user_id:
    raise ForbiddenException("You don't have access to this run")
  
  return run


async def get_user_runs(db: AsyncSession, user_id: int) -> list[Run]:
  result = await db.execute(
    select(Run)
    .where(Run.user_id == user_id)
    .options(selectinload(Run.run_settings))
    .options(selectinload(Run.path_points))
    .order_by(Run.started_at.desc())
  )
  return list(result.scalars().all())


async def delete_run(db: AsyncSession, run_id: int, user_id: int) -> None:
  run = await get_run_by_id(db, run_id, user_id)
  
  await db.delete(run)
  await db.commit()
