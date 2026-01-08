<script lang="ts">
  import { setContext } from 'svelte';

  let { children } = $props();
  let followingPids = $state<bigint[]>([]);
  
  setContext('followingPids', () => followingPids);

</script>

<div>
<nav>
  <ul>
    <li>
      <a href="/">Settings</a>
    </li>
    {#each followingPids as pid}
      <li>
        <a href={`/${pid}`}>{pid}</a>
      </li>
    {/each}
  </ul>
</nav>
<main class="container">
  <h1>JVM RAM Cost</h1>
  {@render children()}
</main>
</div>