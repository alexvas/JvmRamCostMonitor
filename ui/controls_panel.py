"""
Панель управления: кнопки GC, HeapDump, сохранение, настройки метрик
"""
import ROOT
from ROOT import TGHorizontalFrame, TGVerticalFrame, TGGroupFrame, TGTextButton
from ROOT import TGCheckButton, TGLayoutHints, kLHintsExpandX
from ROOT import gSystem
import os


class ControlsPanel:
    """Панель управления"""
    
    def __init__(self, parent, main_window):
        self.main_window = main_window
        
        # Горизонтальный контейнер
        self.frame = TGHorizontalFrame(parent)
        
        # Группа действий
        self.actions_group = TGGroupFrame(self.frame, "Действия")
        
        # Кнопка GC
        self.gc_button = TGTextButton(self.actions_group, "GC")
        self.gc_button.Connect("Clicked()", "ControlsPanel", self, "on_gc_clicked()")
        self.actions_group.AddFrame(self.gc_button,
                                    TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        
        # Кнопка HeapDump
        self.heapdump_button = TGTextButton(self.actions_group, "Heap Dump")
        self.heapdump_button.Connect("Clicked()", "ControlsPanel", self, "on_heapdump_clicked()")
        self.actions_group.AddFrame(self.heapdump_button,
                                    TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        
        # Кнопка сохранения скриншота
        self.save_button = TGTextButton(self.actions_group, "Сохранить график")
        self.save_button.Connect("Clicked()", "ControlsPanel", self, "on_save_clicked()")
        self.actions_group.AddFrame(self.save_button,
                                    TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        
        self.frame.AddFrame(self.actions_group,
                           TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        
        # Группа настроек метрик
        self.metrics_group = TGGroupFrame(self.frame, "Отображаемые метрики")
        
        # Чекбоксы для метрик
        self.metric_checks = {}
        
        from ..config import DEFAULT_METRIC_VISIBILITY, IS_LINUX, IS_WINDOWS
        
        # Linux метрики
        if IS_LINUX:
            self._add_metric_check('rss', 'RSS')
            self._add_metric_check('pss', 'PSS')
            self._add_metric_check('uss', 'USS')
        
        # Windows метрики
        if IS_WINDOWS:
            self._add_metric_check('ws', 'Working Set')
            self._add_metric_check('pws', 'Private Working Set')
            self._add_metric_check('pb', 'Private Bytes')
        
        # JMX метрики (для всех платформ)
        self._add_metric_check('heap_used', 'Heap Used')
        self._add_metric_check('heap_committed', 'Heap Committed')
        self._add_metric_check('nmt', 'NMT')
        
        self.frame.AddFrame(self.metrics_group,
                           TGLayoutHints(kLHintsLeft | kLHintsExpandX, 2, 2, 2, 2))
    
    def _add_metric_check(self, metric: str, label: str):
        """Добавить чекбокс для метрики"""
        from ..config import DEFAULT_METRIC_VISIBILITY
        
        check = TGCheckButton(self.metrics_group, label)
        default_visible = DEFAULT_METRIC_VISIBILITY.get(metric, True)
        if default_visible:
            check.SetState(ROOT.kButtonDown)
        else:
            check.SetState(ROOT.kButtonUp)
        
        self.metrics_group.AddFrame(check,
                                   TGLayoutHints(kLHintsLeft, 2, 2, 2, 2))
        self.metric_checks[metric] = check
    
    def get_frame(self):
        """Получить фрейм панели"""
        return self.frame
    
    def get_metric_visibility(self) -> dict:
        """Получить словарь видимости метрик"""
        visibility = {}
        for metric, check in self.metric_checks.items():
            visibility[metric] = check.IsOn()
        return visibility
    
    def on_gc_clicked(self):
        """Обработчик нажатия кнопки GC"""
        self.main_window.trigger_gc()
    
    def on_heapdump_clicked(self):
        """Обработчик нажатия кнопки Heap Dump"""
        # Простой диалог выбора файла через системный диалог
        # В PyROOT нет встроенного диалога, используем временное решение
        import tempfile
        default_path = os.path.join(tempfile.gettempdir(), f"heapdump_{self.main_window.current_pid}.hprof")
        
        # Для полноценного диалога можно использовать ROOT.TGFileDialog
        # или интегрировать с системным диалогом
        filepath = default_path
        
        if self.main_window.create_heap_dump(filepath):
            print(f"Heap dump сохранен: {filepath}")
        else:
            print("Ошибка создания heap dump")
    
    def on_save_clicked(self):
        """Обработчик нажатия кнопки сохранения"""
        import tempfile
        import time
        default_path = os.path.join(
            tempfile.gettempdir(),
            f"memory_graph_{int(time.time())}.png"
        )
        
        # Простое сохранение в временную директорию
        # Для полноценного диалога нужна дополнительная реализация
        filepath = default_path
        self.main_window.save_screenshot(filepath)
        print(f"График сохранен: {filepath}")

