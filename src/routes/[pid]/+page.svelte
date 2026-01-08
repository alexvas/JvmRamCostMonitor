<script lang="ts">
  import { page } from '$app/stores';
  let pid = $derived($page.params.pid);
</script>

<h2>Process {pid}</h2>