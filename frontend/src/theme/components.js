/**
 * @fileoverview Material UI component overrides applied globally to both themes.
 *
 * Each key corresponds to a MUI component name. These defaults eliminate
 * repetitive prop drilling across the codebase.
 *
 * @param {import('@mui/material').PaletteMode} mode - Current colour mode.
 * @returns {import('@mui/material').ThemeOptions['components']} MUI component overrides.
 */
export function getComponentOverrides(mode) {
  const isDark = mode === 'dark';

  return {
    MuiCssBaseline: {
      styleOverrides: {
        '*, *::before, *::after': { boxSizing: 'border-box' },
        body: { scrollbarWidth: 'thin' },
      },
    },

    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          borderRadius: 8,
          padding: '8px 20px',
          fontWeight: 600,
        },
        sizeSmall: { padding: '4px 12px' },
        sizeLarge: { padding: '12px 28px' },
      },
    },

    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          borderRadius: 12,
          border: `1px solid ${isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)'}`,
        },
      },
    },

    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          backgroundImage: 'none',
        },
        rounded: { borderRadius: 12 },
      },
    },

    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
        size:    'small',
        fullWidth: true,
      },
    },

    MuiOutlinedInput: {
      styleOverrides: {
        root: { borderRadius: 8 },
      },
    },

    MuiInputLabel: {
      defaultProps: { shrink: true },
    },

    MuiChip: {
      styleOverrides: {
        root: { borderRadius: 6, fontWeight: 500 },
      },
    },

    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            fontWeight: 600,
            fontSize: '0.8125rem',
            backgroundColor: isDark ? 'rgba(255,255,255,0.04)' : 'rgba(0,0,0,0.03)',
          },
        },
      },
    },

    MuiTableCell: {
      styleOverrides: {
        root: { borderColor: isDark ? 'rgba(255,255,255,0.08)' : 'rgba(0,0,0,0.08)' },
      },
    },

    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 8,
          '&.Mui-selected': { fontWeight: 600 },
        },
      },
    },

    MuiTooltip: {
      defaultProps: { arrow: true },
    },

    MuiDialog: {
      styleOverrides: {
        paper: { borderRadius: 16 },
      },
    },

    MuiAlert: {
      styleOverrides: {
        root: { borderRadius: 8 },
      },
    },

    MuiAvatar: {
      styleOverrides: {
        root: { fontWeight: 600 },
      },
    },

    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 4 },
      },
    },

    MuiBreadcrumbs: {
      styleOverrides: {
        root: { fontSize: '0.8125rem' },
      },
    },
  };
}
