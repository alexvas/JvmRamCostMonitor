<script lang="ts">
  import { page } from '$app/stores';
  import { getContext } from 'svelte';
  import { ProcInfo } from '$lib/generated/proto/protocol';
  let pidStr = $derived($page.params.pid);
  let pid = $derived(pidStr ? BigInt(pidStr) : null);
  const getAvailableJvmProcesses = getContext<() => Map<bigint, ProcInfo>>('availableJvmProcesses')!;
  let availableJvmProcesses = $derived(getAvailableJvmProcesses());
  let process = $derived(pid ? availableJvmProcesses.get(pid) : undefined);
</script>

{#if !process}
  <h2>Process {pidStr} not found</h2>
{:else}
  <h2>{pidStr} {process.display_name}</h2>
{/if}