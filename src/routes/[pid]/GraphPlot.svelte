<svg class="graph-plot" width="100%" height="100%">
  {#if graphs}
    {#each graphs as graph (graph.metricType)}
      <path
        class="graph-path"
        d={graph.points
          .map(
            (point: GraphPoint, i: number) =>
              `${i === 0 ? "M" : "L"} ${point.moment.getTime()},${point.bytes}`,
          )
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
</script>

<style>
  .graph-plot {
    width: 100%;
    height: 100%;
  }
  .graph-path {
    fill: none;
    stroke: white;
    stroke-width: 1;
  }
</style>
