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
  let dynamicStyles = $derived.by(() => {
    const baseStyles = `
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
    `;
    const graphColor = prefersDark ? "color_dark" : "color_light";
    const metricStyles = Object.keys(graphMetaMap)
      .map((metricTypeKey) => {
        const metricType = Number(metricTypeKey) as MetricType;
        const metricTypeName = MetricType[metricType];
        const meta = graphMetaMap[metricType];
        return `
      .graph-path-${metricTypeName} {
        stroke: ${meta[graphColor]};
      }
    `;
      })
      .join("");
    return `<style>${baseStyles}${metricStyles}</style>`;
  });
</script>

<style>
  .graph-plot {
    width: 100%;
    height: 100%;
    display: block;
    max-height: 100%;
    overflow: hidden;
  }
</style>
