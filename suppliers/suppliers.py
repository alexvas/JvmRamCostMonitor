from typing import Any, Optional
from dataclasses import dataclass
import psutil
from datetime import datetime, timedelta
from .jmx import JmxBeanFactory

class AbstractDataSupplier:
    """Абстрактный поставщик данных"""

    def __init__(self, poll_interval: timedelta) -> None:
        """Инициализация поставщика данных"""
        self.__last_poll_instant: datetime | None = None
        self.__poll_interval = poll_interval
        self.last_data: Any = None

    def get_data(self, pid: int) -> Any:
        """Получить данные"""
        if self.__last_poll_instant is None or datetime.now() - self.__last_poll_instant >= self.__poll_interval:
            self.last_data = self._do_get_data(pid)
            self.__last_poll_instant = datetime.now()
        return self.last_data

    def next_poll_instant(self) -> datetime:
        """Вернуть время следующего опроса"""
        if self.__last_poll_instant is None:
            return datetime.now()
        return self.__last_poll_instant + self.__poll_interval

    def _do_get_data(self, pid: int) -> Any:
        """Получить данные"""
        raise NotImplementedError("Метод get_data должен быть реализован в подклассе")


@dataclass
class MemInfoData:
    """Информация о потреблении памяти из структуры mem_info psutil"""
    rss: float

class MemInfoSupplier(AbstractDataSupplier):
    """Поставщик информации о потреблении памяти"""

    def __init__(self) -> None:
        super().__init__(timedelta(seconds=1))

    def _do_get_data(self, pid: int) -> MemInfoData:
        """Получить информацию о потреблении памяти"""
        try:
            proc = psutil.Process(pid)
            # RSS через psutil
            mem_info = proc.memory_info()
            return MemInfoData(rss=mem_info.rss)
                    
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return MemInfoData(rss=0.0)

@dataclass
class SmapsData:
    """Информация о потреблении памяти из smaps"""
    pss: float
    uss: float


class SmapsSupplier(AbstractDataSupplier):
    """Поставщик информации о потреблении памяти из smaps"""

    def __init__(self) -> None:
        super().__init__(timedelta(seconds=30))

    def _do_get_data(self, pid: int) -> SmapsData:
        """Получить информацию о потреблении памяти из smaps"""
        pss = 0.0
        uss = 0.0
        
        try:
            with open(f'/proc/{pid}/smaps', 'r') as f:
                current_pss = 0.0
                private_clean = 0.0
                private_dirty = 0.0
                
                for line in f:
                    if line.startswith('Pss:'):
                        # PSS уже рассчитан в smaps
                        current_pss = float(line.split()[1]) * 1024  # KB to bytes
                        pss += current_pss
                    elif line.startswith('Private_Clean:'):
                        private_clean = float(line.split()[1]) * 1024
                    elif line.startswith('Private_Dirty:'):
                        private_dirty = float(line.split()[1]) * 1024
                    elif line.startswith('Size:'):
                        # Конец предыдущего блока, накапливаем USS
                        uss += private_clean + private_dirty
                        # Сброс для нового блока
                        private_clean = 0.0
                        private_dirty = 0.0
                
                # Последний блок
                uss += private_clean + private_dirty
        except (FileNotFoundError, PermissionError, IOError):
            pass
        
        return SmapsData(pss=pss, uss=uss)

@dataclass
class WsData:
    """Информация о потреблении памяти из ws"""
    ws: float

class WsSupplier(AbstractDataSupplier):
    """Поставщик информации о потреблении памяти из ws"""

    def __init__(self) -> None:
        super().__init__(timedelta(seconds=2))

    def _do_get_data(self, pid: int) -> WsData:
        """Получить информацию о потреблении памяти из ws"""
        ws = 0.0
        try:
            proc = psutil.Process(pid)
            mem_info = proc.memory_info()
            
            # Working Set
            ws += mem_info.wset if hasattr(mem_info, 'wset') else mem_info.rss
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return WsData(ws=ws)


@dataclass
class PwsData:
    """Информация о потреблении памяти из pws. TODO: реализовать на Windows"""
    pws: float

class PwsSupplier(AbstractDataSupplier):
    """Поставщик информации о потреблении памяти из pws"""

    def __init__(self) -> None:
        super().__init__(timedelta(seconds=5))

    def _do_get_data(self, pid: int) -> PwsData:
        """Получить информацию о потреблении памяти из pws"""
        pws = 0.0
        try:
            proc = psutil.Process(pid)
            mem_info = proc.memory_info()
            pws += mem_info.private if hasattr(mem_info, 'private') else 0
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return PwsData(pws=pws)


@dataclass
class PbData:
    """Информация о потреблении памяти из pb. TODO: реализовать на Windows"""
    pb: float

class PbSupplier(AbstractDataSupplier):
    """Поставщик информации о потреблении памяти из pb"""

    def __init__(self) -> None:
        super().__init__(timedelta(seconds=10))

    def _do_get_data(self, pid: int) -> PbData:
        """Получить информацию о потреблении памяти из pb"""
        pb = 0.0
        try:
            proc = psutil.Process(pid)
            mem_info = proc.memory_info()
            pb += mem_info.private if hasattr(mem_info, 'private') else 0
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass

        return PbData(pb=pb)


@dataclass
class JmxData:
    """Информация о потреблении памяти из jmx"""
    heap_used: float
    heap_committed: float
    nmt: float

class JmxSupplier(AbstractDataSupplier):
    """Поставщик информации о потреблении памяти из jmx"""

    def __init__(self) -> None:
        super().__init__(timedelta(seconds=5))

    def _do_get_data(self, pid: int) -> JmxData:
        """Получить информацию о потреблении памяти из jmx"""
        heap_used = 0.0
        heap_committed = 0.0
        nmt = 0.0
        
        try:
            # Получаем MemoryMXBean для процесса с указанным PID
            memory_mxbean = JmxBeanFactory.get_memory_mxbean(pid)
            
            if memory_mxbean is None:
                return JmxData(heap_used=heap_used, heap_committed=heap_committed, nmt=nmt)
            
            # Получаем информацию о heap памяти
            heap_memory_usage = memory_mxbean.getHeapMemoryUsage()
            if heap_memory_usage is not None:
                heap_used = float(heap_memory_usage.getUsed())
                heap_committed = float(heap_memory_usage.getCommitted())
            
            # Получаем информацию о non-heap памяти (NMT)
            non_heap_memory_usage = memory_mxbean.getNonHeapMemoryUsage()
            if non_heap_memory_usage is not None:
                nmt = float(non_heap_memory_usage.getUsed())
                
        except Exception:
            # В случае ошибки возвращаем нулевые значения
            pass

        return JmxData(heap_used=heap_used, heap_committed=heap_committed, nmt=nmt)