from sqlalchemy import String, DateTime, ForeignKey, Enum
from sqlalchemy.orm import Mapped, mapped_column, relationship
from datetime import datetime, timezone
from typing import Optional, TYPE_CHECKING
import enum

from app.core.db import Base

if TYPE_CHECKING:
  from app.modules.user.models import User


class ProviderEnum(str, enum.Enum):
  GOOGLE = "google"
  FACEBOOK = "facebook"
  APPLE = "apple"
  EMAIL = "email"


class UserIdentity(Base):
  __tablename__ = "user_identity"

  id: Mapped[int] = mapped_column(primary_key=True)
  user_id: Mapped[int] = mapped_column(ForeignKey("user.id"), nullable=False)
  provider: Mapped[ProviderEnum] = mapped_column(Enum(ProviderEnum), nullable=False)
  provider_subject: Mapped[str] = mapped_column(String(255), nullable=False)

  user: Mapped["User"] = relationship("User", back_populates="identities")


class RefreshToken(Base):
  __tablename__ = "refresh_token"

  id: Mapped[int] = mapped_column(primary_key=True)
  user_id: Mapped[int] = mapped_column(ForeignKey("user.id"), nullable=False)
  token_hash: Mapped[str] = mapped_column(String(255), nullable=False)
  created_at: Mapped[datetime] = mapped_column(DateTime, default=lambda: datetime.now(timezone.utc), nullable=False)
  expires_at: Mapped[datetime] = mapped_column(DateTime, nullable=False)
  revoked_at: Mapped[Optional[datetime]] = mapped_column(DateTime, nullable=True)

  user: Mapped["User"] = relationship("User", back_populates="refresh_tokens")
