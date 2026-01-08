<script lang="ts">
  import { invoke } from "@tauri-apps/api/core";
  import { MetricType, SetVisibleRequest } from "../generated/protocol";

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
  const allMetricTypes = Object.values(MetricType).filter((v): v is MetricType => typeof v === 'number');

  let visibleMetrics = $state<MetricType[]>(allMetricTypes);

  async function greet(event: Event) {
    event.preventDefault();
    // Learn more about Tauri commands at https://tauri.app/develop/calling-rust/
    greetMsg = await invoke("greet", { name });
  }
 
  async function setVisible(mt: MetricType) {
    const request = SetVisibleRequest.create({ metricType: mt });
    console.log('set visible request', request);
    const response = await invoke("set_visible", { request });
  }

  async function setInvisible(mt: MetricType) {
    const request = SetInvisibleRequest.create({ metricType: mt });
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

  <div class="row">
    {#each allMetricTypes as mt}
      <label>
        <input 
          type="checkbox"
          name="metric-types"
          value={mt}
          bind:group={visibleMetrics}
        />
        {MetricType[mt]}
      </label>
    {/each}




  </div>
  <form class="row" onsubmit={greet}>
    <input id="greet-input" placeholder="Enter a name..." bind:value={name} />
    <button type="submit">Greet</button>
  </form>
  <p>{greetMsg}</p>
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
  justify-content: center;
}

h1 {
  text-align: center;
}

input,
button {
  border-radius: 8px;
  border: 1px solid transparent;
  padding: 0.6em 1.2em;
  font-size: 1em;
  font-weight: 500;
  font-family: inherit;
  color: #0f0f0f;
  background-color: #ffffff;
  transition: border-color 0.25s;
  box-shadow: 0 2px 2px rgba(0, 0, 0, 0.2);
}

button {
  cursor: pointer;
}

button:hover {
  border-color: #396cd8;
}
button:active {
  border-color: #396cd8;
  background-color: #e8e8e8;
}

input,
button {
  outline: none;
}

#greet-input {
  margin-right: 5px;
}

@media (prefers-color-scheme: dark) {
  :root {
    color: #f6f6f6;
    background-color: #2f2f2f;
  }

  input,
  button {
    color: #ffffff;
    background-color: #0f0f0f98;
  }
  button:active {
    background-color: #0f0f0f69;
  }
}

</style>
