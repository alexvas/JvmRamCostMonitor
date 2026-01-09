<script lang="ts">
  import { page } from '$app/stores';
  import { getContext } from 'svelte';
  import { ProcInfo, GraphPoint, MetricType } from '$lib/generated/proto/protocol';
  let pidStr = $derived($page.params.pid);
  let pid = $derived(pidStr ? BigInt(pidStr) : null);
  const getAvailableJvmProcesses = getContext<() => Map<bigint, ProcInfo>>('availableJvmProcesses')!;
  let availableJvmProcesses = $derived(getAvailableJvmProcesses());
  let process = $derived(pid ? availableJvmProcesses.get(pid) : undefined);
  const getGraphPointQueues = getContext<() => Map<bigint, Map<MetricType, GraphPoint[]>>>('graphPointQueues')!;
  let graphPointQueues = $derived(getGraphPointQueues());
  let metricType2Queue = $derived(pid ? Array.from(graphPointQueues.get(pid)?.entries() || []) : []);
</script>

{#if !process}
  <h2>Process {pidStr} not found</h2>
{:else}
  <h2>{pidStr} {process.display_name}</h2>
  {#each metricType2Queue as [metricType, queue] (metricType)}
    <h3>{metricType}</h3>
    <ul>
      {#each queue as point (point.moment)}
        <li>{point.bytes}</li>
      {/each}
    </ul>
  {/each}
{/if}