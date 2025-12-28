from abc import abstractmethod
from typing import Any, Dict, Type, TypeVar, cast
from dataclasses import dataclass
import psutil
from datetime import datetime, timedelta
from .jmx import JmxBeanFactory

T = TypeVar("T")


class AbstractDataSupplier[T]:
    """Абстрактный поставщик данных"""

    def __init__(self, poll_interval: timedelta) -> None:
        """Инициализация поставщика данных"""
        self.__last_poll_instant: datetime | None = None
        self.__poll_interval = poll_interval
        self.last_data: T | None = None

    def get_data(self) -> T | None:
        """Получить данные"""
        if (
            self.__last_poll_instant is None
            or datetime.now() - self.__last_poll_instant >= self.__poll_interval
        ):
            self.last_data = self._do_get_data()
            self.__last_poll_instant = datetime.now()
        return self.last_data

    def next_poll_instant(self) -> datetime:
        """Вернуть время следующего опроса"""
        if self.__last_poll_instant is None:
            return datetime.now()
        return self.__last_poll_instant + self.__poll_interval

    @abstractmethod
    def _do_get_data(self) -> T:
        """Получить данные"""
        raise NotImplementedError("Метод get_data должен быть реализован в подклассе")


@dataclass
class MemInfoData:
    """Информация о потреблении памяти из структуры mem_info psutil"""

    rss: int


class MemInfoSupplier(AbstractDataSupplier[MemInfoData]):
    """Поставщик информации о потреблении памяти"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=1))
        self.process = psutil.Process(pid)

    def _do_get_data(self) -> MemInfoData:
        """Получить информацию о потреблении памяти"""
        try:
            # RSS через psutil
            mem_info = self.process.memory_info()
            return MemInfoData(rss=mem_info.rss)

        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return MemInfoData(rss=0)


@dataclass
class SmapsData:
    """Информация о потреблении памяти из smaps"""

    pss: int
    uss: int


class SmapsSupplier(AbstractDataSupplier[SmapsData]):
    """Поставщик информации о потреблении памяти из smaps"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=30))
        self.file_name = f"/proc/{pid}/smaps"

    def _do_get_data(self) -> SmapsData:
        """Получить информацию о потреблении памяти из smaps"""
        pss: int = 0
        uss = 0

        try:
            with open(self.file_name, "r") as f:
                current_pss = 0
                private_clean = 0
                private_dirty = 0

                for line in f:
                    if line.startswith("Pss:"):
                        # PSS уже рассчитан в smaps
                        current_pss = int(line.split()[1]) * 1024  # KB to bytes
                        pss += current_pss
                    elif line.startswith("Private_Clean:"):
                        private_clean = int(line.split()[1]) * 1024
                    elif line.startswith("Private_Dirty:"):
                        private_dirty = int(line.split()[1]) * 1024
                    elif line.startswith("Size:"):
                        # Конец предыдущего блока, накапливаем USS
                        uss += private_clean + private_dirty
                        # Сброс для нового блока
                        private_clean = 0
                        private_dirty = 0

                # Последний блок
                uss += private_clean + private_dirty
        except (FileNotFoundError, PermissionError, IOError):
            pass

        return SmapsData(pss=pss, uss=uss)


@dataclass
class WsData:
    """Информация о потреблении памяти из ws"""

    ws: int


class WsSupplier(AbstractDataSupplier[WsData]):
    """Поставщик информации о потреблении памяти из ws"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=2))
        self.process = psutil.Process(pid)

    def _do_get_data(self) -> WsData:
        """Получить информацию о потреблении памяти из ws"""
        ws: int = 0
        try:
            mem_info = self.process.memory_info()

            # Working Set
            ws += mem_info.wset if hasattr(mem_info, "wset") else mem_info.rss
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return WsData(ws=ws)


@dataclass
class PwsData:
    """Информация о потреблении памяти из pws. TODO: реализовать на Windows"""

    pws: int


class PwsSupplier(AbstractDataSupplier[PwsData]):
    """Поставщик информации о потреблении памяти из pws"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=5))
        self.process = psutil.Process(pid)

    def _do_get_data(self) -> PwsData:
        """Получить информацию о потреблении памяти из pws"""
        pws: int = 0
        try:
            mem_info = self.process.memory_info()
            pws += mem_info.private if hasattr(mem_info, "private") else 0
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return PwsData(pws=pws)


@dataclass
class PbData:
    """Информация о потреблении памяти из pb. TODO: реализовать на Windows"""

    pb: int


class PbSupplier(AbstractDataSupplier[PbData]):
    """Поставщик информации о потреблении памяти из pb"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=10))
        self.process = psutil.Process(pid)

    def _do_get_data(self) -> PbData:
        """Получить информацию о потреблении памяти из pb"""
        pb: int = 0
        try:
            mem_info = self.process.memory_info()
            pb += mem_info.private if hasattr(mem_info, "private") else 0
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return PbData(pb=pb)


@dataclass
class JmxData:
    """Информация о потреблении памяти из jmx"""

    heap_used: int
    heap_committed: int
    nmt: int


class JmxSupplier(AbstractDataSupplier[JmxData]):
    """Поставщик информации о потреблении памяти из jmx"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=5))
        self.memory_mxbean = JmxBeanFactory.get_memory_mxbean(pid)

    def _do_get_data(self) -> JmxData:
        """Получить информацию о потреблении памяти из jmx"""
        heap_used = 0
        heap_committed = 0
        nmt = 0

        try:
            # Получаем MemoryMXBean для процесса с указанным PID

            if self.memory_mxbean is None:
                return JmxData(
                    heap_used=heap_used, heap_committed=heap_committed, nmt=nmt
                )

            # Получаем информацию о heap памяти
            heap_memory_usage = self.memory_mxbean.getHeapMemoryUsage()
            if heap_memory_usage is not None:
                heap_used = int(heap_memory_usage.getUsed())
                heap_committed = int(heap_memory_usage.getCommitted())

            # Получаем информацию о non-heap памяти (NMT)
            non_heap_memory_usage = self.memory_mxbean.getNonHeapMemoryUsage()
            if non_heap_memory_usage is not None:
                nmt = int(non_heap_memory_usage.getUsed())

        except Exception:
            # В случае ошибки возвращаем нулевые значения
            pass

        return JmxData(heap_used=heap_used, heap_committed=heap_committed, nmt=nmt)


class SuppliersFactory:
    """Фабрика поставщиков данных"""

    @classmethod
    def __init__(cls) -> None:
        cls.__suppliers_classes = [
            MemInfoSupplier,
            SmapsSupplier,
            WsSupplier,
            PwsSupplier,
            PbSupplier,
            JmxSupplier,
        ]
        cls.__cached_suppliers: Dict[
            int, Dict[Type[AbstractDataSupplier[Any]], AbstractDataSupplier[Any]]
        ] = {}

    @classmethod
    def create_supplier(
        cls, pid: int, supplier_class: Type[AbstractDataSupplier[T]]
    ) -> AbstractDataSupplier[T]:
        if supplier_class not in cls.__suppliers_classes:
            raise ValueError(f"Invalid supplier type: {supplier_class}")
        if pid not in cls.__cached_suppliers:
            suppliers: Dict[
                Type[AbstractDataSupplier[Any]], AbstractDataSupplier[Any]
            ] = {}
            cls.__cached_suppliers[pid] = suppliers
        else:
            suppliers = cls.__cached_suppliers[pid]

        if supplier_class in suppliers:
            return suppliers[supplier_class]

        # Все подклассы AbstractDataSupplier переопределяют __init__ и принимают pid: int
        supplier = cast(Any, supplier_class)(pid)
        suppliers[supplier_class] = supplier
        return supplier
