<script lang="ts">
  import { setContext } from 'svelte';
  import { page } from '$app/stores';

  let { children } = $props();
  let followingPids = $state<bigint[]>([]);
  
  setContext('followingPids', () => followingPids);

  function isActive(href: string): boolean {
    const currentPath = $page.url.pathname;
    if (href === '/') {
      return currentPath === '/';
    }
    return currentPath === href;
  }

</script>

<div>
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
  <h1>JVM RAM Cost</h1>
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