<script lang="ts">
  let { availableJvmProcesses, followingPids } = $props();

  import { invoke } from "@tauri-apps/api/core";
  import { PidList, Pid, ProcInfo } from "../generated/proto/protocol";

  async function followPids(pids: bigint[]) {
    const request = PidList.create({ pids: pids.map(pid => Pid.create({ pid: pid })) });
    const response = await invoke("set_following_pids", { request });
  }

  $effect(() => {
    followPids(followingPids);
  });

  import { listen } from '@tauri-apps/api/event';

  listen<{payload: ProcInfo[]}>('available-jvm-processes-updated', (event) => {
    availableJvmProcesses = event.payload;
  });

</script>


<div class="column card">
    <h2 class="card-title">Processes</h2>
    <div class="process-list">
      {#each availableJvmProcesses as process}
        <label class="process-item" class:selected={followingPids.includes(process.pid)}>
          <input 
            type="checkbox"
            checked={followingPids.includes(process.pid)}
            onchange={(e) => {
              if (e.currentTarget.checked) {
                followingPids = [...followingPids, process.pid];
              } else {
                followingPids = followingPids.filter(pid => pid !== process.pid);
              }
            }}
            class="process-checkbox"
          />
          <span class="process-text">{process.pid} - {process.display_name}</span>
        </label>
      {/each}
    </div>
</div>

<style>
    .process-list {
  width: 100%;
  min-height: 200px;
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #d1d1d1;
  border-radius: 4px;
  background-color: #ffffff;
  padding: 4px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.process-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border-radius: 4px;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.15s ease;
  background-color: transparent;
}

.process-item:hover {
  background-color: #f3f3f3;
}

.process-item.selected {
  background-color: #0078d4;
}

.process-item.selected:hover {
  background-color: #106ebe;
}

.process-checkbox {
  width: 18px;
  height: 18px;
  margin: 0;
  cursor: pointer;
  accent-color: #0078d4;
  flex-shrink: 0;
}

.process-text {
  font-size: 14px;
  line-height: 20px;
  color: #202020;
  flex: 1;
}

.process-item.selected .process-text {
  color: #ffffff;
}

@media (prefers-color-scheme: dark) {

  .process-list {
    background-color: #1e1e1e;
    border-color: #3d3d3d;
  }

  .process-item:hover {
    background-color: #3d3d3d;
  }

  .process-item.selected {
    background-color: #60cdff;
  }

  .process-item.selected:hover {
    background-color: #40b8ff;
  }

  .process-text {
    color: #ffffff;
  }

  .process-item.selected .process-text {
    color: #000000;
  }
}

</style>    