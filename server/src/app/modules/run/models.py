import enum

from sqlalchemy import Boolean, DateTime, Enum, ForeignKey, Float
from sqlalchemy.orm import Mapped, mapped_column, relationship
from datetime import datetime
from typing import Optional, TYPE_CHECKING

from app.core.db import Base

if TYPE_CHECKING:
  from app.modules.user.models import User
  from app.modules.map.models import Map


class TimerStart(str, enum.Enum):
  race_start = "race_start"
  first_point = "first_point"


class RaceStyle(str, enum.Enum):
  standard = "standard"
  compass_bearing = "compass_bearing"


class OrientationType(str, enum.Enum):
  foot = "foot"
  bicycle = "bicycle"


class RunSettings(Base):
  __tablename__ = "run_settings"

  id: Mapped[int] = mapped_column(primary_key=True)
  run_id: Mapped[int] = mapped_column(ForeignKey("run.id"), nullable=False, unique=True)
  detection_radius: Mapped[float] = mapped_column(Float, nullable=False)
  show_self_on_map: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
  ordered_control_points: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
  timer_start: Mapped[TimerStart] = mapped_column(
    Enum(TimerStart, native_enum=False), default=TimerStart.race_start, nullable=False
  )
  race_style: Mapped[RaceStyle] = mapped_column(
    Enum(RaceStyle, native_enum=False), default=RaceStyle.standard, nullable=False
  )
  orientation_type: Mapped[OrientationType] = mapped_column(
    Enum(OrientationType, native_enum=False), default=OrientationType.foot, nullable=False
  )

  run: Mapped["Run"] = relationship("Run", back_populates="run_settings")


class Run(Base):
  __tablename__ = "run"

  id: Mapped[int] = mapped_column(primary_key=True)
  user_id: Mapped[int] = mapped_column(ForeignKey("user.id"), nullable=False)
  map_id: Mapped[int] = mapped_column(ForeignKey("map.id"), nullable=False)
  name: Mapped[str] = mapped_column(nullable=False)
  started_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
  finished_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)

  user: Mapped["User"] = relationship("User", back_populates="runs")
  map: Mapped["Map"] = relationship("Map", back_populates="runs")
  run_settings: Mapped["RunSettings"] = relationship(
    "RunSettings", back_populates="run",
    cascade="all, delete-orphan", uselist=False
  )
  path_points: Mapped[list["PathPoint"]] = relationship("PathPoint", back_populates="run")


class PathPoint(Base):
  __tablename__ = "path_point"

  id: Mapped[int] = mapped_column(primary_key=True)
  run_id: Mapped[int] = mapped_column(ForeignKey("run.id"), nullable=False)
  lat: Mapped[float] = mapped_column(Float, nullable=False)
  lon: Mapped[float] = mapped_column(Float, nullable=False)
  timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)

  run: Mapped["Run"] = relationship("Run", back_populates="path_points")
