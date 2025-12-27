"""
Панель выбора процесса (слева)
"""
from PyQt5.QtWidgets import (QWidget, QVBoxLayout, QHBoxLayout, QPushButton, QGroupBox,
                             QListWidget, QCheckBox, QRadioButton, QButtonGroup)
from PyQt5.QtCore import Qt, QTimer
from typing import TYPE_CHECKING

if TYPE_CHECKING:
    from ui.main_window import MainWindow


class ProcessPanel(QWidget):
    """Панель выбора процесса"""
    
    def __init__(self, main_window: "MainWindow"):
        super().__init__()
        self.main_window = main_window
        
        # Основной layout для контейнера
        main_layout = QVBoxLayout(self)
        main_layout.setContentsMargins(0, 0, 0, 0)
        main_layout.setSpacing(0)
        
        # Контейнер для содержимого панели
        self.content_widget = QWidget()
        layout = QVBoxLayout(self.content_widget)
        layout.setContentsMargins(5, 5, 5, 5)
        
        # Кнопка скрытия панели (внутри панели)
        self.hide_button = QPushButton("Скрыть панель")
        self.hide_button.clicked.connect(self.hide_panel)
        layout.addWidget(self.hide_button)
        
        # Группа выбора процесса
        process_group = QGroupBox("Java процессы")
        process_layout = QVBoxLayout()
        
        self.process_list = QListWidget()
        self.process_list.itemSelectionChanged.connect(self.on_process_selected)
        process_layout.addWidget(self.process_list)
        
        refresh_button = QPushButton("Обновить")
        refresh_button.clicked.connect(self.refresh_processes)
        process_layout.addWidget(refresh_button)
        
        process_group.setLayout(process_layout)
        layout.addWidget(process_group)
        
        # Группа настроек
        settings_group = QGroupBox("Настройки")
        settings_layout = QVBoxLayout()
        
        self.children_check = QCheckBox("Включать потомки")
        self.children_check.stateChanged.connect(self.on_children_toggled)
        settings_layout.addWidget(self.children_check)
        
        mode_group = QGroupBox("Режим группы")
        mode_layout = QVBoxLayout()
        
        self.mode_button_group = QButtonGroup()
        self.cumulative_radio = QRadioButton("Кумулятивный")
        self.separate_radio = QRadioButton("Раздельный")
        self.cumulative_radio.setChecked(True)
        self.last_mode = 'cumulative'
        
        self.mode_button_group.addButton(self.cumulative_radio, 0)
        self.mode_button_group.addButton(self.separate_radio, 1)
        self.mode_button_group.buttonClicked.connect(self.on_mode_changed)
        
        mode_layout.addWidget(self.cumulative_radio)
        mode_layout.addWidget(self.separate_radio)
        mode_group.setLayout(mode_layout)
        settings_layout.addWidget(mode_group)
        
        settings_group.setLayout(settings_layout)
        layout.addWidget(settings_group)
        
        layout.addStretch()
        
        # Узкая кнопка для показа панели (видна только когда панель скрыта)
        # Используем абсолютное позиционирование, чтобы она всегда была видна
        self.show_button = QPushButton("▶", self)
        self.show_button.setFixedSize(30, 30)
        self.show_button.setToolTip("Показать панель")
        self.show_button.clicked.connect(self.show_panel)
        self.show_button.hide()  # По умолчанию скрыта
        self.show_button.move(0, 0)  # Позиционируем в левом верхнем углу
        
        # Добавляем содержимое панели в основной layout
        main_layout.addWidget(self.content_widget)
        
        self.visible = True
        self.processes = []
        
        # Автообновление списка процессов
        self.refresh_timer = QTimer()
        self.refresh_timer.timeout.connect(self.refresh_processes)
        self.refresh_timer.start(5000)  # Каждые 5 секунд
        
        # Первоначальная загрузка
        self.refresh_processes()


    def show_panel(self) -> None:
        """Показать панель"""
        self.visible = True
        # Скрываем узкую кнопку.
        self.show_button.hide()
        # Показываем панель.
        self.content_widget.show()
        # Восстанавливаем нормальные размеры (снимаем фиксированный размер)
        self.setMinimumSize(0, 0)
        self.setMaximumSize(16777215, 16777215)

    def hide_panel(self) -> None:
        """Скрыть панель"""
        self.visible = False
        # Скрываем панель.
        self.content_widget.hide()
        # Устанавливаем фиксированный размер для панели, чтобы узкая кнопка была видна
        self.setFixedSize(30, 30)
        # Показываем узкую кнопку.
        self.show_button.show()
        self.show_button.move(0, 0)
        self.show_button.raise_()
    
    def refresh_processes(self) -> None:
        """Обновить список процессов"""
        if not self.visible:
            return
        
        self.processes = self.main_window.process_manager.get_java_processes()
        
        # Сохраняем текущий выбор
        current_item = self.process_list.currentItem()
        current_pid = None
        if current_item:
            current_text = current_item.text()
            # Извлекаем PID из текста
            try:
                if "(PID:" in current_text:
                    pid_str = current_text.split("(PID:")[1].split(")")[0].strip()
                    current_pid = int(pid_str)
            except:
                pass
        
        # Очистка списка
        self.process_list.clear()
        
        # Заполнение списка
        selected_index = -1
        for i, proc in enumerate(self.processes):
            display_text = f"{proc.name} (PID: {proc.pid})"
            self.process_list.addItem(display_text)
            if current_pid and proc.pid == current_pid:
                selected_index = i
        
        # Восстанавливаем выбор
        if selected_index >= 0:
            self.process_list.setCurrentRow(selected_index)
    
    def on_process_selected(self) -> None:
        """Обработчик выбора процесса"""
        current_item = self.process_list.currentItem()
        if current_item:
            index = self.process_list.row(current_item)
            if 0 <= index < len(self.processes):
                proc = self.processes[index]
                pid = proc.pid
                
                # Остановка предыдущего мониторинга
                self.main_window.stop_monitoring()
                
                # Запуск нового мониторинга
                include_children = self.children_check.isChecked()
                self.main_window.start_monitoring(pid, include_children)
    
    def on_children_toggled(self, state: int) -> None:
        """Обработчик переключения опции потомков"""
        if self.main_window.current_pid:
            include_children = bool(state)
            self.main_window.stop_monitoring()
            self.main_window.start_monitoring(self.main_window.current_pid, include_children)
    
    def on_mode_changed(self, button: QRadioButton) -> None:
        """Обработчик изменения режима отображения группы"""
        mode = 'cumulative' if button == self.cumulative_radio else 'separate'
        if mode != self.last_mode:
            self.last_mode = mode
            self.main_window.set_process_group_mode(mode)
