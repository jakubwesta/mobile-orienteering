from pydantic import BaseModel, EmailStr, ConfigDict
from datetime import datetime
from typing import Optional


class UserBase(BaseModel):
  username: str
  email: EmailStr
  full_name: Optional[str] = None
  phone_number: Optional[str] = None


class UserCreate(UserBase):
  password: str


class UserUpdate(BaseModel):
  full_name: Optional[str] = None
  phone_number: Optional[str] = None
  email: Optional[EmailStr] = None


class UserResponse(UserBase):
  model_config = ConfigDict(from_attributes=True)

  id: int
  created_at: datetime
