"""
Главное окно приложения на PyROOT
"""
import sys
import os
# Добавляем родительскую директорию в путь для импорта модулей из корня проекта
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import ROOT
from ROOT import TApplication, TGMainFrame, TGHorizontalFrame, TGVerticalFrame
from ROOT import TGLayoutHints, kLHintsExpandX, kLHintsExpandY, kLHintsLeft
from ROOT import gClient, gStyle
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


class MainWindow:
    """Главное окно приложения"""
    
    def __init__(self):
        self.app = None
        self.main_frame = None
        self.process_panel = None
        self.graph_panel = None
        self.controls_panel = None
        
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
        self.update_thread = None
        
        # Инициализация PyROOT
        self._init_root()
        self._create_ui()
    
    def _init_root(self) -> None:
        """Инициализация ROOT"""
        if not ROOT.gApplication:
            self.app = TApplication("JvmRamCostMonitor", None, None)
        else:
            self.app = ROOT.gApplication
        
        # Настройка стиля
        gStyle.SetOptStat(0)
        gStyle.SetTitleFont(42, "XYZ")
        gStyle.SetLabelFont(42, "XYZ")
    
    def _create_ui(self) -> None:
        """Создание UI"""
        # Главное окно
        self.main_frame = TGMainFrame(gClient.GetRoot(), 1200, 800)
        self.main_frame.SetWindowName("Jvm RAM Cost Monitor")
        
        # Горизонтальный контейнер для панели процессов и основного контента
        hframe = TGHorizontalFrame(self.main_frame)
        
        # Панель процессов (слева)
        self.process_panel = ProcessPanel(hframe, self)
        hframe.AddFrame(self.process_panel.get_frame(), 
                       TGLayoutHints(kLHintsLeft | kLHintsExpandY, 2, 2, 2, 2))
        
        # Вертикальный контейнер для графика и панели управления
        vframe = TGVerticalFrame(hframe)
        
        # Панель графиков
        self.graph_panel = GraphPanel(vframe, self)
        vframe.AddFrame(self.graph_panel.get_frame(),
                       TGLayoutHints(kLHintsExpandX | kLHintsExpandY, 2, 2, 2, 2))
        
        # Панель управления
        self.controls_panel = ControlsPanel(vframe, self)
        vframe.AddFrame(self.controls_panel.get_frame(),
                       TGLayoutHints(kLHintsExpandX, 2, 2, 2, 2))
        
        hframe.AddFrame(vframe,
                       TGLayoutHints(kLHintsExpandX | kLHintsExpandY, 2, 2, 2, 2))
        
        self.main_frame.AddFrame(hframe,
                                TGLayoutHints(kLHintsExpandX | kLHintsExpandY, 2, 2, 2, 2))
        
        self.main_frame.MapSubwindows()
        self.main_frame.Resize(self.main_frame.GetDefaultSize())
        self.main_frame.MapWindow()
    
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
            self.update_thread = threading.Thread(target=self._update_loop, daemon=True)
            self.update_thread.start()
        
        # Подключение к JMX если это Java процесс
        self.jmx_client.connect(pid)
    
    def stop_monitoring(self) -> None:
        """Остановить мониторинг"""
        self.running = False
        if self.current_pid:
            self.jmx_client.disconnect(self.current_pid)
        self.current_pid = None
    
    def _update_loop(self) -> None:
        """Основной цикл обновления данных"""
        last_rss_update = 0
        last_pss_update = 0
        last_ws_update = 0
        last_pws_update = 0
        last_jmx_update = 0
        
        while self.running and self.current_pid:
            current_time = time.time()
            
            # Обновление временной метки
            self.time_history.append(current_time)
            
            # RSS (Linux) / WS (Windows) - каждые 5 сек
            if current_time - last_rss_update >= POLL_INTERVALS['rss']:
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
                last_rss_update = current_time
            
            # PSS/USS (Linux) - каждые 30 сек
            if IS_LINUX and current_time - last_pss_update >= POLL_INTERVALS['pss']:
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
                last_pss_update = current_time
            
            # PWS/PB (Windows) - каждые 10 сек
            if IS_WINDOWS and current_time - last_pws_update >= POLL_INTERVALS['pws']:
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
                last_pws_update = current_time
            
            # JMX метрики - каждые 5 сек
            if current_time - last_jmx_update >= POLL_INTERVALS['jmx']:
                heap_data = self.jmx_client.get_heap_metrics(self.current_pid)
                if 'heap_used' in heap_data:
                    self._add_data_point('heap_used', heap_data['heap_used'])
                if 'heap_committed' in heap_data:
                    self._add_data_point('heap_committed', heap_data['heap_committed'])
                
                nmt_data = self.jmx_client.get_nmt_metrics(self.current_pid)
                if 'nmt_total' in nmt_data:
                    self._add_data_point('nmt', nmt_data['nmt_total'])
                
                last_jmx_update = current_time
            
            # Обновление графика в главном потоке
            ROOT.gSystem.ProcessEvents()
            
            # Проверка состояния UI элементов
            if self.process_panel:
                self.process_panel._check_ui_state()
            if self.controls_panel:
                self.controls_panel._check_ui_state()
            
            if self.graph_panel:
                self.graph_panel.update_graph()
            
            time.sleep(0.1)  # Небольшая задержка для снижения нагрузки
    
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
    
    def run(self) -> None:
        """Запустить приложение"""
        if self.app:
            self.app.Run(True)

