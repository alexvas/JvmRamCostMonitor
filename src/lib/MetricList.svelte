<div class="column card">
  <h3 class="card-title">Metrics</h3>
  <div class="checkbox-group">
    {#each allMetricTypes as mt}
      <label class="checkbox-label">
        <input
          type="checkbox"
          name="metric-types"
          value={mt}
          bind:group={visibleMetrics}
          class="fluent-checkbox"
        />
        <span class="checkbox-text">{MetricType[mt]}</span>
      </label>
    {/each}
  </div>
</div>

<script lang="ts">
  import { MetricType } from "$lib/GraphStore";
  import {
    setVisible,
    setInvisible,
    getApplicableMetrics,
  } from "$lib/ProtoAdapter";

  let { allMetricTypes, visibleMetrics } = $props();

  getApplicableMetrics().then((types) => {
    allMetricTypes = types;
    visibleMetrics = types;
  });

  let oldVisibleMetrics: MetricType[] | undefined = undefined;
  $effect(() => {
    const newVal = visibleMetrics;
    if (oldVisibleMetrics !== undefined) {
      const added = (newVal?.filter(
        (m: MetricType) => !oldVisibleMetrics?.includes(m),
      ) ?? []) as MetricType[];
      const removed = (oldVisibleMetrics?.filter(
        (m: MetricType) => !newVal?.includes(m),
      ) ?? []) as MetricType[];
      added.forEach((mt: MetricType) => setVisible(mt));
      removed.forEach((mt: MetricType) => setInvisible(mt));
    }
    oldVisibleMetrics = $state.snapshot(newVal) as MetricType[];
  });
</script>

<style>
  .checkbox-group {
    display: flex;
    flex-direction: column;
    gap: 12px;
  }

  .checkbox-label {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 4px 0;
    user-select: none;
  }

  .fluent-checkbox {
    width: 18px;
    height: 18px;
    margin: 0;
    cursor: pointer;
    accent-color: #0078d4;
    flex-shrink: 0;
  }

  .checkbox-text {
    font-size: 14px;
    line-height: 20px;
    color: #202020;
  }

  @media (prefers-color-scheme: dark) {
    .checkbox-text {
      color: #ffffff;
    }
  }
</style>
