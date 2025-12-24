"""
Панель выбора процесса (слева)
"""
import ROOT
from ROOT import TGVerticalFrame, TGGroupFrame, TGListBox, TGTextButton
from ROOT import TGLayoutHints, kLHintsExpandX, kLHintsExpandY
from ROOT import gClient
import threading
import time


class ProcessPanel:
    """Панель выбора процесса"""
    
    def __init__(self, parent, main_window):
        self.main_window = main_window
        self.frame = TGVerticalFrame(parent)
        
        # Кнопка скрытия/показа
        self.toggle_button = TGTextButton(self.frame, "Скрыть панель")
        self.toggle_button.Connect("Clicked()", "ProcessPanel", self, "toggle_panel()")
        self.frame.AddFrame(self.toggle_button,
                           TGLayoutHints(kLHintsExpandX, 2, 2, 2, 2))
        
        # Группа выбора процесса
        self.process_group = TGGroupFrame(self.frame, "Java процессы")
        
        # Список процессов
        self.process_list = TGListBox(self.process_group)
        self.process_list.Connect("Selected(int)", "ProcessPanel", self, "on_process_selected(int)")
        self.process_group.AddFrame(self.process_list,
                                   TGLayoutHints(kLHintsExpandX | kLHintsExpandY, 2, 2, 2, 2))
        
        # Кнопка обновления списка
        self.refresh_button = TGTextButton(self.process_group, "Обновить")
        self.refresh_button.Connect("Clicked()", "ProcessPanel", self, "refresh_processes()")
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
        self.children_check.Connect("Toggled(Bool_t)", "ProcessPanel", self, "on_children_toggled(Bool_t)")
        self.settings_group.AddFrame(self.children_check,
                                     TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        
        # Режим отображения группы процессов
        from ROOT import TGRadioButton, TGButtonGroup
        self.mode_group = TGButtonGroup(self.settings_group, "Режим группы")
        self.cumulative_radio = TGRadioButton(self.mode_group, "Кумулятивный")
        self.separate_radio = TGRadioButton(self.mode_group, "Раздельный")
        self.cumulative_radio.SetState(ROOT.kButtonDown)
        self.cumulative_radio.Connect("Clicked()", "ProcessPanel", self, "on_mode_changed()")
        self.separate_radio.Connect("Clicked()", "ProcessPanel", self, "on_mode_changed()")
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
                time.sleep(5)  # Обновление каждые 5 секунд
                if self.visible:
                    ROOT.gSystem.ProcessEvents()
                    self.refresh_processes()
        
        self.refresh_thread = threading.Thread(target=refresh_loop, daemon=True)
        self.refresh_thread.start()
    
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

