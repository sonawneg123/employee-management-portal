/**
 * @fileoverview Material UI theme factory.
 *
 * Assembles light and dark {@link Theme} objects from the palette, typography,
 * breakpoint, and component override modules.
 */

import { createTheme } from '@mui/material/styles';
import { lightPalette, darkPalette } from './palette';
import typography from './typography';
import { getComponentOverrides } from './components';

/**
 * Custom breakpoint values (pixels).
 *
 * @type {import('@mui/material').BreakpointsOptions}
 */
const breakpoints = {
  values: {
    xs: 0,
    sm: 600,
    md: 900,
    lg: 1200,
    xl: 1536,
  },
};

/**
 * Creates a Material UI theme for the given colour mode.
 *
 * @param {'light' | 'dark'} mode - The colour mode to build the theme for.
 * @returns {import('@mui/material').Theme} The fully assembled MUI theme.
 */
export function createAppTheme(mode) {
  const palette = mode === 'dark' ? darkPalette : lightPalette;

  return createTheme({
    palette,
    typography,
    breakpoints,
    shape: { borderRadius: 12 },
    spacing: 8,
    components: getComponentOverrides(mode),
  });
}

export const lightTheme = createAppTheme('light');
export const darkTheme = createAppTheme('dark');
