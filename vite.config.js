import path from "node:path";
import { defineConfig, searchForWorkspaceRoot } from "vite";
import { sveltekit } from "@sveltejs/kit/vite";

// @ts-expect-error process is a nodejs global
const host = process.env.TAURI_DEV_HOST;
const cwd = process.cwd();

// https://vitejs.dev/config/
export default defineConfig(async () => ({
  plugins: [sveltekit()],

  // Vite options tailored for Tauri development and only applied in `tauri dev` or `tauri build`
  //
  // 1. prevent vite from obscuring rust errors
  clearScreen: false,
  // 2. tauri expects a fixed port, fail if that port is not available
  server: {
    port: 1420,
    strictPort: true,
    host: host || false,
    // Yarn PnP (zipfs) резолвит зависимости в .yarn/berry/cache/*.zip/...
    // В dev-режиме Vite блокирует такие пути по allow-list'у файловой системы,
    // из‑за чего WebView в Tauri открывается пустым.
    fs: {
      // оставляем allow для явности (даже если strict выключен)
      allow: [searchForWorkspaceRoot(cwd), path.resolve(cwd, ".yarn/__virtual__/@sveltejs-kit*")],
    },
    hmr: host
      ? {
          protocol: "ws",
          host,
          port: 1421,
        }
      : undefined,
    watch: {
      // 3. tell vite to ignore watching `src-tauri`
      ignored: ["**/src-tauri/**"],
    },
  },
}));
