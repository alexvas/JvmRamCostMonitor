"""
Кроссплатформенный сбор метрик потребления памяти процессами
"""
import platform
import psutil
from typing import Dict, List, Tuple

IS_LINUX = platform.system() == 'Linux'
IS_WINDOWS = platform.system() == 'Windows'


class MemoryMonitor:
    """Класс для мониторинга потребления памяти процессами"""
    
    def __init__(self) -> None:
        self._cache: Dict[int, Dict] = {}
    
    def get_process_memory_linux(self, pid: int, include_children: bool = False) -> Dict[str, float]:
        """
        Получить метрики памяти для процесса на Linux
        
        Returns:
            Dict с ключами: rss, pss, uss (в байтах)
        """
        if not IS_LINUX:
            return {}
        
        result = {'rss': 0.0, 'pss': 0.0, 'uss': 0.0}
        
        try:
            pids = [pid]
            if include_children:
                pids.extend(self._get_child_pids(pid))
            
            for proc_pid in pids:
                try:
                    proc = psutil.Process(proc_pid)
                    # RSS через psutil
                    mem_info = proc.memory_info()
                    result['rss'] += mem_info.rss
                    
                    # PSS и USS через /proc/[pid]/smaps
                    pss, uss = self._read_smaps(proc_pid)
                    result['pss'] += pss
                    result['uss'] += uss
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    continue
        except Exception:
            pass
        
        return result
    
    def get_process_memory_windows(self, pid: int, include_children: bool = False) -> Dict[str, float]:
        """
        Получить метрики памяти для процесса на Windows
        
        Returns:
            Dict с ключами: ws (Working Set), pws (Private Working Set), pb (Private Bytes)
        """
        if not IS_WINDOWS:
            return {}
        
        result = {'ws': 0.0, 'pws': 0.0, 'pb': 0.0}
        
        try:
            pids = [pid]
            if include_children:
                pids.extend(self._get_child_pids(pid))
            
            for proc_pid in pids:
                try:
                    proc = psutil.Process(proc_pid)
                    mem_info = proc.memory_info()
                    
                    # Working Set
                    result['ws'] += mem_info.wset if hasattr(mem_info, 'wset') else mem_info.rss
                    
                    # Private Working Set и Private Bytes через WMI или psutil
                    # psutil на Windows предоставляет только базовые метрики
                    # Для точных PWS и PB может потребоваться WMI
                    result['pws'] += mem_info.private if hasattr(mem_info, 'private') else 0
                    result['pb'] += mem_info.private if hasattr(mem_info, 'private') else 0
                except (psutil.NoSuchProcess, psutil.AccessDenied):
                    continue
        except Exception:
            pass
        
        return result
    
    def _read_smaps(self, pid: int) -> Tuple[float, float]:
        """
        Читает PSS и USS из /proc/[pid]/smaps
        
        Returns:
            Tuple (PSS, USS) в байтах
        """
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
        
        return pss, uss
    
    def _get_child_pids(self, pid: int) -> List[int]:
        """Получить список PID всех потомков процесса"""
        children = []
        try:
            proc = psutil.Process(pid)
            for child in proc.children(recursive=True):
                children.append(child.pid)
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass
        return children

