<h2>Setup</h2>
<div class="two-column-layout">
  <ProcessList />
  <MetricList {allMetricTypes} {visibleMetrics} />
</div>

<script lang="ts">
  import { MetricType } from "$lib/GraphStore";
  import MetricList from "$lib/MetricList.svelte";
  import ProcessList from "./ProcessList.svelte";

  let allMetricTypes = $state<MetricType[]>();
  let visibleMetrics = $state<MetricType[]>();
</script>
