"""
Панель выбора процесса (слева)
"""
import ROOT
from ROOT import TGVerticalFrame, TGGroupFrame, TGListBox, TGTextButton
from ROOT import TGLayoutHints, kLHintsExpandX, kLHintsExpandY, kLHintsLeft
from ROOT import gClient
import threading
import time

# Глобальный словарь для хранения ссылок на экземпляры панелей
_panel_instances = {}

def _toggle_panel_wrapper():
    """Обёртка для toggle_panel"""
    # Получаем последний созданный экземпляр (упрощённый подход)
    # В реальности нужно использовать более надёжный механизм
    if _panel_instances:
        panel = list(_panel_instances.values())[-1]
        panel.toggle_panel()

def _refresh_processes_wrapper():
    """Обёртка для refresh_processes"""
    if _panel_instances:
        panel = list(_panel_instances.values())[-1]
        panel.refresh_processes()

def _on_process_selected_wrapper(entry_id):
    """Обёртка для on_process_selected"""
    if _panel_instances:
        panel = list(_panel_instances.values())[-1]
        panel.on_process_selected(entry_id)

def _on_children_toggled_wrapper(state):
    """Обёртка для on_children_toggled"""
    if _panel_instances:
        panel = list(_panel_instances.values())[-1]
        panel.on_children_toggled(state)

def _on_mode_changed_wrapper():
    """Обёртка для on_mode_changed"""
    if _panel_instances:
        panel = list(_panel_instances.values())[-1]
        panel.on_mode_changed()


class ProcessPanel:
    """Панель выбора процесса"""
    
    def __init__(self, parent, main_window):
        self.main_window = main_window
        self.frame = TGVerticalFrame(parent)
        
        # Регистрируем экземпляр в глобальном словаре
        _panel_instances[id(self)] = self
        
        # Кнопка скрытия/показа
        self.toggle_button = TGTextButton(self.frame, "Скрыть панель")
        self.toggle_button_clicked = False
        self.frame.AddFrame(self.toggle_button,
                           TGLayoutHints(kLHintsExpandX, 2, 2, 2, 2))
        
        # Группа выбора процесса
        self.process_group = TGGroupFrame(self.frame, "Java процессы")
        
        # Список процессов
        self.process_list = TGListBox(self.process_group)
        self.last_selected_entry = -1
        self.process_group.AddFrame(self.process_list,
                                   TGLayoutHints(kLHintsExpandX | kLHintsExpandY, 2, 2, 2, 2))
        
        # Кнопка обновления списка
        self.refresh_button = TGTextButton(self.process_group, "Обновить")
        self.refresh_button_clicked = False
        self.process_group.AddFrame(self.refresh_button,
                                   TGLayoutHints(kLHintsExpandX, 2, 2, 2, 2))
        
        self.frame.AddFrame(self.process_group,
                           TGLayoutHints(kLHintsExpandX | kLHintsExpandY, 2, 2, 2, 2))
        
        # Группа настроек
        self.settings_group = TGGroupFrame(self.frame, "Настройки")
        
        # Чекбокс для включения потомков
        from ROOT import TGCheckButton
        self.children_check = TGCheckButton(self.settings_group, "Включать потомки")
        self.children_check.SetState(ROOT.kButtonUp)
        self.last_children_state = False
        self.settings_group.AddFrame(self.children_check,
                                     TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        
        # Режим отображения группы процессов
        from ROOT import TGRadioButton, TGButtonGroup
        self.mode_group = TGButtonGroup(self.settings_group, "Режим группы")
        self.cumulative_radio = TGRadioButton(self.mode_group, "Кумулятивный")
        self.separate_radio = TGRadioButton(self.mode_group, "Раздельный")
        self.cumulative_radio.SetState(ROOT.kButtonDown)
        self.last_mode = 'cumulative'
        self.mode_group.Show()
        self.settings_group.AddFrame(self.mode_group,
                                    TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        
        self.frame.AddFrame(self.settings_group,
                           TGLayoutHints(kLHintsExpandX, 2, 2, 2, 2))
        
        self.processes = []
        self.visible = True
        
        # Автообновление списка процессов
        self.refresh_thread = None
        self.refresh_running = True
        self._start_auto_refresh()
        
        # Первоначальная загрузка
        self.refresh_processes()
    
    def _start_auto_refresh(self):
        """Запустить автоматическое обновление списка процессов"""
        def refresh_loop():
            while self.refresh_running:
                time.sleep(0.1)  # Проверка каждые 100мс
                if self.visible:
                    ROOT.gSystem.ProcessEvents()
                    self._check_ui_state()
                # Автообновление списка каждые 5 секунд
                if int(time.time()) % 5 == 0:
                    self.refresh_processes()
        
        self.refresh_thread = threading.Thread(target=refresh_loop, daemon=True)
        self.refresh_thread.start()
    
    def _check_ui_state(self):
        """Проверка состояния UI элементов и обработка событий"""
        # Проверка состояния чекбокса потомков
        current_children_state = self.children_check.IsOn()
        if current_children_state != self.last_children_state:
            self.last_children_state = current_children_state
            self.on_children_toggled(current_children_state)
        
        # Проверка режима группы процессов
        if self.cumulative_radio.IsOn():
            current_mode = 'cumulative'
        else:
            current_mode = 'separate'
        if current_mode != self.last_mode:
            self.last_mode = current_mode
            self.on_mode_changed()
        
        # Проверка выбора процесса в списке
        selected_entry = self.process_list.GetSelected()
        if selected_entry >= 0 and selected_entry != self.last_selected_entry:
            self.last_selected_entry = selected_entry
            self.on_process_selected(selected_entry)
    
    def get_frame(self):
        """Получить фрейм панели"""
        return self.frame
    
    def toggle_panel(self):
        """Переключить видимость панели"""
        self.visible = not self.visible
        if self.visible:
            self.frame.MapWindow()
            self.toggle_button.SetText("Скрыть панель")
        else:
            self.frame.UnmapWindow()
            self.toggle_button.SetText("Показать панель")
    
    def refresh_processes(self):
        """Обновить список процессов"""
        self.processes = self.main_window.process_manager.get_java_processes()
        
        # Очистка списка
        self.process_list.RemoveAll()
        
        # Заполнение списка
        for i, proc in enumerate(self.processes):
            display_text = f"{proc['name']} (PID: {proc['pid']})"
            self.process_list.AddEntry(display_text, i)
        
        self.process_list.Layout()
        ROOT.gSystem.ProcessEvents()
    
    def on_process_selected(self, entry_id):
        """Обработчик выбора процесса"""
        if 0 <= entry_id < len(self.processes):
            proc = self.processes[entry_id]
            pid = proc['pid']
            
            # Остановка предыдущего мониторинга
            self.main_window.stop_monitoring()
            
            # Запуск нового мониторинга
            include_children = self.children_check.IsOn()
            self.main_window.start_monitoring(pid, include_children)
    
    def on_children_toggled(self, state):
        """Обработчик переключения опции потомков"""
        if self.main_window.current_pid:
            include_children = bool(state)
            self.main_window.stop_monitoring()
            self.main_window.start_monitoring(self.main_window.current_pid, include_children)
    
    def on_mode_changed(self):
        """Обработчик изменения режима отображения группы"""
        if self.cumulative_radio.IsOn():
            mode = 'cumulative'
        else:
            mode = 'separate'
        
        self.main_window.set_process_group_mode(mode)

