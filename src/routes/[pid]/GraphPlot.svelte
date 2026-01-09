<svg class="graph-plot" {viewBox} preserveAspectRatio="none">
  {@html dynamicStyles}
  {#if graphs && processMinMax}
    <rect
      class="graph-frame"
      x={frameX}
      y={frameY}
      width={frameWidth}
      height={frameHeight}
    ></rect>
    {#if gridVerticalLines.length > 0}
      <path
        class="grid-lines"
        d={gridVerticalLines
          .map(
            (line) =>
              `M ${line.x},${frameY} L ${line.x},${frameY + frameHeight}`,
          )
          .join(" ")}
      ></path>
    {/if}
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

  // viewBox увеличен на 20% с отступами по 10% с каждой стороны
  let viewBox = $derived.by(() => {
    if (!processMinMax) {
      return "0 0 1 1";
    }
    const width = frameWidth;
    const height = frameHeight;
    // Увеличиваем размеры на 20% и смещаем на -10% для отступов
    const viewBoxX = Math.round(-width * 0.1);
    const viewBoxY = Math.round(-height * 0.1);
    const viewBoxWidth = Math.round(width * 1.2);
    const viewBoxHeight = Math.round(height * 1.2);
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

  // Мемоизация для gridVerticalLines
  let cachedGridLines: Array<{ x: number; tick: number }> = [];
  let cachedMinTime: number | null = null;
  let cachedMaxTime: number | null = null;

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

    // Если временной диапазон не изменился, возвращаем кэшированный результат
    if (
      cachedMinTime === minTime &&
      cachedMaxTime === maxTime &&
      cachedGridLines.length > 0
    ) {
      return cachedGridLines;
    }

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

    // Генерируем позиции осей
    const firstTick = Math.ceil(minTime / selectedInterval) * selectedInterval;
    const lines: Array<{ x: number; tick: number }> = [];

    for (let tick = firstTick; tick < maxTime; tick += selectedInterval) {
      // Координата x: вычитаем минимальное время, делим на 100 (десятые доли секунды)
      const x = Math.round((tick - minTime) / 100.0);
      lines.push({ x, tick });
    }

    // Кэшируем результат
    cachedGridLines = lines;
    cachedMinTime = minTime;
    cachedMaxTime = maxTime;

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
