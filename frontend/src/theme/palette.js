/**
 * @fileoverview Material UI palette definitions for light and dark modes.
 *
 * Brand identity: Indigo (#4F46E5) as primary, Violet (#7C3AED) as accent.
 * Status colours follow semantic conventions: emerald = success,
 * amber = warning, red = error.
 */

/**
 * Light-mode palette.
 *
 * @type {import('@mui/material').ThemeOptions['palette']}
 */
export const lightPalette = {
  mode: 'light',
  primary: {
    main: '#4F46E5',
    light: '#818CF8',
    dark: '#4338CA',
    contrastText: '#ffffff',
  },
  secondary: {
    main: '#7C3AED',
    light: '#A78BFA',
    dark: '#5B21B6',
    contrastText: '#ffffff',
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
    default: '#F8FAFC',
    paper: '#FFFFFF',
  },
  text: {
    primary: '#0F172A',
    secondary: '#64748B',
    disabled: '#94A3B8',
  },
  divider: '#E2E8F0',
};

/**
 * Dark-mode palette.
 *
 * @type {import('@mui/material').ThemeOptions['palette']}
 */
export const darkPalette = {
  mode: 'dark',
  primary: {
    main: '#818CF8',
    light: '#A5B4FC',
    dark: '#4F46E5',
    contrastText: '#ffffff',
  },
  secondary: {
    main: '#A78BFA',
    light: '#C4B5FD',
    dark: '#7C3AED',
    contrastText: '#ffffff',
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
    default: '#0A0F1E',
    paper: '#111827',
  },
  text: {
    primary: '#F1F5F9',
    secondary: '#94A3B8',
    disabled: '#475569',
  },
  divider: 'rgba(241,245,249,0.08)',
};
