from pydantic import BaseModel, ConfigDict
from datetime import datetime
from typing import Optional

from app.modules.map.schemas import MapResponse


class PathPointBase(BaseModel):
  lat: float
  lon: float
  timestamp: datetime


class PathPointCreate(PathPointBase):
  pass


class PathPointResponse(PathPointBase):
  model_config = ConfigDict(from_attributes=True)

  id: int
  run_id: int


class RunSettingsBase(BaseModel):
  detection_radius: float


class RunSettingsCreate(RunSettingsBase):
  pass


class RunSettingsResponse(RunSettingsBase):
  model_config = ConfigDict(from_attributes=True)

  id: int


class RunBase(BaseModel):
  name: str


class RunCreate(RunBase):
  map_id: int
  run_settings: RunSettingsCreate
  started_at: datetime
  finished_at: Optional[datetime] = None
  path_points: list[PathPointCreate] = []


class RunResponse(RunBase):
  model_config = ConfigDict(from_attributes=True)

  id: int
  user_id: int
  started_at: datetime
  finished_at: Optional[datetime] = None
  run_settings: RunSettingsResponse
  path_points: list[PathPointResponse] = []
  map: MapResponse
