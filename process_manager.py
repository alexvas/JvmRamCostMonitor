"""
Управление процессами и интеграция с jps
"""
import subprocess
import psutil
from typing import List, Dict, Optional, Tuple
import re


class ProcessManager:
    """Класс для работы с процессами и jps"""
    
    def __init__(self) -> None:
        self._jps_available = self._check_jps_available()
    
    def _check_jps_available(self) -> bool:
        """Проверить наличие jps в системе"""
        try:
            subprocess.run(['jps', '-help'], 
                         capture_output=True, 
                         timeout=2,
                         check=False)
            return True
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False
    
    def get_java_processes(self) -> List[Dict[str, object]]:
        """
        Получить список Java процессов через jps
        
        Returns:
            Список словарей с ключами: pid, name, main_class, full_name
        """
        if not self._jps_available:
            return []
        
        processes = []
        try:
            # jps -l выводит PID и полное имя главного класса
            result = subprocess.run(['jps', '-l'], 
                                  capture_output=True, 
                                  text=True, 
                                  timeout=5)
            
            if result.returncode != 0:
                return []
            
            for line in result.stdout.strip().split('\n'):
                if not line.strip():
                    continue
                
                # Формат: PID main_class
                parts = line.strip().split(None, 1)
                if len(parts) < 2:
                    continue
                
                try:
                    pid = int(parts[0])
                    main_class = parts[1]
                    
                    # Получить читаемое имя процесса
                    readable_name = self._get_readable_name(pid, main_class)
                    
                    processes.append({
                        'pid': pid,
                        'name': readable_name,
                        'main_class': main_class,
                        'full_name': main_class
                    })
                except ValueError:
                    continue
        except Exception:
            pass
        
        return processes
    
    def _get_readable_name(self, pid: int, main_class: str) -> str:
        """
        Получить читаемое имя процесса, как в VisualVM
        
        Пытается извлечь короткое имя из полного класса или использовать имя процесса
        """
        try:
            # Попытка получить имя через psutil
            proc = psutil.Process(pid)
            name = proc.name()
            
            # Если это java/javaw, используем имя главного класса
            if name.lower() in ('java', 'javaw', 'javaws'):
                # Извлекаем короткое имя из полного класса
                # Например: com.example.Main -> Main
                if '.' in main_class:
                    short_name = main_class.split('.')[-1]
                    return f"{short_name} ({pid})"
                return f"{main_class} ({pid})"
            
            return f"{name} ({pid})"
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            # Fallback на main_class
            if '.' in main_class:
                short_name = main_class.split('.')[-1]
                return f"{short_name} ({pid})"
            return f"{main_class} ({pid})"
    
    def get_process_children(self, pid: int) -> List[int]:
        """
        Получить список PID всех потомков процесса
        
        Returns:
            Список PID потомков (включая сам процесс)
        """
        children = [pid]
        try:
            proc = psutil.Process(pid)
            for child in proc.children(recursive=True):
                children.append(child.pid)
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            pass
        return children
    
    def is_process_alive(self, pid: int) -> bool:
        """Проверить, существует ли процесс"""
        try:
            psutil.Process(pid)
            return True
        except psutil.NoSuchProcess:
            return False
    
    def get_process_info(self, pid: int) -> Optional[Dict[str, object]]:
        """Получить информацию о процессе"""
        try:
            proc = psutil.Process(pid)
            return {
                'pid': pid,
                'name': proc.name(),
                'status': proc.status(),
                'create_time': proc.create_time(),
            }
        except (psutil.NoSuchProcess, psutil.AccessDenied):
            return None

