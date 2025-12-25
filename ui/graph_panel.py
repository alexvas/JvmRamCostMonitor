"""
Панель с графиками памяти
"""
import ROOT
from ROOT import TGVerticalFrame, TRootEmbeddedCanvas, TCanvas, TGraph, TMultiGraph
from ROOT import TGLayoutHints, kLHintsExpandX, kLHintsExpandY
from ROOT import gPad, gStyle
from typing import Dict, List, TYPE_CHECKING

if TYPE_CHECKING:
    from ui.main_window import MainWindow

import time


class GraphPanel:
    """Панель с графиками"""
    
    def __init__(self, parent, main_window: "MainWindow"):
        self.main_window = main_window
        self.frame = TGVerticalFrame(parent)
        
        # Canvas для графиков
        self.embedded_canvas = TRootEmbeddedCanvas("MemoryGraph", self.frame, 1000, 600)
        self.frame.AddFrame(self.embedded_canvas,
                           TGLayoutHints(kLHintsExpandX | kLHintsExpandY, 2, 2, 2, 2))
        
        self.canvas = self.embedded_canvas.GetCanvas()
        self.canvas.cd()
        
        # Мультиграфик для всех метрик
        self.multi_graph = TMultiGraph()
        self.multi_graph.SetTitle("Потребление памяти;Время (сек);Память (байты)")
        
        # Графики для каждой метрики
        self.graphs: Dict[str, TGraph] = {}
        
        # Цвета для метрик
        self.colors = {
            'rss': ROOT.kRed,
            'pss': ROOT.kBlue,
            'uss': ROOT.kGreen,
            'ws': ROOT.kRed,
            'pws': ROOT.kBlue,
            'pb': ROOT.kGreen,
            'heap_used': ROOT.kMagenta,
            'heap_committed': ROOT.kCyan,
            'nmt': ROOT.kYellow,
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
        
        self.canvas.cd()
        self.multi_graph.Draw("AL")
        self.canvas.Update()
    
    def get_frame(self):
        """Получить фрейм панели"""
        return self.frame
    
    def update_graph(self) -> None:
        """Обновить график"""
        if not self.canvas:
            return
        
        self.canvas.cd()
        
        # Очистка старых графиков
        self.multi_graph.Clear()
        self.graphs.clear()
        
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
                    graph = TGraph(len(normalized_times))
                    graph_title = f"{self.metric_names.get(metric, metric)} (PID {pid})"
                    graph.SetTitle(graph_title)
                    # Разные цвета для разных процессов
                    color_index = self.main_window.process_group_pids.index(pid) % 10
                    base_color = self.colors.get(metric, ROOT.kBlack)
                    graph.SetLineColor(base_color + color_index)
                    graph.SetLineWidth(2)
                    graph.SetMarkerStyle(20)
                    graph.SetMarkerSize(0.5)
                    
                    for i, (t, v) in enumerate(zip(normalized_times, values)):
                        graph.SetPoint(i, t, v)
                    
                    graph_key = f"{metric}_pid{pid}"
                    self.graphs[graph_key] = graph
                    self.multi_graph.Add(graph, "L")
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
                graph = TGraph(len(normalized_times))
                graph.SetTitle(self.metric_names.get(metric, metric))
                graph.SetLineColor(self.colors.get(metric, ROOT.kBlack))
                graph.SetLineWidth(2)
                graph.SetMarkerStyle(20)
                graph.SetMarkerSize(0.5)
                
                for i, (t, v) in enumerate(zip(normalized_times, values)):
                    graph.SetPoint(i, t, v)
                
                self.graphs[metric] = graph
                self.multi_graph.Add(graph, "L")
        
        # Обновление графика
        self.canvas.cd()
        self.multi_graph.Draw("AL")
        self.multi_graph.GetXaxis().SetTitle("Время (сек)")
        self.multi_graph.GetYaxis().SetTitle("Память (байты)")
        
        # Легенда
        if self.multi_graph.GetListOfGraphs().GetSize() > 0:
            self.canvas.BuildLegend(0.7, 0.7, 0.95, 0.95)
        
        self.canvas.Modified()
        self.canvas.Update()
        ROOT.gSystem.ProcessEvents()
    
    def save_screenshot(self, filepath: str) -> None:
        """Сохранить скриншот графика"""
        if self.canvas:
            self.canvas.SaveAs(filepath)

