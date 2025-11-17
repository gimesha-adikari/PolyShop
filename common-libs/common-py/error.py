from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, Optional

from pydantic import BaseModel, Field


class ErrorResponse(BaseModel):
    timestamp: datetime = Field(default_factory=lambda: datetime.now(timezone.utc))
    status: int
    error: str
    code: Optional[str] = None
    message: str
    path: Optional[str] = None
    request_id: Optional[str] = Field(default=None, alias="requestId")
    details: Optional[Dict[str, Any]] = None

    class Config:
        populate_by_name = True
