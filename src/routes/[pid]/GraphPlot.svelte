<svg
  class="graph-plot"
  bind:this={svgElement}
  {viewBox}
  preserveAspectRatio="none"
>
  {@html dynamicStyles}
  {#if graphs && processMinMax}
    <rect
      class="graph-frame"
      x={frameX}
      y={frameY}
      width={frameWidth}
      height={frameHeight}
    ></rect>
    <path
      class="grid-lines"
      d={gridVerticalLines
        .map(
          (line) => `M ${line.x},${frameY} L ${line.x},${frameY + frameHeight}`,
        )
        .join(" ")}
    ></path>
    <path
      class="grid-lines"
      d={gridHorizontalLines
        .map(
          (line) => `M ${frameX},${line.y} L ${frameX + frameWidth},${line.y}`,
        )
        .join(" ")}
    ></path>
    <g transform={`translate(0, ${labelY})`}>
      <g transform={`scale(1, ${textScaleY})`}>
        {#each gridVerticalLines as line}
          <text
            class="grid-label"
            x="0"
            y="0"
            text-anchor="middle"
            dominant-baseline="hanging"
            fill={frameColor}
            transform={`translate(${line.x}, 0)`}
          >
            {line.label}
          </text>
        {/each}
      </g>
    </g>
    <g transform={`translate(${frameX - leftLabelSpace}, 0)`}>
      <g transform={`scale(1, ${textScaleY})`}>
        {#each gridHorizontalLines as line}
          <text
            class="grid-label"
            x="0"
            y="0"
            text-anchor="end"
            dominant-baseline="middle"
            fill={frameColor}
            transform={`translate(0, ${line.y})`}
          >
            {line.label}
          </text>
        {/each}
      </g>
    </g>
    {#each graphs as graph (graph.metricType)}
      <path
        class="graph-path graph-path-{MetricType[graph.metricType]}"
        d={graph.points
          .map((point: GraphPoint, i: number) => {
            const minTime = processMinMax!.minMoment.getTime();
            const maxBytes = Number(processMinMax!.maxBytes);
            // Горизонтальная координата: вычитаем минимальное время, делим на 100 (десятые доли секунды)
            const x = (point.moment.getTime() - minTime) / 100;
            // Вертикальная координата: байты делим на 1024 (килобайты), инвертируем для SVG
            // Нам нужно: bytes=0 -> y=maxKB, bytes=maxBytes -> y=0
            const y = (maxBytes - Number(point.bytes)) / 1024;
            return `${i === 0 ? "M" : "L"} ${x},${y}`;
          })
          .join(" ")}
      ></path>
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
  let svgAspectRatio = $state(1);
  let containerWidth = $state(1);
  let containerHeight = $state(1);
  const getGraphVersion = getContext<() => number>("graphVersion")!;
  let graphVersion = $derived(getGraphVersion());
  let graphs = $derived.by(() => {
    // используем переменную graphVersion для side-effect,
    // чтобы перерисовать график при поступлении новых данных
    graphVersion; // читаем graphVersion для реактивности
    return graphStore.getGraphs(pid);
  });
  let processMinMax = $derived.by(() => {
    // используем переменную graphVersion для side-effect,
    // чтобы перерисовать график при поступлении новых данных
    graphVersion; // читаем graphVersion для реактивности
    return graphStore.getProcessMinMax(pid);
  });

  // Минимальный диапазон времени графика в миллисекундах (2 минуты).
  const MIN_TIME_RANGE = 120 * 1000;

  // Исходные размеры области графика (без отступов)
  let frameWidth = $derived.by(() => {
    if (!processMinMax) return 1;
    const minTime = processMinMax.minMoment.getTime();
    const maxTime = processMinMax.maxMoment.getTime();
    const timeRange = Math.max(maxTime - minTime || 1, MIN_TIME_RANGE);
    return Math.round(timeRange / 100.0);
  });

  let frameHeight = $derived.by(() => {
    if (!processMinMax) return 1;
    const maxBytes = Number(processMinMax.maxBytes) || 1;
    return Math.round(maxBytes / 1024.0);
  });

  // Координаты рамки (исходные координаты графика)
  let frameX = 0;
  let frameY = 0;

  // Отступ для меток
  let labelOffset = 20000;

  // Координата Y для меток (ниже рамки)
  let labelY = $derived.by(() => frameY + frameHeight + labelOffset);

  // Отслеживание размеров SVG для вычисления искажения
  $effect(() => {
    const element = svgElement;
    if (!element || typeof window === "undefined") return;
    const updateSizes = () => {
      const rect = element.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        containerWidth = rect.width;
        containerHeight = rect.height;
        svgAspectRatio = rect.height / rect.width;
      }
    };
    updateSizes();
    const resizeObserver = new ResizeObserver(updateSizes);
    resizeObserver.observe(element);
    return () => resizeObserver.disconnect();
  });

  // Коэффициент масштабирования для компенсации искажения текста
  // из-за preserveAspectRatio="none"
  // При preserveAspectRatio="none" SVG растягивается на контейнер:
  // - Горизонтальный масштаб: containerWidth / viewBoxWidth
  // - Вертикальный масштаб: containerHeight / viewBoxHeight
  // Чтобы текст не сплющивался, нужно компенсировать разницу масштабов:
  // scaleY = (containerWidth / viewBoxWidth) / (containerHeight / viewBoxHeight)
  //        = (containerWidth * viewBoxHeight) / (containerHeight * viewBoxWidth)
  let textScaleY = $derived.by(() => {
    if (!processMinMax || containerWidth === 0 || containerHeight === 0)
      return 1;
    const width = frameWidth;
    const height = frameHeight;
    const viewBoxWidth = width * 1.2;
    const labelSpace = 50;
    const viewBoxHeight = height * 1.2 + labelSpace;
    // Используем абсолютные размеры для точного расчета
    return (containerWidth * viewBoxHeight) / (containerHeight * viewBoxWidth);
  });

  // Слева отступ для горизонтальных меток (примерно 10000 единиц)
  const leftLabelSpace = 100;

  // viewBox увеличен на 20% с отступами по 10% с каждой стороны
  // Снизу дополнительный отступ для меток, слева для горизонтальных меток
  let viewBox = $derived.by(() => {
    if (!processMinMax) {
      return "0 0 1 1";
    }
    const width = frameWidth;
    const height = frameHeight;
    // Увеличиваем размеры на 20% и смещаем на -10% для отступов
    const viewBoxX = Math.round(-width * 0.1 - leftLabelSpace);
    const viewBoxY = Math.round(-height * 0.1);
    const viewBoxWidth = Math.round(width * 1.2 + leftLabelSpace);
    // Добавляем снизу дополнительное место для меток (примерно 50 единиц = 50KB в системе координат)
    const labelSpace = 50;
    const viewBoxHeight = Math.round(height * 1.2 + labelSpace);
    return `${viewBoxX} ${viewBoxY} ${viewBoxWidth} ${viewBoxHeight}`;
  });
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
    // Вычисляем относительное время от начала графика
    const relativeMs = tick - minTime;
    const seconds = Math.floor(relativeMs / 1000);
    const minutes = Math.floor(seconds / 60);
    const hours = Math.floor(minutes / 60);

    if (interval < 60 * 1000) {
      // Интервалы меньше минуты - показываем секунды
      return `${seconds}s`;
    } else if (interval < 60 * 60 * 1000) {
      // Интервалы меньше часа - показываем минуты
      return `${minutes}m`;
    } else {
      // Интервалы час и больше - показываем часы
      return `${hours}h`;
    }
  }

  // Функция форматирования байт для меток
  function formatBytesLabel(bytes: number): string {
    const kb = 1024;
    const mb = 1024 * 1024;
    const gb = 1024 * 1024 * 1024;

    if (bytes >= gb) {
      // Гигабайты
      const gbValue = bytes / gb;
      return `${gbValue.toFixed(gbValue >= 10 ? 0 : 1)}GB`;
    } else if (bytes >= mb) {
      // Мегабайты
      const mbValue = bytes / mb;
      return `${mbValue.toFixed(mbValue >= 10 ? 0 : 1)}MB`;
    } else {
      // Килобайты
      const kbValue = bytes / kb;
      return `${Math.round(kbValue)}KB`;
    }
  }

  // Мемоизация для gridVerticalLines
  let cachedGridLines: Array<{ x: number; tick: number; label: string }> = [];
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
      // Вычисляем количество осей для данного интервала
      // Находим первое время, кратное интервалу, которое >= minTime
      const firstTick = Math.ceil(minTime / interval) * interval;
      if (firstTick > maxTime) continue; // интервал слишком большой

      // Считаем количество осей
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
      // Обновляем только координаты x, если minTime изменился (но диапазон тот же)
      return cachedGridLines.map((line) => ({
        ...line,
        x: Math.round((line.tick - minTime) / 100.0),
      }));
    }

    // Генерируем позиции осей
    const firstTick = Math.ceil(minTime / selectedInterval) * selectedInterval;
    const lines: Array<{ x: number; tick: number; label: string }> = [];

    for (let tick = firstTick; tick < maxTime; tick += selectedInterval) {
      // Координата x: вычитаем минимальное время, делим на 100 (десятые доли секунды)
      const x = Math.round((tick - minTime) / 100.0);
      const label = formatTimeLabel(tick, minTime, selectedInterval);
      lines.push({ x, tick, label });
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
    y: number;
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
    const minBytes = 0; // Всегда начинаем с 0

    // Выбираем интервал: максимальное количество осей, но не более 5
    let selectedInterval =
      GRID_INTERVALS_BYTES[GRID_INTERVALS_BYTES.length - 1];
    for (const interval of GRID_INTERVALS_BYTES) {
      // Вычисляем количество осей для данного интервала
      // Находим первое значение, кратное интервалу, которое >= minBytes
      const firstTick = Math.ceil(minBytes / interval) * interval;
      if (firstTick > maxBytes) continue; // интервал слишком большой

      // Считаем количество осей
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
      // Обновляем только координаты y, если maxBytes изменился (но диапазон тот же)
      return cachedHorizontalLines.map((line) => ({
        ...line,
        y: Math.round((maxBytes - line.bytes) / 1024.0),
      }));
    }

    // Генерируем позиции осей
    const firstTick = Math.ceil(minBytes / selectedInterval) * selectedInterval;
    const lines: Array<{ y: number; bytes: number; label: string }> = [];

    for (let tick = firstTick; tick <= maxBytes; tick += selectedInterval) {
      // Координата y: инвертируем (bytes=0 -> y=maxKB, bytes=maxBytes -> y=0)
      const y = Math.round((maxBytes - tick) / 1024.0);
      const label = formatBytesLabel(tick);
      lines.push({ y, bytes: tick, label });
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
      .grid-label {
        fill: ${frameColor};
        font-size: 250px;
        opacity: 1;
        font-family: sans-serif;
        font-weight: normal;
        pointer-events: none;
        transform-origin: center top;
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
