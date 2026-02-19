from pydantic import BaseModel, ConfigDict
from datetime import datetime
from typing import Optional


class ControlPointBase(BaseModel):
  lat: float
  lon: float
  name: str
  sequence: int


class ControlPointCreate(ControlPointBase):
  pass


class ControlPointResponse(ControlPointBase):
  model_config = ConfigDict(from_attributes=True)

  id: int
  map_id: int


class MapBase(BaseModel):
  name: str
  description: Optional[str] = None


class MapCreate(MapBase):
  control_points: list[ControlPointCreate] = []


class MapUpdate(MapBase):
  control_points: list[ControlPointCreate] = []


class MapResponse(MapBase):
  model_config = ConfigDict(from_attributes=True)

  id: int
  user_id: int
  is_snapshot: bool
  original_map_id: Optional[int] = None
  created_at: datetime
  control_points: list[ControlPointResponse] = []
