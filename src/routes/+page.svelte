<script lang="ts">
  import { invoke } from "@tauri-apps/api/core";
  import { MetricType, SetVisibleRequest, SetInvisibleRequest } from "../generated/proto/protocol";

  function watch<T>(getter: () => T, callback: (newVal: T, oldVal: T | undefined) => void) {
    let oldVal: T | undefined = undefined;
    $effect(() => {
      const newVal = getter();
      callback(newVal, oldVal);
      oldVal = $state.snapshot(newVal) as T; // снимает Proxy-обёртку
    });
  }

  let name = $state("");
  let greetMsg = $state("");
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

  watch(
    () => visibleMetrics,
    (newVal, oldVal) => {
      console.log('Изменение:', { oldVal, newVal });
      // Вычисление дельты
      const added = newVal.filter(m => !oldVal?.includes(m));
      const removed = oldVal?.filter(m => !newVal.includes(m)) ?? [];
      console.log('Добавлено:', added.map(m => MetricType[m]), 'Удалено:', removed.map(m => MetricType[m]));
      added.forEach(mt => setVisible(mt));
      removed.forEach(mt => setInvisible(mt));
    }
  )


</script>

<main class="container">
  <h1>JVM RAM Cost</h1>

  {#each allMetricTypes as mt}
    <div class="row">
      <label>
        <input 
          type="checkbox"
          name="metric-types"
          value={mt}
          bind:group={visibleMetrics}
        />
        {MetricType[mt]}
      </label>
    </div>
  {/each}
</main>

<style>

:root {
  font-family: Inter, Avenir, Helvetica, Arial, sans-serif;
  font-size: 16px;
  line-height: 24px;
  font-weight: 400;

  color: #0f0f0f;
  background-color: #f6f6f6;

  font-synthesis: none;
  text-rendering: optimizeLegibility;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  -webkit-text-size-adjust: 100%;
}

.container {
  margin: 0;
  padding-top: 10vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: center;
}

.row {
  display: flex;
  justify-content: left;
}

h1 {
  text-align: center;
}

input,

input {
  outline: none;
}

@media (prefers-color-scheme: dark) {
  :root {
    color: #f6f6f6;
    background-color: #2f2f2f;
  }

  input {
    color: #ffffff;
    background-color: #0f0f0f98;
  }
}

</style>
