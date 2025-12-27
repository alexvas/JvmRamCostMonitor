"""
Главное окно приложения на PyQt5
"""
import sys
import os
import signal
# Добавляем родительскую директорию в путь для импорта модулей из корня проекта
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from PyQt5.QtWidgets import QMainWindow, QWidget, QHBoxLayout, QVBoxLayout
from PyQt5.QtCore import QTimer, Qt
from PyQt5.QtGui import QCloseEvent
import threading
import time
from typing import Optional, Dict, List, Tuple
from collections import deque

from .process_panel import ProcessPanel
from .graph_panel import GraphPanel
from .controls_panel import ControlsPanel
from process_manager import ProcessManager
from memory_monitor import MemoryMonitor
from jmx_client import JMXClient
from config import POLL_INTERVALS, IS_LINUX, IS_WINDOWS

class MainWindow(QMainWindow):
    """Главное окно приложения"""
    
    def __init__(self):
        super().__init__()
        self.setWindowTitle("Jvm RAM Cost Monitor")
        self.setGeometry(100, 100, 1200, 800)
        
        # Менеджеры
        self.process_manager = ProcessManager()
        self.memory_monitor = MemoryMonitor()
        self.jmx_client = JMXClient()
        
        # Данные
        self.current_pid: Optional[int] = None
        self.include_children = False
        self.process_group_mode = 'cumulative'  # 'cumulative' or 'separate'
        self.process_group_pids: List[int] = []  # Список PID в группе
        
        # Хранилище данных для графиков
        # Для кумулятивного режима: metric -> deque
        # Для раздельного режима: f"{metric}_pid{pid}" -> deque
        self.data_history: Dict[str, deque] = {}
        self.time_history = deque(maxlen=1000)  # Храним последние 1000 точек
        
        # Таймеры для обновления данных
        self.running = False
        self.update_timer = QTimer()
        self.update_timer.timeout.connect(self._update_data)
        
        # Переменные для отслеживания времени последнего обновления
        self.last_rss_update = 0
        self.last_pss_update = 0
        self.last_ws_update = 0
        self.last_pws_update = 0
        self.last_jmx_update = 0
        
        # Установка обработчика сигнала для корректного завершения
        signal.signal(signal.SIGTERM, lambda s, f: self.close())
        signal.signal(signal.SIGINT, lambda s, f: self.close())
        
        self._create_ui()
    
    def _create_ui(self) -> None:
        """Создание UI"""
        central_widget = QWidget()
        self.setCentralWidget(central_widget)
        
        main_layout = QHBoxLayout(central_widget)
        main_layout.setContentsMargins(5, 5, 5, 5)
        
        # Панель процессов (слева)
        self.process_panel = ProcessPanel(self)
        # Выравниваем панель по верхнему краю, чтобы узкая кнопка была вверху
        # Используем getattr для получения константы AlignTop
        align_top = getattr(Qt, 'AlignTop', 0x0001)
        main_layout.addWidget(self.process_panel, stretch=0, alignment=align_top)  # type: ignore
        
        # Вертикальный контейнер для графика и панели управления
        right_widget = QWidget()
        right_layout = QVBoxLayout(right_widget)
        right_layout.setContentsMargins(5, 5, 5, 5)
        
        # Панель графиков
        self.graph_panel = GraphPanel(self)
        right_layout.addWidget(self.graph_panel, stretch=1)
        
        # Панель управления
        self.controls_panel = ControlsPanel(self)
        right_layout.addWidget(self.controls_panel)
        
        main_layout.addWidget(right_widget, stretch=1)
    
    def start_monitoring(self, pid: int, include_children: bool = False) -> None:
        """Начать мониторинг процесса"""
        self.current_pid = pid
        self.include_children = include_children
        
        # Определение группы процессов
        if include_children:
            self.process_group_pids = self.process_manager.get_process_children(pid)
        else:
            self.process_group_pids = [pid]
        
        if not self.running:
            self.running = True
            # Запускаем таймер для обновления данных каждые 100мс
            self.update_timer.start(100)
        
        # Подключение к JMX если это Java процесс
        self.jmx_client.connect(pid)
    
    def stop_monitoring(self) -> None:
        """Остановить мониторинг"""
        self.running = False
        self.update_timer.stop()
        if self.current_pid:
            self.jmx_client.disconnect(self.current_pid)
        self.current_pid = None
    
    def _update_data(self) -> None:
        """Обновление данных (вызывается таймером)"""
        if not self.running or not self.current_pid:
            return
        
        current_time = time.time()
        
        # Обновление временной метки
        self.time_history.append(current_time)
        
        # RSS (Linux) / WS (Windows) - каждые 5 сек
        if current_time - self.last_rss_update >= POLL_INTERVALS['rss']:
            if self.process_group_mode == 'separate' and len(self.process_group_pids) > 1:
                # Раздельный режим - собираем данные для каждого процесса
                for proc_pid in self.process_group_pids:
                    if IS_LINUX:
                        mem_data = self.memory_monitor.get_process_memory_linux(
                            proc_pid, False)  # Без потомков, так как уже в группе
                        if 'rss' in mem_data:
                            self._add_data_point('rss', mem_data['rss'], proc_pid)
                    elif IS_WINDOWS:
                        mem_data = self.memory_monitor.get_process_memory_windows(
                            proc_pid, False)
                        if 'ws' in mem_data:
                            self._add_data_point('ws', mem_data['ws'], proc_pid)
            else:
                # Кумулятивный режим
                if IS_LINUX:
                    mem_data = self.memory_monitor.get_process_memory_linux(
                        self.current_pid, self.include_children)
                    if 'rss' in mem_data:
                        self._add_data_point('rss', mem_data['rss'])
                elif IS_WINDOWS:
                    mem_data = self.memory_monitor.get_process_memory_windows(
                        self.current_pid, self.include_children)
                    if 'ws' in mem_data:
                        self._add_data_point('ws', mem_data['ws'])
            self.last_rss_update = current_time
        
        # PSS/USS (Linux) - каждые 30 сек
        if IS_LINUX and current_time - self.last_pss_update >= POLL_INTERVALS['pss']:
            if self.process_group_mode == 'separate' and len(self.process_group_pids) > 1:
                # Раздельный режим
                for proc_pid in self.process_group_pids:
                    mem_data = self.memory_monitor.get_process_memory_linux(proc_pid, False)
                    if 'pss' in mem_data:
                        self._add_data_point('pss', mem_data['pss'], proc_pid)
                    if 'uss' in mem_data:
                        self._add_data_point('uss', mem_data['uss'], proc_pid)
            else:
                # Кумулятивный режим
                mem_data = self.memory_monitor.get_process_memory_linux(
                    self.current_pid, self.include_children)
                if 'pss' in mem_data:
                    self._add_data_point('pss', mem_data['pss'])
                if 'uss' in mem_data:
                    self._add_data_point('uss', mem_data['uss'])
            self.last_pss_update = current_time
        
        # PWS/PB (Windows) - каждые 10 сек
        if IS_WINDOWS and current_time - self.last_pws_update >= POLL_INTERVALS['pws']:
            if self.process_group_mode == 'separate' and len(self.process_group_pids) > 1:
                # Раздельный режим
                for proc_pid in self.process_group_pids:
                    mem_data = self.memory_monitor.get_process_memory_windows(proc_pid, False)
                    if 'pws' in mem_data:
                        self._add_data_point('pws', mem_data['pws'], proc_pid)
                    if 'pb' in mem_data:
                        self._add_data_point('pb', mem_data['pb'], proc_pid)
            else:
                # Кумулятивный режим
                mem_data = self.memory_monitor.get_process_memory_windows(
                    self.current_pid, self.include_children)
                if 'pws' in mem_data:
                    self._add_data_point('pws', mem_data['pws'])
                if 'pb' in mem_data:
                    self._add_data_point('pb', mem_data['pb'])
            self.last_pws_update = current_time
        
        # JMX метрики - каждые 5 сек
        if current_time - self.last_jmx_update >= POLL_INTERVALS['jmx']:
            heap_data = self.jmx_client.get_heap_metrics(self.current_pid)
            if 'heap_used' in heap_data:
                self._add_data_point('heap_used', heap_data['heap_used'])
            if 'heap_committed' in heap_data:
                self._add_data_point('heap_committed', heap_data['heap_committed'])
            
            nmt_data = self.jmx_client.get_nmt_metrics(self.current_pid)
            if 'nmt_total' in nmt_data:
                self._add_data_point('nmt', nmt_data['nmt_total'])
            
            self.last_jmx_update = current_time
        
        # Обновление графика
        if self.graph_panel:
            self.graph_panel.update_graph()
    
    def _add_data_point(self, metric: str, value: float, pid: Optional[int] = None) -> None:
        """Добавить точку данных для метрики"""
        if self.process_group_mode == 'cumulative' or len(self.process_group_pids) == 1:
            # Кумулятивный режим или один процесс
            key = metric
        else:
            # Раздельный режим
            if pid is None:
                pid = self.current_pid
            key = f"{metric}_pid{pid}"
        
        if key not in self.data_history:
            self.data_history[key] = deque(maxlen=1000)
        self.data_history[key].append(value)
    
    def get_data(self, metric: str, pid: Optional[int] = None) -> Tuple[List[float], List[float]]:
        """Получить данные для метрики (время, значения)"""
        if self.process_group_mode == 'separate' and pid is not None:
            key = f"{metric}_pid{pid}"
        else:
            key = metric
        
        if key not in self.data_history:
            return ([], [])
        
        times = list(self.time_history)
        values = list(self.data_history[key])
        
        # Синхронизация размеров
        min_len = min(len(times), len(values))
        if min_len > 0:
            return (times[:min_len], values[:min_len])
        return ([], [])
    
    def set_process_group_mode(self, mode: str) -> None:
        """Установить режим отображения группы процессов"""
        self.process_group_mode = mode
        # Очистка данных при смене режима
        self.data_history.clear()
        self.time_history.clear()
    
    def trigger_gc(self) -> None:
        """Запустить GC"""
        if self.current_pid:
            self.jmx_client.trigger_gc(self.current_pid)
    
    def create_heap_dump(self, filepath: str) -> bool:
        """Создать heap dump"""
        if self.current_pid:
            return self.jmx_client.create_heap_dump(self.current_pid, filepath)
        return False
    
    def save_screenshot(self, filepath: str) -> None:
        """Сохранить скриншот графика"""
        if self.graph_panel:
            self.graph_panel.save_screenshot(filepath)
    
    def closeEvent(self, a0: Optional[QCloseEvent]) -> None:
        """Обработчик закрытия окна"""
        self.stop_monitoring()
        if a0:
            a0.accept()
