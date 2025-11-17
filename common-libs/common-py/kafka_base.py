from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any, Callable, Awaitable


class BaseKafkaProducer(ABC):
    @abstractmethod
    async def send(self, topic: str, key: str | None, value: dict[str, Any]) -> None:
        ...


class BaseKafkaConsumer(ABC):
    @abstractmethod
    async def start(self) -> None:
        ...

    @abstractmethod
    async def stop(self) -> None:
        ...

    @abstractmethod
    async def subscribe(self, topic: str, handler: Callable[[dict[str, Any]], Awaitable[None]]) -> None:
        ...
