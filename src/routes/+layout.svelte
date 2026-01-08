<script lang="ts">
  import { setContext } from 'svelte';
  import { page } from '$app/stores';
  import { ProcInfo } from '$lib/generated/proto/protocol';
  import { invoke } from '@tauri-apps/api/core';

  let { children } = $props();
  let followingPids = $state<bigint[]>([]);
  let availableJvmProcesses = $state<Map<bigint, ProcInfo>>(new Map());

  setContext('followingPids', () => followingPids);
  setContext('availableJvmProcesses', () => availableJvmProcesses);

  import { listen } from '@tauri-apps/api/event';

  listen<{payload: ProcInfo[]}>('available-jvm-processes-updated', (event) => {
    const sortedProcesses = [...event.payload].sort((a, b) => {
      const pidA = typeof a.pid === 'bigint' ? a.pid : BigInt(a.pid);
      const pidB = typeof b.pid === 'bigint' ? b.pid : BigInt(b.pid);
      if (pidA < pidB) return -1;
      if (pidA > pidB) return 1;
      return 0;
    });
    availableJvmProcesses = new Map(sortedProcesses.map(proc => {
      const pid = typeof proc.pid === 'bigint' ? proc.pid : BigInt(proc.pid);
      return [pid, proc];
    }));
  });

  function isActive(href: string): boolean {
    const currentPath = $page.url.pathname;
    if (href === '/') {
      return currentPath === '/';
    }
    return currentPath === href;
  }

</script>

<div>
<h1>JVM RAM Cost</h1>
<nav class="tabs-nav">
  <ul class="tabs-list">
    <li>
      <a href="/" class="tab-link" class:active={isActive('/')}>Settings</a>
    </li>
    {#each followingPids as pid}
      <li>
        <a href={`/${pid}`} class="tab-link" class:active={isActive(`/${pid}`)}>{pid}</a>
      </li>
    {/each}
  </ul>
</nav>
<main class="container">
  {@render children()}
</main>
</div>

<style>
  .tabs-nav {
    background-color: #ffffff;
    border-bottom: 1px solid #d1d1d1;
    padding: 0;
    margin: 0;
  }

  .tabs-list {
    display: flex;
    flex-direction: row;
    list-style: none;
    margin: 0;
    padding: 0;
    gap: 0;
  }

  .tabs-list li {
    margin: 0;
    padding: 0;
  }

  .tab-link {
    display: block;
    padding: 12px 20px;
    text-decoration: none;
    color: #202020;
    border-bottom: 2px solid transparent;
    transition: background-color 0.15s ease, border-color 0.15s ease;
    font-size: 14px;
    line-height: 20px;
    cursor: pointer;
  }

  .tab-link:hover {
    background-color: #f3f3f3;
  }

  .tab-link:global(.active) {
    border-bottom-color: #0078d4;
    color: #0078d4;
    font-weight: 500;
  }

  @media (prefers-color-scheme: dark) {
    .tabs-nav {
      background-color: #2d2d2d;
      border-bottom-color: #3d3d3d;
    }

    .tab-link {
      color: #ffffff;
    }

    .tab-link:hover {
      background-color: #3d3d3d;
    }

    .tab-link:global(.active) {
      border-bottom-color: #60cdff;
      color: #60cdff;
    }
  }
</style>