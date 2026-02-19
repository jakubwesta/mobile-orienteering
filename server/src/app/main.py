import logging
import sys
from contextlib import asynccontextmanager

import uvicorn
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.exceptions import RequestValidationError
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from jose import JWTError

from app.core.config import config
from app.core.router import api_router
from app.core.db import db_lifespan_context
from app.core.exceptions import AppException
from app.core.error_handlers import (
  app_exception_handler,
  validation_exception_handler,
  integrity_error_handler,
  sqlalchemy_error_handler,
  jwt_error_handler,
  generic_exception_handler,
)

logging.basicConfig(
  level=logging.INFO,
  format="%(asctime)s - %(name)s - %(message)s",
  datefmt="[%Y-%m-%d %H:%M:%S]",
    handlers=[logging.StreamHandler(sys.stdout)]
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

app.add_exception_handler(AppException, app_exception_handler)
app.add_exception_handler(RequestValidationError, validation_exception_handler)
app.add_exception_handler(IntegrityError, integrity_error_handler)
app.add_exception_handler(SQLAlchemyError, sqlalchemy_error_handler)
app.add_exception_handler(JWTError, jwt_error_handler)
app.add_exception_handler(Exception, generic_exception_handler)

app.add_middleware(
  CORSMiddleware,
  allow_origins=[
    "https://mobileorienteering.com",
    "https://www.mobileorienteering.com"
  ],
  allow_credentials=True,
  allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"],
  allow_headers=["Authorization", "Content-Type"],
)


@app.get("/")
async def root():
  return {
    "message": "Mobile Orienteering Backend",
    "version": config.VERSION,
    "health": f"{config.API_PREFIX}/health"
  }


app.include_router(api_router, prefix=config.API_PREFIX)


def main():
  uvicorn.run(
    "app.main:app", 
    host=config.HOST, 
    port=config.PORT, 
    reload=config.RELOAD,
    log_level="info",
    proxy_headers=True,
    forwarded_allow_ips="*",
    access_log=False,
  )
