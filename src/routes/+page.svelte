<script lang="ts">
  import { invoke } from "@tauri-apps/api/core";
  import { MetricType, SetVisibleRequest, SetInvisibleRequest, PidList, ProcInfo } from "../generated/proto/protocol";

  function watch<T>(getter: () => T, callback: (newVal: T, oldVal: T | undefined) => void) {
    let oldVal: T | undefined = undefined;
    $effect(() => {
      const newVal = getter();
      callback(newVal, oldVal);
      oldVal = $state.snapshot(newVal) as T; // снимает Proxy-обёртку
    });
  }

  const allMetricTypes = Object.values(MetricType).filter((v): v is MetricType => typeof v === 'number' && v >= 0);
  let visibleMetrics = $state<MetricType[]>(allMetricTypes);
 
  async function setVisible(mt: MetricType) {
    const request = SetVisibleRequest.create({ metric_type: mt });
    console.log('set visible request', request);
    const response = await invoke("set_visible", { request });
  }

  async function setInvisible(mt: MetricType) {
    const request = SetInvisibleRequest.create({ metric_type: mt });
    const response = await invoke("set_invisible", { request });
  }

  let followingPids = $state<bigint[]>([]);

  async function followPids(pids: bigint[]) {
    const request = PidList.create({ pids: pids.map(pid => Pid.create({ pid: pid })) });
    const response = await invoke("set_following_pids", { request });
  }

  $effect(() => {
    followPids(followingPids);
  });

  watch(
    () => visibleMetrics,
    (newVal, oldVal) => {
      const added = newVal.filter(m => !oldVal?.includes(m));
      const removed = oldVal?.filter(m => !newVal.includes(m)) ?? [];
      added.forEach(mt => setVisible(mt));
      removed.forEach(mt => setInvisible(mt));
    }
  )

  import { listen } from '@tauri-apps/api/event';
  let availableJvmProcesses = $state<ProcInfo[]>([]);

  listen<{payload: ProcInfo[]}>('available-jvm-processes-updated', (event) => {
    availableJvmProcesses = event.payload;
  });

</script>

<main class="container">
  <h1>JVM RAM Cost</h1>

  <div class="two-column-layout">
    <div class="column card">
      <h2 class="card-title">Metric Types</h2>
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

    <div class="column card">
      <h2 class="card-title">Processes</h2>
      <select multiple bind:value={followingPids} class="fluent-select">
        {#each availableJvmProcesses as process}
          <option value={process.pid}>{process.pid} - {process.display_name}</option>
        {/each}
      </select>
    </div>
  </div>
</main>

<style>
:root {
  font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, 'Roboto', 'Helvetica Neue', Arial, sans-serif;
  font-size: 14px;
  line-height: 20px;
  font-weight: 400;

  color: #202020;
  background-color: #f3f3f3;

  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  -webkit-text-size-adjust: 100%;
}

.container {
  margin: 0;
  padding: 24px;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

h1 {
  font-size: 28px;
  font-weight: 600;
  line-height: 36px;
  margin: 0 0 24px 0;
  color: #202020;
  text-align: left;
}

.two-column-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  flex: 1;
}

.column {
  display: flex;
  flex-direction: column;
}

.card {
  background-color: #ffffff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 1.6px 3.6px rgba(0, 0, 0, 0.13), 0 0.3px 0.9px rgba(0, 0, 0, 0.11);
  transition: box-shadow 0.2s ease;
}

.card:hover {
  box-shadow: 0 3.2px 7.2px rgba(0, 0, 0, 0.13), 0 0.6px 1.8px rgba(0, 0, 0, 0.11);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 24px;
  margin: 0 0 16px 0;
  color: #202020;
}

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

.fluent-select {
  width: 100%;
  min-height: 200px;
  padding: 8px 12px;
  border: 1px solid #d1d1d1;
  border-radius: 4px;
  background-color: #ffffff;
  font-size: 14px;
  line-height: 20px;
  color: #202020;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.fluent-select:focus {
  border-color: #0078d4;
  box-shadow: 0 0 0 1px #0078d4;
}

.fluent-select option {
  padding: 8px;
}

@media (prefers-color-scheme: dark) {
  :root {
    color: #ffffff;
    background-color: #202020;
  }

  h1 {
    color: #ffffff;
  }

  .card {
    background-color: #2d2d2d;
    box-shadow: 0 1.6px 3.6px rgba(0, 0, 0, 0.36), 0 0.3px 0.9px rgba(0, 0, 0, 0.24);
  }

  .card:hover {
    box-shadow: 0 3.2px 7.2px rgba(0, 0, 0, 0.36), 0 0.6px 1.8px rgba(0, 0, 0, 0.24);
  }

  .card-title {
    color: #ffffff;
  }

  .checkbox-text {
    color: #ffffff;
  }

  .fluent-select {
    background-color: #1e1e1e;
    border-color: #3d3d3d;
    color: #ffffff;
  }

  .fluent-select:focus {
    border-color: #60cdff;
    box-shadow: 0 0 0 1px #60cdff;
  }
}

</style>
