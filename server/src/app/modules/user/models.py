from sqlalchemy import String, DateTime
from sqlalchemy.orm import Mapped, mapped_column, relationship
from datetime import datetime, timezone
from typing import Optional, TYPE_CHECKING

from app.core.db import Base

if TYPE_CHECKING:
  from app.modules.auth.models import UserIdentity, RefreshToken
  from app.modules.run.models import Run


class User(Base):
  __tablename__ = "user"

  id: Mapped[int] = mapped_column(primary_key=True)
  username: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
  email: Mapped[str] = mapped_column(String(255), unique=True, nullable=False)
  full_name: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
  phone_number: Mapped[Optional[str]] = mapped_column(String(50), nullable=True)
  password_hash: Mapped[Optional[str]] = mapped_column(String(255), nullable=True)
  created_at: Mapped[datetime] = mapped_column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)

  identities: Mapped[list["UserIdentity"]] = relationship("UserIdentity", back_populates="user")
  refresh_tokens: Mapped[list["RefreshToken"]] = relationship("RefreshToken", back_populates="user")
  runs: Mapped[list["Run"]] = relationship("Run", back_populates="user")
