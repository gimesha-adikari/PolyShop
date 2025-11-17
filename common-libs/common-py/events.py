from __future__ import annotations

from datetime import datetime, timezone
from typing import Any, Dict, Generic, Optional, TypeVar
from pydantic import BaseModel, Field

T = TypeVar("T")


class EventTypes(str):
    ORDER_CREATED = "order.created"
    ORDER_CANCELLED = "order.cancelled"
    PAYMENT_SUCCEEDED = "payment.succeeded"
    PAYMENT_FAILED = "payment.failed"
    STOCK_RESERVED = "stock.reserved"
    STOCK_RESERVATION_FAILED = "stock.reservation_failed"


class EventEnvelope(BaseModel, Generic[T]):
    event_id: str = Field(..., alias="eventId")
    event_type: str = Field(..., alias="eventType")
    aggregate_type: str = Field(..., alias="aggregateType")
    aggregate_id: str = Field(..., alias="aggregateId")
    correlation_id: Optional[str] = Field(None, alias="correlationId")
    causation_id: Optional[str] = Field(None, alias="causationId")
    occurred_at: datetime = Field(default_factory=lambda: datetime.now(timezone.utc), alias="occurredAt")
    payload: T
    metadata: Optional[Dict[str, Any]] = None

    class Config:
        populate_by_name = True
