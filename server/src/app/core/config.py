from functools import lru_cache
from dotenv import load_dotenv
import os

load_dotenv()


class Config:
  """Application config"""
  USE_SQLITE: bool = os.getenv("USE_SQLITE", "true").lower() == "true"
  API_PREFIX: str = "/api"
  VERSION: str = "0.1.0"

  # Uvicorn
  HOST: str = os.getenv("HOST", "0.0.0.0")
  PORT: int = int(os.getenv("PORT", "8000"))
  RELOAD: bool = os.getenv("RELOAD", "true").lower() == "true"

  # PostgreSQL
  POSTGRES_HOST: str = os.getenv("POSTGRES_HOST", "localhost")
  POSTGRES_PORT: int = int(os.getenv("POSTGRES_PORT", "5432"))
  POSTGRES_USER: str = os.getenv("POSTGRES_USER", "postgres")
  POSTGRES_PASSWORD: str = os.getenv("POSTGRES_PASSWORD", "password")
  POSTGRES_DB: str = os.getenv("POSTGRES_DB", "mobile_orienteering")

  # JWT
  SECRET_KEY: str = os.getenv("SECRET_KEY", "secret_key")
  ALGORITHM: str = os.getenv("ALGORITHM", "HS256")
  ACCESS_TOKEN_EXPIRE_HOURS: int = int(os.getenv("ACCESS_TOKEN_EXPIRE_HOURS", "24"))
  REFRESH_TOKEN_EXPIRE_DAYS: int = int(os.getenv("REFRESH_TOKEN_EXPIRE_DAYS", "365"))

  # Google OAuth
  GOOGLE_CLIENT_ID: str = os.getenv("GOOGLE_CLIENT_ID", "")

  # Cloudflare R2
  R2_ENDPOINT_URL: str = os.getenv("R2_ENDPOINT_URL", "")
  R2_ACCESS_KEY_ID: str = os.getenv("R2_ACCESS_KEY_ID", "")
  R2_SECRET_ACCESS_KEY: str = os.getenv("R2_SECRET_ACCESS_KEY", "")
  R2_BUCKET_NAME: str = os.getenv("R2_BUCKET_NAME", "mobile-orienteering")
  R2_PUBLIC_BASE_URL: str = os.getenv("R2_PUBLIC_BASE_URL", "https://mobileorienteering.com")
  R2_PRESIGNED_UPLOAD_EXPIRE_SECONDS: int = int(os.getenv("R2_PRESIGNED_UPLOAD_EXPIRE_SECONDS", "300"))


@lru_cache()
def get_config() -> Config:
  return Config()


config = get_config()
