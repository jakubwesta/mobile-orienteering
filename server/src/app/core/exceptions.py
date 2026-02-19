from typing import Any, Optional
from fastapi import HTTPException, status


class AppException(HTTPException):
  def __init__(
    self,
    status_code: int,
    detail: Any = None,
    headers: Optional[dict[str, str]] = None,
  ):
    super().__init__(status_code=status_code, detail=detail, headers=headers)


class BadRequestException(AppException):
  def __init__(self, detail: str = "Bad request"):
    super().__init__(status_code=status.HTTP_400_BAD_REQUEST, detail=detail)


class UnauthorizedException(AppException):
  def __init__(self, detail: str = "Unauthorized"):
    super().__init__(
      status_code=status.HTTP_401_UNAUTHORIZED,
      detail=detail,
      headers={"WWW-Authenticate": "Bearer"},
    )


class ForbiddenException(AppException):
  def __init__(self, detail: str = "Forbidden"):
    super().__init__(status_code=status.HTTP_403_FORBIDDEN, detail=detail)


class NotFoundException(AppException):
  def __init__(self, detail: str = "Resource not found"):
    super().__init__(status_code=status.HTTP_404_NOT_FOUND, detail=detail)


class ConflictException(AppException):
  def __init__(self, detail: str = "Resource conflict"):
    super().__init__(status_code=status.HTTP_409_CONFLICT, detail=detail)


class UnprocessableEntityException(AppException):
  def __init__(self, detail: str = "Unprocessable entity"):
    super().__init__(status_code=status.HTTP_422_UNPROCESSABLE_ENTITY, detail=detail)


class InternalServerException(AppException):
  def __init__(self, detail: str = "Internal server error"):
    super().__init__(status_code=status.HTTP_500_INTERNAL_SERVER_ERROR, detail=detail)


class ServiceUnavailableException(AppException):
  def __init__(self, detail: str = "Service unavailable"):
    super().__init__(status_code=status.HTTP_503_SERVICE_UNAVAILABLE, detail=detail)
