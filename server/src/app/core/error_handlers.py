import logging
from fastapi import Request, status
from fastapi.responses import JSONResponse
from fastapi.exceptions import RequestValidationError
from sqlalchemy.exc import IntegrityError, SQLAlchemyError
from jose import JWTError

from app.core.exceptions import AppException

logger = logging.getLogger(__name__)


async def app_exception_handler(request: Request, exc: AppException) -> JSONResponse:
  return JSONResponse(
    status_code=exc.status_code,
    content={
      "error": {
        "message": exc.detail,
        "status_code": exc.status_code,
      }
    },
    headers=exc.headers,
  )


async def validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
  errors = []
  for error in exc.errors():
    errors.append({
      "field": ".".join(str(loc) for loc in error["loc"]),
      "message": error["msg"],
      "type": error["type"],
    })

  return JSONResponse(
    status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
    content={
      "error": {
        "message": "Validation error",
        "status_code": status.HTTP_422_UNPROCESSABLE_ENTITY,
        "details": errors,
      }
    },
  )


async def integrity_error_handler(request: Request, exc: IntegrityError) -> JSONResponse:
  logger.error(f"Database integrity error: {exc}")

  detail = "Database constraint violation"
  if "unique constraint" in str(exc).lower():
    detail = "Resource already exists"
  elif "foreign key constraint" in str(exc).lower():
    detail = "Referenced resource does not exist"

  return JSONResponse(
    status_code=status.HTTP_409_CONFLICT,
    content={
      "error": {
        "message": detail,
        "status_code": status.HTTP_409_CONFLICT,
      }
    },
  )


async def sqlalchemy_error_handler(request: Request, exc: SQLAlchemyError) -> JSONResponse:
  logger.error(f"Database error: {exc}")
  return JSONResponse(
    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
    content={
      "error": {
        "message": "Database error occurred",
        "status_code": status.HTTP_500_INTERNAL_SERVER_ERROR,
      }
    },
  )


async def jwt_error_handler(request: Request, exc: JWTError) -> JSONResponse:
  logger.warning(f"JWT error: {exc}")
  return JSONResponse(
    status_code=status.HTTP_401_UNAUTHORIZED,
    content={
      "error": {
        "message": "Invalid or expired token",
        "status_code": status.HTTP_401_UNAUTHORIZED,
      }
    },
    headers={"WWW-Authenticate": "Bearer"},
  )


async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
  logger.exception(f"Unhandled exception: {exc}")
  return JSONResponse(
    status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
    content={
      "error": {
        "message": "An unexpected error occurred",
        "status_code": status.HTTP_500_INTERNAL_SERVER_ERROR,
      }
    },
  )
