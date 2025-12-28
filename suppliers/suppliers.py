from abc import abstractmethod, ABC
from typing import Any, Dict, Type, TypeVar, cast, Literal, Generic, Optional
from dataclasses import dataclass
import functools
import psutil
from datetime import datetime, timedelta
from .jmx import JmxBeanFactory


class AbstractRawData:
    """Абстрактный класс для raw данных"""

    kind: Literal["none", "same", "different"] = "different"


T = TypeVar("T", bound=AbstractRawData)


class AbstractDataSupplier(ABC, Generic[T]):
    """Абстрактный поставщик данных"""

    def __init__(self, poll_interval: timedelta) -> None:
        """Инициализация поставщика данных"""
        self.__last_poll_instant: Optional[datetime] = None
        self.__poll_interval = poll_interval
        self.__initialized = False

    def _set_initialized(self) -> None:
        """Установить флаг успешной инициализации"""
        self.__initialized = True

    def get_data(self) -> T:
        """Получить данные"""
        if not self.__initialized:
            return self._get_none_data()

        lp = self.__last_poll_instant
        if lp is not None and datetime.now() - lp < self.__poll_interval:
            return self._get_same_data()

        self.__last_poll_instant = datetime.now()

        try:
            return self._do_get_data()
        except Exception as _e:
            return self._get_none_data()

    def next_poll_instant(self) -> datetime:
        """Вернуть время следующего опроса"""
        if self.__last_poll_instant is None:
            return datetime.now()
        return self.__last_poll_instant + self.__poll_interval

    @abstractmethod
    def _do_get_data(self) -> T:
        """Получить данные"""
        raise NotImplementedError("Метод get_data должен быть реализован в подклассе")

    @classmethod
    @abstractmethod
    def _get_none_data(cls) -> T:
        """Получить данные"""
        raise NotImplementedError(
            "Метод get_none_data должен быть реализован в подклассе"
        )

    @classmethod
    @abstractmethod
    def _get_same_data(cls) -> T:
        """Получить данные"""
        raise NotImplementedError(
            "Метод get_none_data должен быть реализован в подклассе"
        )


@dataclass
class MemInfoData(AbstractRawData):
    """Информация о потреблении памяти из структуры mem_info psutil"""

    rss: int
    kind: Literal["none", "same", "different"] = "different"


same_mem_info_data = MemInfoData(rss=0, kind="same")
none_mem_info_data = MemInfoData(rss=0, kind="none")


class MemInfoSupplier(AbstractDataSupplier[MemInfoData]):
    """Поставщик информации о потреблении памяти"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=1))
        self.process = psutil.Process(pid)
        self._set_initialized()

    def _do_get_data(self) -> MemInfoData:
        """Получить информацию о потреблении памяти"""
        mem_info = self.process.memory_info()
        return MemInfoData(rss=mem_info.rss)

    @classmethod
    def _get_none_data(cls) -> MemInfoData:
        """Получить данные none"""
        return none_mem_info_data

    @classmethod
    def _get_same_data(cls) -> MemInfoData:
        """Получить данные same"""
        return same_mem_info_data


@dataclass
class SmapsData(AbstractRawData):
    """Информация о потреблении памяти из smaps"""

    pss: int
    uss: int
    kind: Literal["none", "same", "different"] = "different"


same_smaps_data = SmapsData(pss=0, uss=0, kind="same")
none_smaps_data = SmapsData(pss=0, uss=0, kind="none")


class SmapsSupplier(AbstractDataSupplier[SmapsData]):
    """Поставщик информации о потреблении памяти из smaps"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=30))
        self.file_name = f"/proc/{pid}/smaps"
        self._set_initialized()

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
            return none_smaps_data

        return SmapsData(pss=pss, uss=uss)

    @classmethod
    def _get_none_data(cls) -> SmapsData:
        """Получить данные none"""
        return none_smaps_data

    @classmethod
    def _get_same_data(cls) -> SmapsData:
        """Получить данные same"""
        return same_smaps_data


@dataclass
class WsData(AbstractRawData):
    """Информация о потреблении памяти из ws"""

    ws: int
    kind: Literal["none", "same", "different"] = "different"


same_ws_data = WsData(ws=0, kind="same")
none_ws_data = WsData(ws=0, kind="none")


class WsSupplier(AbstractDataSupplier[WsData]):
    """Поставщик информации о потреблении памяти из ws"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=2))
        self.process = psutil.Process(pid)
        self._set_initialized()

    def _do_get_data(self) -> WsData:
        """Получить информацию о потреблении памяти из ws"""
        mem_info = self.process.memory_info()
        ws = mem_info.wset if hasattr(mem_info, "wset") else mem_info.rss
        return WsData(ws=ws)

    @classmethod
    def _get_none_data(cls) -> WsData:
        """Получить данные none"""
        return none_ws_data

    @classmethod
    def _get_same_data(cls) -> WsData:
        """Получить данные same"""
        return same_ws_data


@dataclass
class PwsData(AbstractRawData):
    """Информация о потреблении памяти из pws. TODO: реализовать на Windows"""

    pws: int
    kind: Literal["none", "same", "different"] = "different"


same_pws_data = PwsData(pws=0, kind="same")
none_pws_data = PwsData(pws=0, kind="none")


class PwsSupplier(AbstractDataSupplier[PwsData]):
    """Поставщик информации о потреблении памяти из pws"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=5))
        self.process = psutil.Process(pid)
        self._set_initialized()

    def _do_get_data(self) -> PwsData:
        """Получить информацию о потреблении памяти из pws"""
        mem_info = self.process.memory_info()
        if not hasattr(mem_info, "private"):
            return none_pws_data
        pws = mem_info.private
        return PwsData(pws=pws)

    @classmethod
    def _get_none_data(cls) -> PwsData:
        """Получить данные none"""
        return none_pws_data

    @classmethod
    def _get_same_data(cls) -> PwsData:
        """Получить данные same"""
        return same_pws_data


@dataclass
class PbData(AbstractRawData):
    """Информация о потреблении памяти из pb. TODO: реализовать на Windows"""

    pb: int
    kind: Literal["none", "same", "different"] = "different"


same_pb_data = PbData(pb=0, kind="same")
none_pb_data = PbData(pb=0, kind="none")


class PbSupplier(AbstractDataSupplier[PbData]):
    """Поставщик информации о потреблении памяти из pb"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=10))
        self.process = psutil.Process(pid)
        self._set_initialized()

    def _do_get_data(self) -> PbData:
        """Получить информацию о потреблении памяти из pb"""
        mem_info = self.process.memory_info()
        if not hasattr(mem_info, "private"):
            return none_pb_data
        pb = mem_info.private
        return PbData(pb=pb)

    @classmethod
    def _get_none_data(cls) -> PbData:
        """Получить данные none"""
        return none_pb_data

    @classmethod
    def _get_same_data(cls) -> PbData:
        """Получить данные same"""
        return same_pb_data


@dataclass
class JmxData(AbstractRawData):
    """Информация о потреблении памяти из jmx"""

    heap_used: int
    heap_committed: int
    nmt: int
    kind: Literal["none", "same", "different"] = "different"


same_jmx_data = JmxData(heap_used=0, heap_committed=0, nmt=0, kind="same")
none_jmx_data = JmxData(heap_used=0, heap_committed=0, nmt=0, kind="none")


class JmxSupplier(AbstractDataSupplier[JmxData]):
    """Поставщик информации о потреблении памяти из jmx"""

    def __init__(self, pid: int) -> None:
        super().__init__(timedelta(seconds=5))
        # Получаем MemoryMXBean для процесса с указанным PID
        self.memory_mxbean = JmxBeanFactory.get_memory_mxbean(pid)
        if self.memory_mxbean is not None:
            self._set_initialized()

    def _do_get_data(self) -> JmxData:
        """Получить информацию о потреблении памяти из jmx"""
        if self.memory_mxbean is None:
            return none_jmx_data

        heap_used = 0
        heap_committed = 0
        nmt = 0

        # Получаем информацию о heap памяти
        heap_memory_usage = self.memory_mxbean.getHeapMemoryUsage()
        if heap_memory_usage is not None:
            heap_used = int(heap_memory_usage.getUsed())
            heap_committed = int(heap_memory_usage.getCommitted())
        # Получаем информацию о non-heap памяти (NMT)
        non_heap_memory_usage = self.memory_mxbean.getNonHeapMemoryUsage()
        if non_heap_memory_usage is not None:
            nmt = int(non_heap_memory_usage.getUsed())

        return JmxData(heap_used=heap_used, heap_committed=heap_committed, nmt=nmt)

    @classmethod
    def _get_none_data(cls) -> JmxData:
        """Получить данные none"""
        return none_jmx_data

    @classmethod
    def _get_same_data(cls) -> JmxData:
        """Получить данные same"""
        return same_jmx_data


def ensure_initialized(func):
    """Декоратор для автоматической инициализации класса перед вызовом метода"""

    @functools.wraps(func)
    def wrapper(cls_or_self, *args, **kwargs):
        cls = cls_or_self if isinstance(cls_or_self, type) else type(cls_or_self)
        if not hasattr(cls, "_SuppliersFactory__cached_suppliers"):
            init_method = getattr(cls, "__init__", None)
            if init_method:
                init_method()
        return func(cls_or_self, *args, **kwargs)

    return wrapper


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
    @ensure_initialized
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
