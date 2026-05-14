import hashlib
import bcrypt
from datetime import datetime, timedelta, timezone
from typing import Optional
from jose import JWTError, jwt
from fastapi import Depends
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials

from app.core.config import config
from app.core.exceptions import UnauthorizedException

security = HTTPBearer()


def _prehash(value: str) -> bytes:
  return hashlib.sha256(value.encode()).digest()


def hash_password(password: str) -> str:
  return bcrypt.hashpw(_prehash(password), bcrypt.gensalt()).decode()


def verify_password(plain_password: str, hashed_password: str) -> bool:
  return bcrypt.checkpw(_prehash(plain_password), hashed_password.encode())


def create_access_token(user_id: int, expires_delta: Optional[timedelta] = None) -> str:
  if expires_delta:
    expire = datetime.now(timezone.utc) + expires_delta
  else:
    expire = datetime.now(timezone.utc) + timedelta(hours=config.ACCESS_TOKEN_EXPIRE_HOURS)

  to_encode = {
    "sub": str(user_id),
    "exp": expire,
    "type": "access"
  }
  encoded_jwt = jwt.encode(to_encode, config.SECRET_KEY, algorithm=config.ALGORITHM)
  return encoded_jwt


def create_refresh_token(user_id: int, expires_delta: Optional[timedelta] = None) -> str:
  if expires_delta:
    expire = datetime.now(timezone.utc) + expires_delta
  else:
    expire = datetime.now(timezone.utc) + timedelta(days=config.REFRESH_TOKEN_EXPIRE_DAYS)

  to_encode = {
    "sub": str(user_id),
    "exp": expire,
    "type": "refresh"
  }
  encoded_jwt = jwt.encode(to_encode, config.SECRET_KEY, algorithm=config.ALGORITHM)
  return encoded_jwt


def decode_token(token: str) -> dict:
  try:
    payload = jwt.decode(token, config.SECRET_KEY, algorithms=[config.ALGORITHM])
    return payload
  except JWTError:
    raise UnauthorizedException("Could not validate credentials")


def get_user_id_from_token(token: str) -> int:
  payload = decode_token(token)
  user_id: Optional[str] = payload.get("sub")
  if user_id is None:
    raise UnauthorizedException("Could not validate credentials")
  return int(user_id)


async def get_current_user_id(
  credentials: HTTPAuthorizationCredentials = Depends(security)
) -> int:
  token = credentials.credentials
  return get_user_id_from_token(token)
