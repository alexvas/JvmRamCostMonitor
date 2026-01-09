{#if !process}
  <h2>Process {pidStr} not found</h2>
{:else}
  <h2>{pidStr} {process.display_name}</h2>
  {#if pid}
    <div class="graph-container">
      <GraphPlot {pid} />
    </div>
  {/if}
{/if}

<script lang="ts">
  import { page } from "$app/state";
  import { getContext } from "svelte";
  import { ProcInfo } from "$lib/generated/proto/protocol";
  import GraphPlot from "./GraphPlot.svelte";
  let pidStr = $derived(page.params.pid);
  let pid = $derived(pidStr ? BigInt(pidStr) : null);
  const getAvailableJvmProcesses = getContext<() => Map<bigint, ProcInfo>>(
    "availableJvmProcesses",
  )!;
  let availableJvmProcesses = $derived(getAvailableJvmProcesses());
  let process = $derived(pid ? availableJvmProcesses.get(pid) : undefined);
</script>

<style>
  .graph-container {
    width: 100%;
    height: 500px;
    overflow: hidden;
  }
</style>
