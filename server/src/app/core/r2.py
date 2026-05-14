import logging
from typing import Optional

import boto3
from botocore.config import Config as BotocoreConfig
from botocore.exceptions import BotoCoreError, ClientError

from app.core.config import config

logger = logging.getLogger(__name__)

_ALLOWED_CONTENT_TYPES = {"image/jpeg", "image/png"}


def _get_client():
  return boto3.client(
    "s3",
    endpoint_url=config.R2_ENDPOINT_URL,
    aws_access_key_id=config.R2_ACCESS_KEY_ID,
    aws_secret_access_key=config.R2_SECRET_ACCESS_KEY,
    region_name="auto",
    config=BotocoreConfig(signature_version="s3v4"),
  )


def map_image_key(map_id: int) -> str:
  return f"maps/{map_id}/image.jpg"


def public_url(object_key: str) -> str:
  base = config.R2_PUBLIC_BASE_URL.rstrip("/")
  return f"{base}/{object_key}"


def generate_presigned_upload_url(map_id: int, content_type: str = "image/jpeg") -> dict:
  if content_type not in _ALLOWED_CONTENT_TYPES:
    raise ValueError(f"Unsupported content type: {content_type}. Allowed: {_ALLOWED_CONTENT_TYPES}")

  key = map_image_key(map_id)
  client = _get_client()

  try:
    upload_url = client.generate_presigned_url(
      ClientMethod="put_object",
      Params={
        "Bucket": config.R2_BUCKET_NAME,
        "Key": key
      },
      ExpiresIn=config.R2_PRESIGNED_UPLOAD_EXPIRE_SECONDS,
    )
  except (BotoCoreError, ClientError) as exc:
    logger.error("Failed to generate presigned upload URL for map %d: %s", map_id, exc)
    raise

  return {
    "upload_url": upload_url,
    "object_key": key,
    "expires_in": config.R2_PRESIGNED_UPLOAD_EXPIRE_SECONDS,
  }


def delete_object(object_key: str) -> None:
  client = _get_client()
  try:
    client.delete_object(Bucket=config.R2_BUCKET_NAME, Key=object_key)
    logger.info("Deleted R2 object: %s", object_key)
  except ClientError as exc:
    error_code = exc.response.get("Error", {}).get("Code", "")
    if error_code == "NoSuchKey":
      logger.debug("R2 object already absent: %s", object_key)
    else:
      logger.error("Failed to delete R2 object %s: %s", object_key, exc)
      raise
  except BotoCoreError as exc:
    logger.error("BotoCore error deleting R2 object %s: %s", object_key, exc)
    raise


def is_r2_configured() -> bool:
  return bool(config.R2_ENDPOINT_URL and config.R2_ACCESS_KEY_ID and config.R2_SECRET_ACCESS_KEY)


def get_image_url(object_key: Optional[str]) -> Optional[str]:
  if not object_key:
    return None
  return public_url(object_key)
