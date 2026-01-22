import logging
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.middleware.httpsredirect import HTTPSRedirectMiddleware

from app.core.config import config
from app.core.router import api_router
from app.core.db import db_lifespan_context

logging.basicConfig(
  level=logging.INFO,
  format="%(asctime)s - %(name)s - %(message)s",
  datefmt="[%Y-%m-%d %H:%M:%S]"
)

logging.getLogger('sqlalchemy').setLevel(logging.CRITICAL)
logging.getLogger('sqlalchemy.engine').setLevel(logging.WARNING)
logging.getLogger('aiosqlite').setLevel(logging.WARNING)
logging.getLogger('uvicorn.access').setLevel(logging.WARNING)

logger = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
  logger.info("Starting application lifespan")
  async with db_lifespan_context():
    logger.info("Database context initialized")
    try:
      yield
    finally:
      logger.info("Stopping application lifespan")


app = FastAPI(
  title="Mobile Orienteering Backend",
  version="0.1.0",
  lifespan=lifespan,
  debug=True
)

app.add_middleware(
  CORSMiddleware,
  allow_origins=[
    "https://mobileorienteering.com",
    "https://www.mobileorienteering.com"
  ],
  allow_credentials=True,
  allow_methods=["*"],
  allow_headers=["*"],
)

app.add_middleware(HTTPSRedirectMiddleware)

@app.get("/")
async def root():
  return {
    "message": "Mobile Orienteering Backend",
    "version": config.VERSION,
    "docs": "/docs",
    "health": f"{config.API_PREFIX}/health"
  }

app.include_router(api_router, prefix=config.API_PREFIX)

logger.info(f"Mobile Orienteering Backend configured.")


def main():
  uvicorn.run(
    "app.main:app", 
    host=config.HOST, 
    port=config.PORT, 
    reload=config.RELOAD,
    log_level="info",
    proxy_headers=True,
    forwarded_allow_ips="*"
  )
