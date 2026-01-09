<svg class="graph-plot" {viewBox} preserveAspectRatio="none">
  {#if graphs && processMinMax}
    {#each graphs as graph (graph.metricType)}
      <path
        class="graph-path"
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
  import { getContext } from "svelte";
  let { pid }: { pid: bigint } = $props();
  let graphVersion = getContext<() => number>("graphVersion")!();
  let graphs = $derived(
    // используем переменную graphVersion для side-effect,
    // чтобы перерисовать график при поступлении новых данных
    graphStore.getGraphs(BigInt(graphVersion * 0) + pid),
  );
  let processMinMax = $derived(
    // используем переменную graphVersion для side-effect,
    // чтобы перерисовать график при поступлении новых данных
    graphStore.getProcessMinMax(BigInt(graphVersion * 0) + pid),
  );
  let viewBox = $derived.by(() => {
    if (!processMinMax) {
      return "0 0 1 1";
    }
    const minTime = processMinMax.minMoment.getTime();
    const maxTime = processMinMax.maxMoment.getTime();
    const timeRange = maxTime - minTime || 1;
    const maxBytes = Number(processMinMax.maxBytes) || 1;
    // viewBox: x, y, width, height
    // x = 0, y = 0 (нормализованные координаты)
    // width = диапазон времени в десятых долях секунды (делим на 100)
    // height = максимальное значение в килобайтах (делим на 1024)
    return `0 0 ${timeRange / 100} ${maxBytes / 1024}`;
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
  .graph-path {
    fill: none;
    stroke: red;
    stroke-width: 0.7;
    vector-effect: non-scaling-stroke;
  }
</style>
