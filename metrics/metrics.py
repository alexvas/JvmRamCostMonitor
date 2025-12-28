from abc import abstractmethod, ABC
import functools
import sys
from datetime import datetime
from typing import Any, Dict, Literal, Type, cast, List
from suppliers import (
    AbstractDataSupplier,
    SuppliersFactory,
    MemInfoData,
    MemInfoSupplier,
    SmapsData,
    SmapsSupplier,
    WsData,
    WsSupplier,
    PwsData,
    PwsSupplier,
    PbData,
    PbSupplier,
    JmxData,
    JmxSupplier,
)


class AbstractMetric(ABC):
    """Абстрактный класс для метрик"""

    def __init__(
        self, name: str, pid: int, color: str, supplier: AbstractDataSupplier
    ) -> None:
        self.__name = name
        self.__enabled = True
        self._pid = pid
        self.__color = color
        self.__supplier = supplier

    def is_enabled(self) -> bool:
        """Получить состояние метрики"""
        return self.__enabled

    def enable(self) -> None:
        """Включить метрику"""
        self.__enabled = True

    def disable(self) -> None:
        """Выключить метрику"""
        self.__enabled = False

    def get_name(self) -> str:
        """Получить имя метрики"""
        return self.__name

    @abstractmethod
    def get_value(self) -> int:
        """Получить значение метрики"""
        raise NotImplementedError("Метод get_value должен быть реализован в подклассе")

    def get_color(self) -> str:
        """Получить цвет метрики"""
        return self.__color

    @classmethod
    @abstractmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        """Получить тип метрики"""
        raise NotImplementedError("Метод kind должен быть реализован в подклассе")

    def next_poll_instant(self) -> datetime:
        """Получить время следующего опроса"""
        return self.__supplier.next_poll_instant()


class RssMetric(AbstractMetric):
    """Метрика RSS"""

    def __init__(self, pid: int, color: str, supplier: MemInfoSupplier) -> None:
        super().__init__("rss", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""

        data: MemInfoData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.rss

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["linux"]


class PssMetric(AbstractMetric):
    """Метрика PSS"""

    def __init__(self, pid: int, color: str, supplier: SmapsSupplier) -> None:
        super().__init__("pss", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""
        data: SmapsData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.pss

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["linux"]


class UssMetric(AbstractMetric):
    """Метрика USS"""

    def __init__(self, pid: int, color: str, supplier: SmapsSupplier) -> None:
        super().__init__("uss", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""
        data: SmapsData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.uss

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["linux"]


class WsMetric(AbstractMetric):
    """Метрика WS"""

    def __init__(self, pid: int, color: str, supplier: WsSupplier) -> None:
        super().__init__("ws", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""
        data: WsData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.ws

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["windows"]


class PwsMetric(AbstractMetric):
    """Метрика PWS"""

    def __init__(self, pid: int, color: str, supplier: PwsSupplier) -> None:
        super().__init__("pws", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""
        data: PwsData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.pws

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["windows"]


class PbMetric(AbstractMetric):
    """Метрика PB"""

    def __init__(self, pid: int, color: str, supplier: PbSupplier) -> None:
        super().__init__("pb", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""
        data: PbData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.pb

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["windows"]


class HeapUsedMetric(AbstractMetric):
    """Метрика Heap Used"""

    def __init__(self, pid: int, color: str, supplier: JmxSupplier) -> None:
        super().__init__("heap_used", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""
        data: JmxData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.heap_used

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["windows", "linux"]


class HeapCommittedMetric(AbstractMetric):
    """Метрика Heap Committed"""

    def __init__(self, pid: int, color: str, supplier: JmxSupplier) -> None:
        super().__init__("heap_committed", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики Heap Committed"""
        data: JmxData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.heap_committed

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["windows", "linux"]


class NmtMetric(AbstractMetric):
    """Метрика NMT"""

    def __init__(self, pid: int, color: str, supplier: JmxSupplier) -> None:
        super().__init__("nmt", pid, color, supplier)
        self.__supplier = supplier

    def get_value(self) -> int:
        """Получить значение метрики"""
        data: JmxData = self.__supplier.get_data()
        if data.kind == "none":
            return -1
        if data.kind == "same":
            return -2
        return data.nmt

    @classmethod
    def get_kind(cls) -> List[Literal["windows", "linux"]]:
        return ["windows", "linux"]


def ensure_initialized(func):
    """Декоратор для автоматической инициализации класса перед вызовом метода"""

    @functools.wraps(func)
    def wrapper(cls_or_self, *args, **kwargs):
        cls = cls_or_self if isinstance(cls_or_self, type) else type(cls_or_self)
        if not hasattr(cls, "_MetricsFactory__metrics"):
            init_method = getattr(cls, "__init__", None)
            if init_method:
                init_method()
        return func(cls_or_self, *args, **kwargs)

    return wrapper


class MetricsFactory:
    """Фабрика метрик"""

    @classmethod
    def __init__(cls) -> None:
        cls.__metric_classes: List[Type[AbstractMetric]] = [
            RssMetric,
            PssMetric,
            UssMetric,
            WsMetric,
            PwsMetric,
            PbMetric,
            HeapUsedMetric,
            HeapCommittedMetric,
            NmtMetric,
        ]
        cls.metric_mapping = {
            RssMetric: MemInfoSupplier,
            PssMetric: SmapsSupplier,
            UssMetric: SmapsSupplier,
            WsMetric: WsSupplier,
            PwsMetric: PwsSupplier,
            PbMetric: PbSupplier,
            HeapUsedMetric: JmxSupplier,
        }
        cls.metric_color_mapping = {
            RssMetric: "red",
            PssMetric: "green",
            UssMetric: "blue",
            WsMetric: "yellow",
            PwsMetric: "purple",
            PbMetric: "orange",
            HeapUsedMetric: "violet",
            HeapCommittedMetric: "cyan",
            NmtMetric: "gray",
        }
        cls.__metrics: Dict[int, List[AbstractMetric]] = {}

    @classmethod
    @ensure_initialized
    def create_metrics(
        cls, pid: int, os: Literal["windows", "linux"]
    ) -> List[AbstractMetric]:
        if pid in cls.__metrics:
            return cls.__metrics[pid]

        metrics: List[AbstractMetric] = []

        for metric_class in cls.__metric_classes:
            if os not in metric_class.get_kind():
                continue

            supplier_class = cls.metric_mapping[metric_class]
            supplier = SuppliersFactory.create_supplier(pid, supplier_class)
            color = cls.metric_color_mapping[metric_class]

            m = cast(Any, metric_class)(pid, color, supplier)
            metrics.append(m)

        cls.__metrics[pid] = metrics
        return metrics
