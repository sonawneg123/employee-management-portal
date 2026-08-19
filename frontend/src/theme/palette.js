/**
 * @fileoverview Material UI palette definitions for light and dark modes.
 *
 * Brand identity: Deep navy (#1A2342) as primary, warm cream background,
 * gold/amber accent (#F5C518) for CTAs and highlights.
 * Premium SaaS HR product colour system.
 */

/**
 * Light-mode palette — warm cream, deep navy, gold accent.
 *
 * @type {import('@mui/material').ThemeOptions['palette']}
 */
export const lightPalette = {
  mode: 'light',
  primary: {
    main: '#1A2342',
    light: '#2D3A6B',
    dark: '#0F1628',
    contrastText: '#ffffff',
  },
  secondary: {
    main: '#F5C518',
    light: '#FFD966',
    dark: '#C49A00',
    contrastText: '#1A2342',
  },
  error: {
    main: '#EF4444',
    light: '#FCA5A5',
    dark: '#DC2626',
  },
  warning: {
    main: '#F59E0B',
    light: '#FCD34D',
    dark: '#D97706',
  },
  info: {
    main: '#3B82F6',
    light: '#93C5FD',
    dark: '#2563EB',
  },
  success: {
    main: '#10B981',
    light: '#6EE7B7',
    dark: '#059669',
  },
  background: {
    default: '#F5F0E8',
    paper: '#FFFFFF',
  },
  text: {
    primary: '#1A2342',
    secondary: '#6B7280',
    disabled: '#9CA3AF',
  },
  divider: '#E8E3D8',
};

/**
 * Dark-mode palette — deep navy tones, cream accent, gold highlights.
 *
 * @type {import('@mui/material').ThemeOptions['palette']}
 */
export const darkPalette = {
  mode: 'dark',
  primary: {
    main: '#8B9FD4',
    light: '#B0C0E8',
    dark: '#4F6AB5',
    contrastText: '#0F1628',
  },
  secondary: {
    main: '#F5C518',
    light: '#FFD966',
    dark: '#C49A00',
    contrastText: '#1A2342',
  },
  error: {
    main: '#F87171',
    light: '#FCA5A5',
    dark: '#EF4444',
  },
  warning: {
    main: '#FCD34D',
    light: '#FDE68A',
    dark: '#F59E0B',
  },
  info: {
    main: '#60A5FA',
    light: '#93C5FD',
    dark: '#3B82F6',
  },
  success: {
    main: '#34D399',
    light: '#6EE7B7',
    dark: '#10B981',
  },
  background: {
    default: '#0C1220',
    paper: '#131C2E',
  },
  text: {
    primary: '#F0EDE6',
    secondary: '#94A3B8',
    disabled: '#475569',
  },
  divider: 'rgba(240,237,230,0.08)',
};
