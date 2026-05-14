from pydantic import BaseModel, ConfigDict, computed_field
from datetime import datetime
from typing import Optional

from app.core.r2 import get_image_url


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
  image_key: Optional[str] = None

  @computed_field
  @property
  def image_url(self) -> Optional[str]:
    return get_image_url(self.image_key)


class MapImageUploadUrlResponse(BaseModel):
  upload_url: str
  object_key: str
  expires_in: int
