{#if !process}
  <h2>Process {pidStr} not found</h2>
{:else}
  <h2>{pidStr} {process.display_name}</h2>
  {#if hasGraph}
    <div class="graph-container">
      <GraphPlot pid={pid!} />
    </div>
  {/if}
{/if}

<script lang="ts">
  import { page } from "$app/state";
  import { getContext } from "svelte";
  import type { ProcInfo } from "$lib/ProcHandle";
  import GraphPlot from "./GraphPlot.svelte";
  let pidStr = $derived(page.params.pid);
  let pid = $derived(pidStr ? BigInt(pidStr) : null);
  const getAvailableJvmProcesses = getContext<() => Map<bigint, ProcInfo>>(
    "availableJvmProcesses",
  )!;
  import { graphStore } from "$lib/GraphStore";
  let process = $derived.by(() =>
    pid ? getAvailableJvmProcesses().get(pid) : undefined,
  );
  // Процесс может быть среди отслеживаемых, но ещё не прислал данных.
  const hasGraph = $derived(
    pid ? graphStore.hasGraphDataForProcess(pid) : false,
  );
</script>

<style>
  .graph-container {
    width: 100%;
    flex: 1;
    min-height: 0; /* важно для flexbox с overflow */
    overflow: hidden;
    display: flex; /* чтобы GraphPlot растягивался */
  }
</style>
