<svg
  class="graph-plot"
  bind:this={svgElement}
  {viewBox}
  preserveAspectRatio="none"
>
  {@html dynamicStyles}
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
      {#each graphs as graph}
        <path
          class="graph-path graph-path-{MetricType[graph.metricType]}"
          d={graph.points
            .map((point: GraphPoint, i: number) => {
              const minTime = processMinMax.minMoment;
              const maxBytes = processMinMax.maxBytes;
              // Преобразуем в единицы данных: десятые секунды и килобайты
              const x = point.moment - minTime;
              const y = Number(maxBytes - point.bytes) / 1024;
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
      y={containerHeight - BOTTOM_LABEL_SPACE}
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
      x="0"
      y={toY(line.bytes / 1024)}
      text-anchor="start"
      dominant-baseline="middle"
      fill={frameColor}
    >
      {line.label}
    </text>
  {/each}
</svg>

<script lang="ts">
  import { type GraphPoint, graphStore, MetricType } from "$lib/GraphStore";
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
  })!;
  let processMinMax = $derived.by(() => {
    graphVersion; // читаем graphVersion для реактивности
    return graphStore.getProcessMinMax(pid);
  })!;

  // Отслеживание размеров SVG контейнера
  $effect(() => {
    const element = svgElement;
    if (!element || typeof window === "undefined") return;

    let pendingUpdateCounter = 0;
    const DEBOUNCE_DELAY_MS = 16; // примерно один кадр

    const updateSizes = () => {
      const rect = element.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        containerWidth = rect.width;
        containerHeight = rect.height;
      }
    };

    // Первоначальное обновление через debounce механизм
    pendingUpdateCounter++;
    setTimeout(() => {
      pendingUpdateCounter--;
      if (pendingUpdateCounter <= 0) {
        updateSizes();
      }
    }, DEBOUNCE_DELAY_MS);

    const resizeObserver = new ResizeObserver(() => {
      pendingUpdateCounter++; // инкрементируем счетчик при каждом событии
      setTimeout(() => {
        pendingUpdateCounter--; // декрементируем при выполнении
        if (pendingUpdateCounter <= 0) {
          // Выполняем реальную логику только когда счетчик достиг нуля
          // (т.е. все запланированные обработчики выполнились)
          updateSizes();
        }
        // Если счетчик > 0, ничего не делаем - есть еще запланированные обработчики
      }, DEBOUNCE_DELAY_MS);
    });

    resizeObserver.observe(element);

    return () => {
      resizeObserver.disconnect();
    };
  });

  // ViewBox равен размерам контейнера (1:1)
  let viewBox = $derived.by(() => {
    return `0 0 ${containerWidth} ${containerHeight}`;
  });

  // Константы для отступов
  const PADDING_PERCENT = 0.05; // 5% со всех сторон
  const BOTTOM_LABEL_SPACE = 25; // пикселей для подписей снизу
  const LEFT_LABEL_SPACE = 20; // пикселей для подписей слева

  // Размеры области графика (после вычитания отступов)
  let graphAreaWidth = $derived.by(() => {
    const padding = containerWidth * PADDING_PERCENT;
    return containerWidth - padding * 2 - LEFT_LABEL_SPACE;
  });

  let graphAreaHeight = $derived.by(() => {
    const padding = containerHeight * PADDING_PERCENT;
    return containerHeight - padding * 2 - BOTTOM_LABEL_SPACE;
  });

  // Минимальный диапазон времени графика в десятых долях секунды (2 минуты)
  const MIN_TIME_RANGE = 2 * 60 * 10;

  // Размеры данных графика в единицах данных
  let dataWidth = $derived.by(() => {
    if (!processMinMax) return 1;
    const minTime = processMinMax.minMoment;
    const maxTime = processMinMax.maxMoment;
    const timeRange = Math.max(maxTime - minTime, MIN_TIME_RANGE);
    return timeRange; // в десятых долях секунды
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

  // Функция для подписей ординаты: прямое преобразование без инверсии
  // kilobytes=0 -> низ графика (большая координата Y)
  // kilobytes=max -> верх графика (маленькая координата Y)
  function toY(kilobytes: number): number {
    return graphTranslateY + graphAreaHeight - kilobytes * graphScaleY;
  }

  let prefersDark = getContext<() => boolean>("prefersDark")!();
  let frameColor = prefersDark ? "white" : "black";

  // Возможные интервалы для вертикальных осей в десятых долях секунды
  const GRID_INTERVALS_TENTH_OF_SECOND = [
    30 * 10, // 30 секунд
    60 * 10, // 1 минута
    5 * 60 * 10, // 5 минут
    10 * 60 * 10, // 10 минут
    30 * 60 * 10, // 30 минут
    60 * 60 * 10, // 1 час
    2 * 60 * 60 * 10, // 2 часа
    4 * 60 * 60 * 10, // 4 часа
    8 * 60 * 60 * 10, // 8 часов
    24 * 60 * 60 * 10, // сутки
  ];

  // Возможные интервалы для горизонтальных осей в байтах
  const GRID_INTERVALS_BYTES = [
    10 * 1024 * 1024, // 10MB
    50 * 1024 * 1024, // 50MB
    100 * 1024 * 1024, // 100MB
    300 * 1024 * 1024, // 300MB
    500 * 1024 * 1024, // 500MB
    800 * 1024 * 1024, // 800MB
    1024 * 1024 * 1024, // 1GB
    3 * 1024 * 1024 * 1024, // 3GB
    5 * 1024 * 1024 * 1024, // 5GB
    8 * 1024 * 1024 * 1024, // 8GB
    10 * 1024 * 1024 * 1024, // 10GB
    30 * 1024 * 1024 * 1024, // 30GB
    50 * 1024 * 1024 * 1024, // 50GB
    80 * 1024 * 1024 * 1024, // 80GB
    100 * 1024 * 1024 * 1024, // 100GB
    300 * 1024 * 1024 * 1024, // 300GB
    500 * 1024 * 1024 * 1024, // 500GB
    800 * 1024 * 1024 * 1024, // 800GB
    1024 * 1024 * 1024 * 1024, // 1TB
  ];

  // Функция форматирования времени для меток (относительно начала графика)
  function formatTimeLabel(
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
    } else if (bytes >= kb) {
      const kbValue = bytes / kb;
      return `${Math.round(kbValue)}KB`;
    } else {
      return `${bytes}B`;
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
    const minTime = processMinMax.minMoment;
    const maxTime = processMinMax.maxMoment;
    const timeRange = maxTime - minTime;

    // Выбираем интервал: максимальное количество осей, но не более 10
    let selectedInterval =
      GRID_INTERVALS_TENTH_OF_SECOND[GRID_INTERVALS_TENTH_OF_SECOND.length - 1];
    for (const interval of GRID_INTERVALS_TENTH_OF_SECOND) {
      // деление нацело
      let count = timeRange / interval;

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
        xInDataUnits: line.tick - minTime,
      }));
    }

    // Генерируем позиции осей
    const firstTick = minTime + selectedInterval;
    const lines: Array<{
      xInDataUnits: number;
      tick: number;
      label: string;
    }> = [];

    for (let tick = firstTick; tick < maxTime; tick += selectedInterval) {
      const xInDataUnits = tick - minTime; // в десятых долях секунды
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
      // деление нацело
      let count = (maxBytes - minBytes) / interval;
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
    const firstTick = selectedInterval;
    const lines: Array<{
      yInDataUnits: number;
      bytes: number;
      label: string;
    }> = [];

    for (let tick = firstTick; tick < maxBytes; tick += selectedInterval) {
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
