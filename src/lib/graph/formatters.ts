/**
 * Функции форматирования для меток осей графика
 */

/**
 * Форматирует время для меток абсциссы (относительно начала графика)
 * @param tick - абсолютное время в десятых долях секунды
 * @param minTime - минимальное время (начало графика) в десятых долях секунды
 * @param interval - интервал сетки в десятых долях секунды
 * @returns отформатированная строка времени
 */
export function formatTimeLabel(
  tick: number,
  minTime: number,
  interval: number,
): string {
  const relativeTenthsOfSecond = tick - minTime;
  const seconds = Math.floor(relativeTenthsOfSecond / 10);
  const minutes = Math.floor(seconds / 60);
  const hours = Math.floor(minutes / 60);

  if (interval < 60 * 10) {
    return `${seconds}s`;
  } else if (interval < 60 * 60 * 10) {
    return `${minutes}m`;
  } else {
    return `${hours}h`;
  }
}

/**
 * Форматирует байты для меток ординаты
 * @param kb - значение в килобайтах
 * @returns отформатированная строка с единицами измерения
 */
export function formatBytesLabel(kb: number): string {
  const mb = 1024;
  const gb = 1024 * 1024;
  const tb = 1024 * 1024 * 1024;

  if (kb >= tb) {
    const tbValue = kb / tb;
    return `${tbValue.toFixed(tbValue >= 10 ? 0 : 1)}TB`;
  } else if (kb >= gb) {
    const gbValue = kb / gb;
    return `${gbValue.toFixed(gbValue >= 10 ? 0 : 1)}GB`;
  } else if (kb >= mb) {
    const mbValue = kb / mb;
    return `${mbValue.toFixed(mbValue >= 10 ? 0 : 1)}MB`;
  } else {
    return `${kb}KB`;
  }
}
