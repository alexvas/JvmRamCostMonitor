#!/usr/bin/env python3
"""
Точка входа приложения JvmRamCostMonitor
"""
import sys
import ROOT
from ui.main_window import MainWindow


def main():
    """Главная функция"""
    try:
        # Создание и запуск главного окна
        window = MainWindow()
        window.run()
    except KeyboardInterrupt:
        print("\nПриложение завершено пользователем")
        sys.exit(0)
    except Exception as e:
        print(f"Ошибка при запуске приложения: {e}", file=sys.stderr)
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == "__main__":
    main()

