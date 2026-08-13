/**
 * @fileoverview Application entry point.
 *
 * Mounts the React root into the {@code #root} div defined in {@code index.html}.
 * React 19 uses {@code createRoot} with concurrent mode enabled by default.
 */

import React from 'react';
import { createRoot } from 'react-dom/client';
import App from './App';

const container = document.getElementById('root');

if (!container) {
  throw new Error('[main.jsx] Root element #root not found in DOM. Check index.html.');
}

createRoot(container).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
);
