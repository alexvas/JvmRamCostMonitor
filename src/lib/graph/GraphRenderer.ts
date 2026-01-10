/**
 * Основной класс для рендеринга SVG-графиков
 */

import type {
  GraphConfig,
  GraphConfigResolved,
  GraphRenderData,
  GraphTransform,
  GridLine,
  MetricColorMap,
  MetricNameMap,
} from './types';

import {
  DEFAULT_PADDING_PERCENT,
  DEFAULT_BOTTOM_LABEL_SPACE,
  DEFAULT_LEFT_LABEL_SPACE,
  DEFAULT_MIN_TIME_RANGE,
  GRID_INTERVALS_TENTH_OF_SECOND,
  GRID_INTERVALS_KB,
  MAX_VERTICAL_GRID_LINES,
  MAX_HORIZONTAL_GRID_LINES,
} from './constants';

import { formatTimeLabel, formatBytesLabel } from './formatters';

/**
 * Рендерер графиков в SVG
 */
export class GraphRenderer {
  private config: GraphConfigResolved;
  private metricColors: MetricColorMap;
  private metricNames: MetricNameMap;

  // Кэш для вертикальных линий
  private cachedVerticalLines: GridLine[] = [];
  private cachedVerticalMinTime: number | null = null;
  private cachedVerticalMaxTime: number | null = null;
  private cachedVerticalInterval: number | null = null;

  // Кэш для горизонтальных линий
  private cachedHorizontalLines: GridLine[] = [];
  private cachedHorizontalMaxKb: number | null = null;
  private cachedHorizontalInterval: number | null = null;

  constructor(
    config: GraphConfig,
    metricColors: MetricColorMap,
    metricNames: MetricNameMap,
  ) {
    this.config = {
      containerWidth: config.containerWidth,
      containerHeight: config.containerHeight,
      paddingPercent: config.paddingPercent ?? DEFAULT_PADDING_PERCENT,
      bottomLabelSpace: config.bottomLabelSpace ?? DEFAULT_BOTTOM_LABEL_SPACE,
      leftLabelSpace: config.leftLabelSpace ?? DEFAULT_LEFT_LABEL_SPACE,
      minTimeRange: config.minTimeRange ?? DEFAULT_MIN_TIME_RANGE,
      prefersDark: config.prefersDark ?? false,
    };
    this.metricColors = metricColors;
    this.metricNames = metricNames;
  }

  /**
   * Обновить размеры контейнера
   */
  updateSize(width: number, height: number): void {
    this.config.containerWidth = width;
    this.config.containerHeight = height;
  }

  /**
   * Обновить тему (светлая/тёмная)
   */
  updateTheme(prefersDark: boolean): void {
    this.config.prefersDark = prefersDark;
  }

  /**
   * Получить текущую конфигурацию
   */
  getConfig(): Readonly<GraphConfigResolved> {
    return this.config;
  }

  /**
   * Вычислить трансформации для графика
   */
  getTransform(data: GraphRenderData): GraphTransform {
    const { containerWidth, containerHeight, paddingPercent,
            bottomLabelSpace, leftLabelSpace, minTimeRange } = this.config;
    
    const paddingX = containerWidth * paddingPercent;
    const paddingY = containerHeight * paddingPercent;
    
    const graphAreaWidth = containerWidth - paddingX * 2 - leftLabelSpace;
    const graphAreaHeight = containerHeight - paddingY * 2 - bottomLabelSpace;
    
    const timeRange = Math.max(
      data.processMinMax.maxMoment - data.processMinMax.minMoment,
      minTimeRange
    );
    const dataWidth = timeRange;
    const dataHeight = data.processMinMax.maxKb || 1;

    return {
      translateX: paddingX + leftLabelSpace,
      translateY: paddingY,
      scaleX: dataWidth === 0 ? 1 : graphAreaWidth / dataWidth,
      scaleY: dataHeight === 0 ? 1 : graphAreaHeight / dataHeight,
      dataWidth,
      dataHeight,
      graphAreaWidth,
      graphAreaHeight,
    };
  }

  /**
   * Вычислить вертикальные линии сетки (ось времени)
   */
  getVerticalGridLines(data: GraphRenderData): GridLine[] {
    const { minMoment, maxMoment } = data.processMinMax;
    const timeRange = maxMoment - minMoment;

    // Выбираем интервал
    let selectedInterval = GRID_INTERVALS_TENTH_OF_SECOND[GRID_INTERVALS_TENTH_OF_SECOND.length - 1];
    for (const interval of GRID_INTERVALS_TENTH_OF_SECOND) {
      const count = timeRange / interval;
      if (count <= MAX_VERTICAL_GRID_LINES) {
        selectedInterval = interval;
        break;
      }
    }

    // Проверяем кэш
    if (
      this.cachedVerticalMinTime === minMoment &&
      this.cachedVerticalMaxTime === maxMoment &&
      this.cachedVerticalInterval === selectedInterval &&
      this.cachedVerticalLines.length > 0
    ) {
      return this.cachedVerticalLines;
    }

    // Генерируем линии
    const firstTick = minMoment + selectedInterval;
    const lines: GridLine[] = [];

    for (let tick = firstTick; tick < maxMoment; tick += selectedInterval) {
      const positionInDataUnits = tick - minMoment;
      const label = formatTimeLabel(tick, minMoment, selectedInterval);
      lines.push({ positionInDataUnits, label });
    }

    // Кэшируем
    this.cachedVerticalLines = lines;
    this.cachedVerticalMinTime = minMoment;
    this.cachedVerticalMaxTime = maxMoment;
    this.cachedVerticalInterval = selectedInterval;

    return lines;
  }

  /**
   * Вычислить горизонтальные линии сетки (ось памяти)
   */
  getHorizontalGridLines(data: GraphRenderData): GridLine[] {
    const { maxKb } = data.processMinMax;

    // Выбираем интервал
    let selectedInterval = GRID_INTERVALS_KB[GRID_INTERVALS_KB.length - 1];
    for (const interval of GRID_INTERVALS_KB) {
      const count = maxKb / interval;
      if (count <= MAX_HORIZONTAL_GRID_LINES) {
        selectedInterval = interval;
        break;
      }
    }

    // Проверяем кэш
    if (
      this.cachedHorizontalMaxKb === maxKb &&
      this.cachedHorizontalInterval === selectedInterval &&
      this.cachedHorizontalLines.length > 0
    ) {
      return this.cachedHorizontalLines;
    }

    // Генерируем линии
    const lines: GridLine[] = [];
    for (let tick = selectedInterval; tick < maxKb; tick += selectedInterval) {
      const positionInDataUnits = maxKb - tick; // инвертировано для SVG координат
      const label = formatBytesLabel(tick);
      lines.push({ positionInDataUnits, label });
    }

    // Кэшируем
    this.cachedHorizontalLines = lines;
    this.cachedHorizontalMaxKb = maxKb;
    this.cachedHorizontalInterval = selectedInterval;

    return lines;
  }

  /**
   * Преобразовать x-координату из единиц данных в пиксели
   */
  toX(dataX: number, transform: GraphTransform): number {
    return transform.translateX + dataX * transform.scaleX;
  }

  /**
   * Преобразовать y-координату из килобайт в пиксели
   * (0 -> низ графика, maxKb -> верх)
   */
  toY(kb: number, transform: GraphTransform): number {
    return transform.translateY + transform.graphAreaHeight - kb * transform.scaleY;
  }

  /**
   * Получить цвет рамки в зависимости от темы
   */
  getFrameColor(): string {
    return this.config.prefersDark ? 'white' : 'black';
  }

  /**
   * Получить цвет фона в зависимости от темы
   */
  getBackgroundColor(): string {
    return this.config.prefersDark ? '#202020' : '#f3f3f3';
  }

  /**
   * Сгенерировать CSS-стили для SVG
   */
  renderStyles(): string {
    const frameColor = this.getFrameColor();
    const backgroundColor = this.getBackgroundColor();
    const colorKey = this.config.prefersDark ? 'color_dark' : 'color_light';

    const baseStyles = /*css*/ `
      .graph-plot {
        width: 100%;
        height: 100%;
        background-color: ${backgroundColor};
        display: block;
        max-height: 100%;
        overflow: hidden;
      }
      .graph-path {
        fill: none;
        stroke-width: 0.7;
        vector-effect: non-scaling-stroke;
      }
      .graph-frame {
        fill: none;
        stroke: ${frameColor};
        stroke-width: 0.5;
        vector-effect: non-scaling-stroke;
        opacity: 0.5;
      }
      .grid-lines {
        stroke: ${frameColor};
        stroke-width: 0.3;
        vector-effect: non-scaling-stroke;
        opacity: 0.3;
        fill: none;
      }
      .grid-label-x,
      .grid-label-y {
        fill: ${frameColor};
        font-size: 12px;
        font-family: sans-serif;
        font-weight: normal;
        pointer-events: none;
      }
    `;

    const metricStyles = Object.keys(this.metricColors)
      .map((metricTypeKey) => {
        const metricType = Number(metricTypeKey);
        const metricTypeName = this.metricNames[metricType] || `Metric${metricType}`;
        const meta = this.metricColors[metricType];
        return /*css*/ `
      .graph-path-${metricTypeName} {
        stroke: ${meta[colorKey]};
      }
    `;
      })
      .join('');

    return /*svg*/`<style>${baseStyles}${metricStyles}</style>`;
  }

  /**
   * Сгенерировать path для рамки графика
   */
  renderFrame(transform: GraphTransform): string {
    return /*svg*/`<rect class="graph-frame" x="0" y="0" width="${transform.dataWidth}" height="${transform.dataHeight}"/>`;
  }

  /**
   * Сгенерировать path для вертикальных линий сетки
   */
  renderVerticalGridLines(lines: GridLine[], dataHeight: number): string {
    if (lines.length === 0) return '';
    const d = lines
      .map((line) => `M ${line.positionInDataUnits},0 L ${line.positionInDataUnits},${dataHeight}`)
      .join(' ');
    return /*svg*/`<path class="grid-lines" d="${d}"/>`;
  }

  /**
   * Сгенерировать path для горизонтальных линий сетки
   */
  renderHorizontalGridLines(lines: GridLine[], dataWidth: number): string {
    if (lines.length === 0) return '';
    const d = lines
      .map((line) => `M 0,${line.positionInDataUnits} L ${dataWidth},${line.positionInDataUnits}`)
      .join(' ');
    return /*svg*/`<path class="grid-lines" d="${d}"/>`;
  }

  /**
   * Сгенерировать path для одного графика
   */
  renderGraphPath(
    metricType: number,
    points: Array<{ moment: number; kb: number }>,
    minMoment: number,
    maxKb: number,
  ): string {
    if (points.length === 0) return '';
    
    const metricTypeName = this.metricNames[metricType] || `Metric${metricType}`;
    const d = points
      .map((point, i) => {
        const x = point.moment - minMoment;
        const y = maxKb - point.kb;
        return `${i === 0 ? 'M' : 'L'} ${x},${y}`;
      })
      .join(' ');
    
    return /*svg*/`<path class="graph-path graph-path-${metricTypeName}" d="${d}"/>`;
  }

  /**
   * Сгенерировать все пути графиков
   */
  renderGraphPaths(data: GraphRenderData): string {
    const { minMoment, maxKb } = data.processMinMax;
    const paths: string[] = [];
    
    for (const graph of data.graphs) {
      const path = this.renderGraphPath(graph.metricType, graph.points, minMoment, maxKb);
      if (path) paths.push(path);
    }
    
    return paths.join('\n      ');
  }

  /**
   * Сгенерировать подписи оси X (время)
   */
  renderXLabels(lines: GridLine[], transform: GraphTransform): string {
    const { containerHeight, bottomLabelSpace } = this.config;
    const frameColor = this.getFrameColor();
    const y = containerHeight - bottomLabelSpace;

    return lines
      .map((line) => {
        const x = this.toX(line.positionInDataUnits, transform);
        return /*svg*/`<text class="grid-label-x" x="${x}" y="${y}" text-anchor="middle" dominant-baseline="hanging" fill="${frameColor}">${line.label}</text>`;
      })
      .join('\n  ');
  }

  /**
   * Сгенерировать подписи оси Y (память)
   */
  renderYLabels(lines: GridLine[], data: GraphRenderData, transform: GraphTransform): string {
    const frameColor = this.getFrameColor();
    const maxKb = data.processMinMax.maxKb;

    return lines
      .map((line) => {
        // Восстанавливаем kb из positionInDataUnits
        const kb = maxKb - line.positionInDataUnits;
        const y = this.toY(kb, transform);
        return /*svg*/`<text class="grid-label-y" x="0" y="${y}" text-anchor="start" dominant-baseline="middle" fill="${frameColor}">${line.label}</text>`;
      })
      .join('\n  ');
  }

  /**
   * Сгенерировать полный SVG как строку
   */
  renderToString(data: GraphRenderData): string {
    const { containerWidth, containerHeight } = this.config;
    const viewBox = `0 0 ${containerWidth} ${containerHeight}`;

    const transform = this.getTransform(data);
    const verticalLines = this.getVerticalGridLines(data);
    const horizontalLines = this.getHorizontalGridLines(data);

    const styles = this.renderStyles();
    const frame = this.renderFrame(transform);
    const vGridLines = this.renderVerticalGridLines(verticalLines, transform.dataHeight);
    const hGridLines = this.renderHorizontalGridLines(horizontalLines, transform.dataWidth);
    const graphPaths = this.renderGraphPaths(data);
    const xLabels = this.renderXLabels(verticalLines, transform);
    const yLabels = this.renderYLabels(horizontalLines, data, transform);

    return /*svg*/`<svg class="graph-plot" viewBox="${viewBox}" preserveAspectRatio="none">
  ${styles}
  <!-- Группа трансформаций для графика -->
  <g transform="translate(${transform.translateX}, ${transform.translateY})">
    <g transform="scale(${transform.scaleX}, ${transform.scaleY})">
      <!-- Рамка графика -->
      ${frame}
      <!-- Вспомогательные вертикальные линии -->
      ${vGridLines}
      <!-- Вспомогательные горизонтальные линии -->
      ${hGridLines}
      <!-- Графики данных -->
      ${graphPaths}
    </g>
  </g>
  <!-- Подписи абсциссы -->
  ${xLabels}
  <!-- Подписи ординаты -->
  ${yLabels}
</svg>`;
  }

  /**
   * Очистить кэш
   */
  clearCache(): void {
    this.cachedVerticalLines = [];
    this.cachedVerticalMinTime = null;
    this.cachedVerticalMaxTime = null;
    this.cachedVerticalInterval = null;
    this.cachedHorizontalLines = [];
    this.cachedHorizontalMaxKb = null;
    this.cachedHorizontalInterval = null;
  }
}
