"""
Конфигурация интервалов опроса и настроек отображения метрик
"""
import platform

# Интервалы опроса (в секундах)
POLL_INTERVALS = {
    'rss': 5,           # RSS на Linux
    'pss': 30,          # PSS на Linux
    'uss': 30,          # USS на Linux
    'ws': 5,            # Working Set на Windows
    'pws': 10,          # Private Working Set на Windows
    'pb': 10,           # Private Bytes на Windows
    'jmx': 5,           # JMX метрики (NMT, Heap)
}

# Настройки отображения метрик по умолчанию
DEFAULT_METRIC_VISIBILITY = {
    'rss': True,
    'pss': True,
    'uss': False,
    'ws': True,
    'pws': True,
    'pb': False,
    'heap_used': True,
    'heap_committed': True,
    'nmt': True,
}

# Определение платформы
IS_LINUX = platform.system() == 'Linux'
IS_WINDOWS = platform.system() == 'Windows'

