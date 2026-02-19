from pydantic import BaseModel, EmailStr


class RegisterRequest(BaseModel):
  username: str
  email: EmailStr
  password: str
  full_name: str | None = None
  phone_number: str | None = None


class LoginRequest(BaseModel):
  username: str
  password: str


class GoogleLoginRequest(BaseModel):
  id_token: str


class TokenResponse(BaseModel):
  access_token: str
  refresh_token: str
  token_type: str = "bearer"


class RefreshRequest(BaseModel):
  refresh_token: str
