<svg
  class="graph-plot"
  bind:this={svgElement}
  {viewBox}
  preserveAspectRatio="none"
>
  {@html dynamicStyles}
  {#if graphs && processMinMax}
    <!-- Группа трансформаций для графика -->
    <g transform={`translate(${graphTranslateX}, ${graphTranslateY})`}>
      <g transform={`scale(${graphScaleX}, ${graphScaleY})`}>
        <!-- Рамка графика -->
        <rect
          class="graph-frame"
          x="0"
          y="0"
          width={dataWidth}
          height={dataHeight}
        ></rect>
        <!-- Вспомогательные вертикальные линии -->
        <path
          class="grid-lines"
          d={gridVerticalLines
            .map(
              (line) =>
                `M ${line.xInDataUnits},0 L ${line.xInDataUnits},${dataHeight}`,
            )
            .join(" ")}
        ></path>
        <!-- Вспомогательные горизонтальные линии -->
        <path
          class="grid-lines"
          d={gridHorizontalLines
            .map(
              (line) =>
                `M 0,${line.yInDataUnits} L ${dataWidth},${line.yInDataUnits}`,
            )
            .join(" ")}
        ></path>
        <!-- Графики данных -->
        {#each graphs as graph (graph.metricType)}
          <path
            class="graph-path graph-path-{MetricType[graph.metricType]}"
            d={graph.points
              .map((point: GraphPoint, i: number) => {
                const minTime = processMinMax!.minMoment.getTime();
                const maxBytes = Number(processMinMax!.maxBytes);
                // Преобразуем в единицы данных: десятые секунды и килобайты
                const x = (point.moment.getTime() - minTime) / 100;
                const y = (maxBytes - Number(point.bytes)) / 1024;
                return `${i === 0 ? "M" : "L"} ${x},${y}`;
              })
              .join(" ")}
          ></path>
        {/each}
      </g>
    </g>
    <!-- Подписи абсциссы (вне групп трансформаций) -->
    {#each gridVerticalLines as line}
      <text
        class="grid-label-x"
        x={toX(line.xInDataUnits)}
        y={containerHeight - 25}
        text-anchor="middle"
        dominant-baseline="hanging"
        fill={frameColor}
      >
        {line.label}
      </text>
    {/each}
    <!-- Подписи ординаты (вне групп трансформаций) -->
    {#each gridHorizontalLines as line}
      <text
        class="grid-label-y"
        x={30}
        y={toY(line.yInDataUnits)}
        text-anchor="end"
        dominant-baseline="middle"
        fill={frameColor}
      >
        {line.label}
      </text>
    {/each}
  {/if}
</svg>

<script lang="ts">
  import { type GraphPoint, graphStore } from "$lib/GraphStore";
  import { MetricType } from "$lib/generated/proto/protocol";
  import { getContext } from "svelte";
  import { graphMetaMap } from "$lib/GraphMeta";
  let { pid }: { pid: bigint } = $props();
  let svgElement: SVGElement | null = $state(null);
  let containerWidth = $state(1);
  let containerHeight = $state(1);

  const getGraphVersion = getContext<() => number>("graphVersion")!;
  let graphVersion = $derived(getGraphVersion());
  let graphs = $derived.by(() => {
    graphVersion; // читаем graphVersion для реактивности
    return graphStore.getGraphs(pid);
  });
  let processMinMax = $derived.by(() => {
    graphVersion; // читаем graphVersion для реактивности
    return graphStore.getProcessMinMax(pid);
  });

  // Отслеживание размеров SVG контейнера
  $effect(() => {
    const element = svgElement;
    if (!element || typeof window === "undefined") return;
    const updateSizes = () => {
      const rect = element.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        containerWidth = rect.width;
        containerHeight = rect.height;
      }
    };
    updateSizes();
    const resizeObserver = new ResizeObserver(updateSizes);
    resizeObserver.observe(element);
    return () => resizeObserver.disconnect();
  });

  // ViewBox равен размерам контейнера (1:1)
  let viewBox = $derived.by(() => {
    return `0 0 ${containerWidth} ${containerHeight}`;
  });

  // Константы для отступов
  const PADDING_PERCENT = 0.1; // 10% со всех сторон
  const BOTTOM_LABEL_SPACE = 25; // пикселей для подписей снизу
  const LEFT_LABEL_SPACE = 30; // пикселей для подписей слева

  // Размеры области графика (после вычитания отступов)
  let graphAreaWidth = $derived.by(() => {
    const padding = containerWidth * PADDING_PERCENT;
    return containerWidth - padding * 2 - LEFT_LABEL_SPACE;
  });

  let graphAreaHeight = $derived.by(() => {
    const padding = containerHeight * PADDING_PERCENT;
    return containerHeight - padding * 2 - BOTTOM_LABEL_SPACE;
  });

  // Минимальный диапазон времени графика в миллисекундах (2 минуты)
  const MIN_TIME_RANGE = 120 * 1000;

  // Размеры данных графика в единицах данных
  let dataWidth = $derived.by(() => {
    if (!processMinMax) return 1;
    const minTime = processMinMax.minMoment.getTime();
    const maxTime = processMinMax.maxMoment.getTime();
    const timeRange = Math.max(maxTime - minTime || 1, MIN_TIME_RANGE);
    return timeRange / 100.0; // в десятых долях секунды
  });

  let dataHeight = $derived.by(() => {
    if (!processMinMax) return 1;
    const maxBytes = Number(processMinMax.maxBytes) || 1;
    return maxBytes / 1024.0; // в килобайтах
  });

  // Трансформации для графика
  let graphTranslateX = $derived.by(() => {
    const padding = containerWidth * PADDING_PERCENT;
    return padding + LEFT_LABEL_SPACE;
  });

  let graphTranslateY = $derived.by(() => {
    const padding = containerHeight * PADDING_PERCENT;
    return padding;
  });

  let graphScaleX = $derived.by(() => {
    if (dataWidth === 0) return 1;
    return graphAreaWidth / dataWidth;
  });

  let graphScaleY = $derived.by(() => {
    if (dataHeight === 0) return 1;
    return graphAreaHeight / dataHeight;
  });

  // Функции преобразования координат
  function toX(tenthsOfSecond: number): number {
    return graphTranslateX + tenthsOfSecond * graphScaleX;
  }

  function toY(kilobytes: number): number {
    // Инвертируем Y: kilobytes=0 -> y=низ, kilobytes=max -> y=верх
    return graphTranslateY + (dataHeight - kilobytes) * graphScaleY;
  }

  let prefersDark = getContext<() => boolean>("prefersDark")!();
  let frameColor = prefersDark ? "white" : "black";

  // Возможные интервалы для вертикальных осей в миллисекундах
  const GRID_INTERVALS_MS = [
    30 * 1000, // 30 секунд
    60 * 1000, // 1 минута
    5 * 60 * 1000, // 5 минут
    10 * 60 * 1000, // 10 минут
    30 * 60 * 1000, // 30 минут
    60 * 60 * 1000, // 1 час
    2 * 60 * 60 * 1000, // 2 часа
    4 * 60 * 60 * 1000, // 4 часа
    8 * 60 * 60 * 1000, // 8 часов
    24 * 60 * 60 * 1000, // сутки
  ];

  // Возможные интервалы для горизонтальных осей в байтах
  const GRID_INTERVALS_BYTES = [
    1024 * 1024, // 1MB
    10 * 1024 * 1024, // 10MB
    100 * 1024 * 1024, // 100MB
    1024 * 1024 * 1024, // 1GB
    10 * 1024 * 1024 * 1024, // 10GB
    100 * 1024 * 1024 * 1024, // 100GB
  ];

  // Функция форматирования времени для меток (относительно начала графика)
  function formatTimeLabel(
    tick: number,
    minTime: number,
    interval: number,
  ): string {
    const relativeMs = tick - minTime;
    const seconds = Math.floor(relativeMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (interval < 60 * 1000) {
      return `${seconds}s`;
    } else if (interval < 60 * 60 * 1000) {
      return `${minutes}m`;
    } else {
      return `${hours}h`;
    }
  }

  // Функция форматирования байт для меток
  function formatBytesLabel(bytes: number): string {
    const kb = 1024;
    const mb = 1024 * 1024;
    const gb = 1024 * 1024 * 1024;

    if (bytes >= gb) {
      const gbValue = bytes / gb;
      return `${gbValue.toFixed(gbValue >= 10 ? 0 : 1)}GB`;
    } else if (bytes >= mb) {
      const mbValue = bytes / mb;
      return `${mbValue.toFixed(mbValue >= 10 ? 0 : 1)}MB`;
    } else {
      const kbValue = bytes / kb;
      return `${Math.round(kbValue)}KB`;
    }
  }

  // Мемоизация для gridVerticalLines
  let cachedGridLines: Array<{
    xInDataUnits: number;
    tick: number;
    label: string;
  }> = [];
  let cachedMinTime: number | null = null;
  let cachedMaxTime: number | null = null;
  let cachedInterval: number | null = null;

  // Вычисление позиций вертикальных осей
  let gridVerticalLines = $derived.by(() => {
    if (!processMinMax) {
      cachedGridLines = [];
      cachedMinTime = null;
      cachedMaxTime = null;
      return [];
    }
    const minTime = processMinMax.minMoment.getTime();
    const maxTime = processMinMax.maxMoment.getTime();

    // Выбираем интервал: максимальное количество осей, но не более 10
    let selectedInterval = GRID_INTERVALS_MS[GRID_INTERVALS_MS.length - 1];
    for (const interval of GRID_INTERVALS_MS) {
      const firstTick = Math.ceil(minTime / interval) * interval;
      if (firstTick > maxTime) continue;

      let count = 0;
      for (let tick = firstTick; tick <= maxTime; tick += interval) {
        count++;
      }

      if (count <= 10) {
        selectedInterval = interval;
        break;
      }
    }

    // Если временной диапазон и интервал не изменились, возвращаем кэшированный результат
    if (
      cachedMinTime === minTime &&
      cachedMaxTime === maxTime &&
      cachedInterval === selectedInterval &&
      cachedGridLines.length > 0
    ) {
      return cachedGridLines.map((line) => ({
        ...line,
        xInDataUnits: (line.tick - minTime) / 100.0,
      }));
    }

    // Генерируем позиции осей
    const firstTick = Math.ceil(minTime / selectedInterval) * selectedInterval;
    const lines: Array<{
      xInDataUnits: number;
      tick: number;
      label: string;
    }> = [];

    for (let tick = firstTick; tick < maxTime; tick += selectedInterval) {
      const xInDataUnits = (tick - minTime) / 100.0; // в десятых долях секунды
      const label = formatTimeLabel(tick, minTime, selectedInterval);
      lines.push({ xInDataUnits, tick, label });
    }

    // Кэшируем результат
    cachedGridLines = lines;
    cachedMinTime = minTime;
    cachedMaxTime = maxTime;
    cachedInterval = selectedInterval;

    return lines;
  });

  // Мемоизация для gridHorizontalLines
  let cachedHorizontalLines: Array<{
    yInDataUnits: number;
    bytes: number;
    label: string;
  }> = [];
  let cachedMinBytes: number | null = null;
  let cachedMaxBytes: number | null = null;
  let cachedBytesInterval: number | null = null;

  // Вычисление позиций горизонтальных осей
  let gridHorizontalLines = $derived.by(() => {
    if (!processMinMax) {
      cachedHorizontalLines = [];
      cachedMinBytes = null;
      cachedMaxBytes = null;
      return [];
    }
    const maxBytes = Number(processMinMax.maxBytes);
    const minBytes = 0;

    // Выбираем интервал: максимальное количество осей, но не более 5
    let selectedInterval =
      GRID_INTERVALS_BYTES[GRID_INTERVALS_BYTES.length - 1];
    for (const interval of GRID_INTERVALS_BYTES) {
      const firstTick = Math.ceil(minBytes / interval) * interval;
      if (firstTick > maxBytes) continue;

      let count = 0;
      for (let tick = firstTick; tick <= maxBytes; tick += interval) {
        count++;
      }

      if (count <= 5) {
        selectedInterval = interval;
        break;
      }
    }

    // Если диапазон байт и интервал не изменились, возвращаем кэшированный результат
    if (
      cachedMinBytes === minBytes &&
      cachedMaxBytes === maxBytes &&
      cachedBytesInterval === selectedInterval &&
      cachedHorizontalLines.length > 0
    ) {
      return cachedHorizontalLines.map((line) => ({
        ...line,
        yInDataUnits: (maxBytes - line.bytes) / 1024.0,
      }));
    }

    // Генерируем позиции осей
    const firstTick = Math.ceil(minBytes / selectedInterval) * selectedInterval;
    const lines: Array<{
      yInDataUnits: number;
      bytes: number;
      label: string;
    }> = [];

    for (let tick = firstTick; tick <= maxBytes; tick += selectedInterval) {
      const yInDataUnits = (maxBytes - tick) / 1024.0; // в килобайтах, инвертировано
      const label = formatBytesLabel(tick);
      lines.push({ yInDataUnits, bytes: tick, label });
    }

    // Кэшируем результат
    cachedHorizontalLines = lines;
    cachedMinBytes = minBytes;
    cachedMaxBytes = maxBytes;
    cachedBytesInterval = selectedInterval;

    return lines;
  });

  let dynamicStyles = $derived.by(() => {
    const baseStyles = /*css*/ `
      .graph-plot {
        width: 100%;
        height: 100%;
        background-color: ${prefersDark ? "#202020" : "#f3f3f3"};
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
    const graphColor = prefersDark ? "color_dark" : "color_light";
    const metricStyles = Object.keys(graphMetaMap)
      .map((metricTypeKey) => {
        const metricType = Number(metricTypeKey) as MetricType;
        const metricTypeName = MetricType[metricType];
        const meta = graphMetaMap[metricType];
        return /*css*/ `
      .graph-path-${metricTypeName} {
        stroke: ${meta[graphColor]};
      }
    `;
      })
      .join("");
    return `<style>${baseStyles}${metricStyles}</style>`;
  });
</script>
