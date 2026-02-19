from sqlalchemy import String, Text, Boolean, DateTime, ForeignKey, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship
from datetime import datetime, timezone
from typing import Optional, TYPE_CHECKING

from app.core.db import Base

if TYPE_CHECKING:
  from app.modules.user.models import User
  from app.modules.run.models import Run


class Map(Base):
  __tablename__ = "map"

  id: Mapped[int] = mapped_column(primary_key=True)
  user_id: Mapped[int] = mapped_column(ForeignKey("user.id"), nullable=False)
  name: Mapped[str] = mapped_column(String(255), nullable=False)
  description: Mapped[Optional[str]] = mapped_column(Text, nullable=True)
  is_snapshot: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
  original_map_id: Mapped[Optional[int]] = mapped_column(ForeignKey("map.id"), nullable=True)
  created_at: Mapped[datetime] = mapped_column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)

  control_points: Mapped[list["ControlPoint"]] = relationship("ControlPoint", back_populates="map")
  runs: Mapped[list["Run"]] = relationship("Run", back_populates="map")


class ControlPoint(Base):
  __tablename__ = "control_point"

  id: Mapped[int] = mapped_column(primary_key=True)
  map_id: Mapped[int] = mapped_column(ForeignKey("map.id"), nullable=False)
  lat: Mapped[float] = mapped_column(nullable=False)
  lon: Mapped[float] = mapped_column(nullable=False)
  name: Mapped[str] = mapped_column(String(255), nullable=False)
  sequence: Mapped[int] = mapped_column(Integer, nullable=False)

  map: Mapped["Map"] = relationship("Map", back_populates="control_points")
