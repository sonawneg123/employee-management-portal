/**
 * @fileoverview Material UI palette definitions for light and dark modes.
 *
 * The primary colour is the company brand blue (#1976d2). All other colours
 * are derived from it or from Material UI's default palette to ensure
 * accessible contrast ratios (WCAG AA minimum).
 */

/**
 * Light-mode palette.
 *
 * @type {import('@mui/material').ThemeOptions['palette']}
 */
export const lightPalette = {
  mode: 'light',
  primary: {
    main:         '#1976d2',
    light:        '#42a5f5',
    dark:         '#1565c0',
    contrastText: '#ffffff',
  },
  secondary: {
    main:         '#7c3aed',
    light:        '#a78bfa',
    dark:         '#5b21b6',
    contrastText: '#ffffff',
  },
  error: {
    main:  '#d32f2f',
    light: '#ef5350',
    dark:  '#c62828',
  },
  warning: {
    main:  '#ed6c02',
    light: '#ff9800',
    dark:  '#e65100',
  },
  info: {
    main:  '#0288d1',
    light: '#03a9f4',
    dark:  '#01579b',
  },
  success: {
    main:  '#2e7d32',
    light: '#4caf50',
    dark:  '#1b5e20',
  },
  background: {
    default: '#f4f6f8',
    paper:   '#ffffff',
  },
  text: {
    primary:   'rgba(0,0,0,0.87)',
    secondary: 'rgba(0,0,0,0.6)',
    disabled:  'rgba(0,0,0,0.38)',
  },
  divider: 'rgba(0,0,0,0.12)',
};

/**
 * Dark-mode palette.
 *
 * @type {import('@mui/material').ThemeOptions['palette']}
 */
export const darkPalette = {
  mode: 'dark',
  primary: {
    main:         '#42a5f5',
    light:        '#80d8ff',
    dark:         '#0077c2',
    contrastText: '#000000',
  },
  secondary: {
    main:         '#a78bfa',
    light:        '#c4b5fd',
    dark:         '#7c3aed',
    contrastText: '#000000',
  },
  error: {
    main:  '#f44336',
    light: '#e57373',
    dark:  '#d32f2f',
  },
  warning: {
    main:  '#ffa726',
    light: '#ffb74d',
    dark:  '#f57c00',
  },
  info: {
    main:  '#29b6f6',
    light: '#4fc3f7',
    dark:  '#0288d1',
  },
  success: {
    main:  '#66bb6a',
    light: '#81c784',
    dark:  '#388e3c',
  },
  background: {
    default: '#0f1723',
    paper:   '#1a2332',
  },
  text: {
    primary:   'rgba(255,255,255,0.87)',
    secondary: 'rgba(255,255,255,0.6)',
    disabled:  'rgba(255,255,255,0.38)',
  },
  divider: 'rgba(255,255,255,0.12)',
};
