"""
Панель с графиками памяти
"""
from PyQt5.QtWidgets import QWidget, QVBoxLayout
from PyQt5.QtCore import Qt
from matplotlib.figure import Figure
from matplotlib.backends.backend_qt5agg import FigureCanvasQTAgg as FigureCanvas
from matplotlib.backends.backend_qt5agg import NavigationToolbar2QT as NavigationToolbar
import matplotlib.pyplot as plt
from typing import Dict, TYPE_CHECKING
import time

if TYPE_CHECKING:
    from ui.main_window import MainWindow


class GraphPanel(QWidget):
    """Панель с графиками"""
    
    def __init__(self, main_window: "MainWindow"):
        super().__init__()
        self.main_window = main_window
        
        layout = QVBoxLayout(self)
        layout.setContentsMargins(5, 5, 5, 5)
        
        # Matplotlib figure и canvas
        self.figure = Figure(figsize=(10, 6))
        self.canvas = FigureCanvas(self.figure)
        self.axes = self.figure.add_subplot(111)
        
        # Toolbar для масштабирования и навигации
        self.toolbar = NavigationToolbar(self.canvas, self)
        
        layout.addWidget(self.toolbar)
        layout.addWidget(self.canvas)
        
        # Настройка осей
        self.axes.set_xlabel("Время (сек)")
        self.axes.set_ylabel("Память (байты)")
        self.axes.set_title("Потребление памяти")
        self.axes.grid(True)
        
        # Цвета для метрик
        self.colors = {
            'rss': 'red',
            'pss': 'blue',
            'uss': 'green',
            'ws': 'red',
            'pws': 'blue',
            'pb': 'green',
            'heap_used': 'magenta',
            'heap_committed': 'cyan',
            'nmt': 'yellow',
        }
        
        # Названия метрик
        self.metric_names = {
            'rss': 'RSS',
            'pss': 'PSS',
            'uss': 'USS',
            'ws': 'Working Set',
            'pws': 'Private Working Set',
            'pb': 'Private Bytes',
            'heap_used': 'Heap Used',
            'heap_committed': 'Heap Committed',
            'nmt': 'NMT',
        }
    
    def update_graph(self) -> None:
        """Обновить график"""
        self.axes.clear()
        
        # Получение видимости метрик из панели управления
        visibility = self.main_window.controls_panel.get_metric_visibility()
        
        # Создание графиков для видимых метрик
        start_time = 0
        if self.main_window.time_history:
            start_time = self.main_window.time_history[0] if self.main_window.time_history else time.time()
        
        # Режим отображения группы процессов
        if self.main_window.process_group_mode == 'separate' and len(self.main_window.process_group_pids) > 1:
            # Раздельный режим - отдельные графики для каждого процесса
            for metric, visible in visibility.items():
                if not visible:
                    continue
                
                for pid in self.main_window.process_group_pids:
                    times, values = self.main_window.get_data(metric, pid)
                    if not times or not values:
                        continue
                    
                    # Нормализация времени
                    normalized_times = [(t - start_time) for t in times]
                    
                    # Создание графика
                    label = f"{self.metric_names.get(metric, metric)} (PID {pid})"
                    color = self.colors.get(metric, 'black')
                    
                    self.axes.plot(normalized_times, values, label=label, color=color, linewidth=2)
        else:
            # Кумулятивный режим
            for metric, visible in visibility.items():
                if not visible:
                    continue
                
                times, values = self.main_window.get_data(metric)
                if not times or not values:
                    continue
                
                # Нормализация времени (относительно начала)
                normalized_times = [(t - start_time) for t in times]
                
                # Создание графика
                label = self.metric_names.get(metric, metric)
                color = self.colors.get(metric, 'black')
                
                self.axes.plot(normalized_times, values, label=label, color=color, linewidth=2)
        
        # Обновление графика
        self.axes.set_xlabel("Время (сек)")
        self.axes.set_ylabel("Память (байты)")
        self.axes.set_title("Потребление памяти")
        self.axes.grid(True)
        
        # Легенда
        if self.axes.get_legend_handles_labels()[0]:
            self.axes.legend(loc='upper right')
        
        self.canvas.draw()
    
    def save_screenshot(self, filepath: str) -> None:
        """Сохранить скриншот графика"""
        self.figure.savefig(filepath, dpi=100, bbox_inches='tight')
