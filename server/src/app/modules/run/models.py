from sqlalchemy import DateTime, ForeignKey, Float
from sqlalchemy.orm import Mapped, mapped_column, relationship
from datetime import datetime
from typing import Optional, TYPE_CHECKING

from app.core.db import Base

if TYPE_CHECKING:
  from app.modules.user.models import User
  from app.modules.map.models import Map


class RunSettings(Base):
  __tablename__ = "run_settings"

  id: Mapped[int] = mapped_column(primary_key=True)
  detection_radius: Mapped[float] = mapped_column(Float, nullable=False)

  runs: Mapped[list["Run"]] = relationship("Run", back_populates="run_settings")


class Run(Base):
  __tablename__ = "run"

  id: Mapped[int] = mapped_column(primary_key=True)
  user_id: Mapped[int] = mapped_column(ForeignKey("user.id"), nullable=False)
  map_id: Mapped[int] = mapped_column(ForeignKey("map.id"), nullable=False)
  run_settings_id: Mapped[int] = mapped_column(ForeignKey("run_settings.id"), nullable=False)
  name: Mapped[str] = mapped_column(nullable=False)
  started_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
  finished_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)

  user: Mapped["User"] = relationship("User", back_populates="runs")
  map: Mapped["Map"] = relationship("Map", back_populates="runs")
  run_settings: Mapped["RunSettings"] = relationship("RunSettings", back_populates="runs")
  path_points: Mapped[list["PathPoint"]] = relationship("PathPoint", back_populates="run")


class PathPoint(Base):
  __tablename__ = "path_point"

  id: Mapped[int] = mapped_column(primary_key=True)
  run_id: Mapped[int] = mapped_column(ForeignKey("run.id"), nullable=False)
  lat: Mapped[float] = mapped_column(Float, nullable=False)
  lon: Mapped[float] = mapped_column(Float, nullable=False)
  timestamp: Mapped[datetime] = mapped_column(DateTime, nullable=False)

  run: Mapped["Run"] = relationship("Run", back_populates="path_points")
