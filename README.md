# JvmRamCostMonitor

Кроссплатформенный монитор потребления RAM процессами с GUI на PyROOT, аналогичный VisualVM, но с дополнительными метриками offheap памяти.

## Возможности

- **Кроссплатформенность**: Linux и Windows
- **Метрики памяти**:
  - Linux: RSS, PSS, USS
  - Windows: Working Set, Private Working Set, Private Bytes
  - JMX: NMT, Used Heap, Committed Heap
- **Мониторинг группы процессов**: с опцией включения потомков
- **Режимы отображения**: кумулятивный или раздельный для группы процессов
- **Управление Java процессами**: GC, Heap Dump
- **Графики**: отображение всех метрик на едином графике
- **Настройка отображения**: выбор видимых метрик

## Требования

- Python >= 3.8
- ROOT (PyROOT) - должен быть установлен в системе
- JDK с утилитой `jps` (для обнаружения Java процессов)
- psutil >= 5.9.0
- jpype1 >= 1.4.0 (для JMX подключения)

## Установка

1. Установите CERN ROOT / PyRoot с использованием conda (рекомендуется):
   ```bash
   # Установка Miniconda (если еще не установлена)
   # Linux/Mac:
   wget https://repo.anaconda.com/miniconda/Miniconda3-latest-Linux-x86_64.sh
   # Проверка чексуммы (рекомендуется):
   sha256sum Miniconda3-latest-Linux-x86_64.sh
   # Сравните полученный хэш с официальным на странице:
   # https://repo.anaconda.com/miniconda/
   bash Miniconda3-latest-Linux-x86_64.sh
   # Следуйте инструкциям установщика
   
   # Windows:
   # Скачайте установщик с https://docs.conda.io/en/latest/miniconda.html
   # Запустите установщик и следуйте инструкциям
   
   # После установки перезапустите терминал или выполните:
   source ~/.bashrc  # Linux/Mac
   # или
   source ~/.zshrc   # если используете zsh
   
   # Создание окружения с ROOT
   conda create -n root-env python=3.8
   conda activate root-env
   conda install -c conda-forge root
   ```
   
   **Для активации окружения root-env в дальнейшем:**
   ```bash
   conda activate root-env
   ```

2. Альтернативные способы установки ROOT:

   **Создайте виртуальное окружение (для Ubuntu/Debian):**
   ```bash
   sudo apt update
   sudo apt install python3-venv
   python3 -m venv venv
   source venv/bin/activate  # Linux/Mac
   # или
   venv\Scripts\activate  # Windows
   ```

   **Вариант A: Установка через pip (если доступен PyRoot wheel)**
   ```bash
   pip install root
   ```

   **Вариант B: Установка из исходников или бинарных пакетов**
   ```bash
   # Скачайте и установите ROOT согласно официальной документации CERN
   # https://root.cern.ch/downloading-root
   # После установки убедитесь, что ROOT доступен в текущем окружении
   ```

3. Установите зависимости:

   **Если используете conda окружение (из шага 1):**
   ```bash
   conda install -c conda-forge psutil jpype1
   ```

   **Или через pip:**
   ```bash
   pip install -r requirements.txt
   ```

4. Убедитесь, что ROOT установлен и доступен:
```bash
python -c "import ROOT; print(ROOT.__version__)"
```

5. Убедитесь, что `jps` доступен в PATH:
```bash
jps -l
```

## Использование

Запустите приложение:
```bash
python main.py
```

### Интерфейс

- **Левая панель**: список Java процессов, настройки мониторинга
- **Центральная область**: график потребления памяти
- **Нижняя панель**: кнопки управления (GC, Heap Dump, сохранение) и настройки отображения метрик

### Интервалы опроса

- RSS (Linux): 5 сек
- PSS/USS (Linux): 30 сек
- Working Set (Windows): 5 сек
- Private Working Set/Private Bytes (Windows): 10 сек
- JMX метрики: 5 сек

### Настройки

- **Включать потомки**: при включении мониторинг распространяется на все дочерние процессы
- **Режим группы**: 
  - Кумулятивный: суммирование метрик всех процессов группы
  - Раздельный: отдельные графики для каждого процесса

## Примечания

- JMX подключение работает только для локальных процессов
- Для работы JMX функций (GC, Heap Dump) процесс должен быть запущен с соответствующими опциями JMX
- NMT метрики требуют включения Native Memory Tracking в Java процессе

## Структура проекта

```
JvmRamCostMonitor/
├── main.py                 # Точка входа
├── config.py               # Конфигурация
├── memory_monitor.py       # Сбор метрик памяти
├── jmx_client.py           # JMX клиент
├── process_manager.py      # Управление процессами
├── ui/                     # UI компоненты
│   ├── main_window.py
│   ├── process_panel.py
│   ├── graph_panel.py
│   └── controls_panel.py
└── requirements.txt
```

