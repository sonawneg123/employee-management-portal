/**
 * @fileoverview ThemeContext — provides colour mode switching across the app.
 *
 * Persists the user's preference in localStorage so that it survives
 * page refreshes. Wrap the entire component tree with {@link ThemeProvider}.
 */

import React, { createContext, useCallback, useContext, useMemo, useState } from 'react';
import { ThemeProvider as MuiThemeProvider, CssBaseline } from '@mui/material';
import { createAppTheme } from './index';
import { getItem, setItem } from '@/utils/localStorage';

/** @type {string} */
const THEME_MODE_KEY = 'emp_portal_theme_mode';

/**
 * @typedef {'light' | 'dark'} ThemeMode
 */

/**
 * @typedef {Object} ThemeContextValue
 * @property {ThemeMode} mode          - Current colour mode.
 * @property {() => void} toggleMode   - Switches between light and dark mode.
 * @property {(m: ThemeMode) => void} setMode - Sets a specific mode.
 */

/** @type {React.Context<ThemeContextValue>} */
const ThemeContext = createContext(/** @type {ThemeContextValue} */ ({}));

/**
 * Application theme provider.
 *
 * Wraps children with the correct MUI theme and exposes {@link ThemeContext}
 * so that any descendant component can read or toggle the colour mode.
 *
 * @param {{ children: React.ReactNode }} props
 * @returns {JSX.Element}
 */
export function ThemeProvider({ children }) {
  const [mode, setModeState] = useState(
    /** @type {ThemeMode} */ (getItem(THEME_MODE_KEY, 'light')),
  );

  /**
   * Toggles between light and dark mode.
   *
   * @type {() => void}
   */
  const toggleMode = useCallback(() => {
    setModeState((prev) => {
      const next = prev === 'light' ? 'dark' : 'light';
      setItem(THEME_MODE_KEY, next);
      return next;
    });
  }, []);

  /**
   * Sets a specific colour mode.
   *
   * @type {(m: ThemeMode) => void}
   */
  const setMode = useCallback((newMode) => {
    setItem(THEME_MODE_KEY, newMode);
    setModeState(newMode);
  }, []);

  const theme = useMemo(() => createAppTheme(mode), [mode]);

  const contextValue = useMemo(() => ({ mode, toggleMode, setMode }), [mode, toggleMode, setMode]);

  return (
    <ThemeContext.Provider value={contextValue}>
      <MuiThemeProvider theme={theme}>
        <CssBaseline />
        {children}
      </MuiThemeProvider>
    </ThemeContext.Provider>
  );
}

/**
 * Hook to access the current theme mode and toggle function.
 *
 * @returns {ThemeContextValue}
 */
// eslint-disable-next-line react-refresh/only-export-components
export function useThemeMode() {
  const ctx = useContext(ThemeContext);
  if (!ctx) throw new Error('useThemeMode must be used inside <ThemeProvider>');
  return ctx;
}

export { ThemeContext };
