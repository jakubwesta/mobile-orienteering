import logging
from fastapi import APIRouter

from app.core.db import db_manager
from app.core.config import config
from app.modules.health.schemas import HealthResponse, DatabaseHealthResponse

logger = logging.getLogger(__name__)

health_router = APIRouter(prefix="/health", tags=["health"])


@health_router.get("/", response_model=HealthResponse)
async def health():
  logger.debug("Health endpoint accessed")
  return {
    "status": "ok",
    "version": config.VERSION,
  }


@health_router.get("/db", response_model=DatabaseHealthResponse)
async def database_health():
  logger.debug("Database health endpoint accessed")
  is_healthy = await db_manager.health_check()
  return {
    "status": "healthy" if is_healthy else "unhealthy"
  }
