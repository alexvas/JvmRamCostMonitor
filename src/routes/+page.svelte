<script lang="ts">
  import { invoke } from "@tauri-apps/api/core";
  import { getContext } from 'svelte';
  import { 
    MetricType, 
    SetVisibleRequest, 
    SetInvisibleRequest, 
    Pid,
    PidList, 
    ProcInfo, 
    ApplicableMetricsResponse,
  } from "../generated/proto/protocol";
  import MetricList from "./MetricList.svelte";
  import ProcessList from "./ProcessList.svelte";

  let allMetricTypes = $state<MetricType[]>();
  let visibleMetrics = $state<MetricType[]>();
 
  let availableJvmProcesses = $state<ProcInfo[]>([]);

</script>

<h2>Settings</h2>
<div class="two-column-layout">
  
  <MetricList allMetricTypes={allMetricTypes} visibleMetrics={visibleMetrics} />
  <ProcessList availableJvmProcesses={availableJvmProcesses} />

</div>
