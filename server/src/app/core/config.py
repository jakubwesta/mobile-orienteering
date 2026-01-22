from functools import lru_cache
import os


class Config:
  """Application config"""
  USE_SQLITE: bool = os.getenv("USE_SQLITE", "true").lower() == "true"
  API_PREFIX: str = "/api"
  VERSION: str = "0.1.0"

  # PostgreSQL settings
  POSTGRES_HOST: str = os.getenv("POSTGRES_HOST", "localhost")
  POSTGRES_PORT: int = int(os.getenv("POSTGRES_PORT", "5432"))
  POSTGRES_USER: str = os.getenv("POSTGRES_USER", "postgres")
  POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "password")
  POSTGRES_DB: str = os.getenv("POSTGRES_DB", "mobile_orienteering")

  # JWT settings
  SECRET_KEY: str = os.getenv("SECRET_KEY", "secret_key")
  ALGORITHM: str = os.getenv("ALGORITHM", "HS256")
  ACCESS_TOKEN_EXPIRE_HOURS: int = int(os.getenv("ACCESS_TOKEN_EXPIRE_HOURS", "24"))
  REFRESH_TOKEN_EXPIRE_DAYS: int = int(os.getenv("REFRESH_TOKEN_EXPIRE_DAYS", "365"))


@lru_cache()
def get_config() -> Config:
  return Config()


config = get_config()
