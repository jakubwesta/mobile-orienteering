import logging
from fastapi import APIRouter

from app.modules.health.router import health_router

logger = logging.getLogger(__name__)

api_router = APIRouter()

api_router.include_router(health_router)

logger.debug("API router initialized with all endpoints")
