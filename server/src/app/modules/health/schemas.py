from pydantic import BaseModel


class HealthResponse(BaseModel):
  status: str
  version: str


class DatabaseHealthResponse(BaseModel):
  status: str
