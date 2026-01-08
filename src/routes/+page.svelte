<script lang="ts">
  import { invoke } from "@tauri-apps/api/core";
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
  let followingPids = $state<bigint[]>([]);

</script>


<div class="two-column-layout">
  
  <MetricList allMetricTypes={allMetricTypes} visibleMetrics={visibleMetrics} />
  <ProcessList availableJvmProcesses={availableJvmProcesses} followingPids={followingPids} />

</div>
