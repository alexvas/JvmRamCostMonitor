"""
Панель управления: кнопки GC, HeapDump, сохранение, настройки метрик
"""
import sys
import os
# Добавляем родительскую директорию в путь для импорта модулей из корня проекта
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from PyQt5.QtWidgets import (QWidget, QHBoxLayout, QVBoxLayout, QGroupBox,
                             QPushButton, QCheckBox, QFileDialog)
from PyQt5.QtCore import Qt
from typing import TYPE_CHECKING, Dict

if TYPE_CHECKING:
    from ui.main_window import MainWindow


class ControlsPanel(QWidget):
    """Панель управления"""
    
    def __init__(self, main_window: "MainWindow"):
        super().__init__()
        self.main_window = main_window
        
        layout = QHBoxLayout(self)
        layout.setContentsMargins(5, 5, 5, 5)
        
        # Группа действий
        actions_group = QGroupBox("Действия")
        actions_layout = QVBoxLayout()
        
        gc_button = QPushButton("GC")
        gc_button.clicked.connect(self.on_gc_clicked)
        actions_layout.addWidget(gc_button)
        
        heapdump_button = QPushButton("Heap Dump")
        heapdump_button.clicked.connect(self.on_heapdump_clicked)
        actions_layout.addWidget(heapdump_button)
        
        save_button = QPushButton("Сохранить график")
        save_button.clicked.connect(self.on_save_clicked)
        actions_layout.addWidget(save_button)
        
        actions_group.setLayout(actions_layout)
        layout.addWidget(actions_group)
        
        # Группа настроек метрик
        metrics_group = QGroupBox("Отображаемые метрики")
        metrics_layout = QVBoxLayout()
        
        self.metric_checks = {}
        
        from config import DEFAULT_METRIC_VISIBILITY, IS_LINUX, IS_WINDOWS
        
        # Linux метрики
        if IS_LINUX:
            self._add_metric_check(metrics_layout, 'rss', 'RSS')
            self._add_metric_check(metrics_layout, 'pss', 'PSS')
            self._add_metric_check(metrics_layout, 'uss', 'USS')
        
        # Windows метрики
        if IS_WINDOWS:
            self._add_metric_check(metrics_layout, 'ws', 'Working Set')
            self._add_metric_check(metrics_layout, 'pws', 'Private Working Set')
            self._add_metric_check(metrics_layout, 'pb', 'Private Bytes')
        
        # JMX метрики (для всех платформ)
        self._add_metric_check(metrics_layout, 'heap_used', 'Heap Used')
        self._add_metric_check(metrics_layout, 'heap_committed', 'Heap Committed')
        self._add_metric_check(metrics_layout, 'nmt', 'NMT')
        
        metrics_group.setLayout(metrics_layout)
        layout.addWidget(metrics_group)
    
    def _add_metric_check(self, layout, metric: str, label: str) -> None:
        """Добавить чекбокс для метрики"""
        from config import DEFAULT_METRIC_VISIBILITY
        
        check = QCheckBox(label)
        default_visible = DEFAULT_METRIC_VISIBILITY.get(metric, True)
        check.setChecked(default_visible)
        
        layout.addWidget(check)
        self.metric_checks[metric] = check
    
    def get_metric_visibility(self) -> Dict[str, bool]:
        """Получить словарь видимости метрик"""
        return {metric: check.isChecked() for metric, check in self.metric_checks.items()}
    
    def on_gc_clicked(self) -> None:
        """Обработчик нажатия кнопки GC"""
        self.main_window.trigger_gc()
    
    def on_heapdump_clicked(self) -> None:
        """Обработчик нажатия кнопки Heap Dump"""
        filepath, _ = QFileDialog.getSaveFileName(
            self, "Сохранить Heap Dump", "", "Heap Dump (*.hprof);;All Files (*)"
        )
        if filepath:
            if self.main_window.create_heap_dump(filepath):
                print(f"Heap dump сохранен: {filepath}")
            else:
                print("Ошибка создания heap dump")
    
    def on_save_clicked(self) -> None:
        """Обработчик нажатия кнопки сохранения"""
        filepath, _ = QFileDialog.getSaveFileName(
            self, "Сохранить график", "", "PNG (*.png);;PDF (*.pdf);;SVG (*.svg);;All Files (*)"
        )
        if filepath:
            self.main_window.save_screenshot(filepath)
            print(f"График сохранен: {filepath}")
