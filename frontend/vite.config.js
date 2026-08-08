import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));


/**
 * Vite configuration for the Employee Management Portal frontend.
 *
 * - Resolves `@/` as an alias to `src/` for clean absolute imports.
 * - Proxies `/api` requests to the Spring Boot backend during development,
 *   so the browser never touches CORS on localhost.
 * - Configures a deterministic manual chunk split to keep bundles small.
 */
export default defineConfig({
  plugins: [react({ include: /\.[jt]sx?$/ })],

  test: {
    environment: 'jsdom',
    globals:     true,
    setupFiles:  ['./src/tests/setup.js'],
    css:         false,
    coverage: {
      provider:   'v8',
      reporter:   ['text', 'lcov'],
      include:    ['src/**/*.{js,jsx}'],
      exclude:    ['src/tests/**', 'src/pages/**/*Page.jsx', 'src/main.jsx'],
    },
  },


  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src'),
    },
  },

  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
      },
    },
  },

  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1200,
    rollupOptions: {
      output: {
        manualChunks: {
          vendor:    ['react', 'react-dom', 'react-router-dom'],
          mui:       ['@mui/material', '@mui/icons-material', '@emotion/react', '@emotion/styled'],
          query:     ['@tanstack/react-query'],
          forms:     ['react-hook-form', '@hookform/resolvers', 'zod'],
          utilities: ['axios', 'dayjs', 'jwt-decode'],
        },
      },
    },
  },

  preview: {
    port: 4173,
    strictPort: true,
  },
});
