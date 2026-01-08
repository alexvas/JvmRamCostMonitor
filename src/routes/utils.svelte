<script lang="ts">

export function watch<T>(getter: () => T, callback: (newVal: T, oldVal: T | undefined) => void) {
    let oldVal: T | undefined = undefined;
    $effect(() => {
      const newVal = getter();
      callback(newVal, oldVal);
      oldVal = $state.snapshot(newVal) as T; // снимает Proxy-обёртку
    });
}

</script>
