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
  username: Optional[str] = None
  email: Optional[EmailStr] = None
  full_name: Optional[str] = None
  phone_number: Optional[str] = None
  new_password: Optional[str] = None
  old_password: Optional[str] = None


class UserResponse(UserBase):
  model_config = ConfigDict(from_attributes=True)

  id: int
  created_at: datetime
