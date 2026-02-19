import logging
from fastapi import APIRouter

from app.modules.health.router import health_router
from app.modules.auth.router import auth_router
from app.modules.user.router import user_router
from app.modules.map.router import map_router
from app.modules.run.router import run_router

logger = logging.getLogger(__name__)

api_router = APIRouter()

api_router.include_router(health_router)
api_router.include_router(auth_router)
api_router.include_router(user_router)
api_router.include_router(map_router)
api_router.include_router(run_router)

logger.debug("API router initialized with all endpoints")
