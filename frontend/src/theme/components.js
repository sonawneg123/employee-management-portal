/**
 * @fileoverview Material UI component overrides — premium SaaS design system.
 *
 * Applies global defaults that eliminate repetitive prop drilling.
 * Design tokens: border-radius 12px cards, 10px inputs, 8px buttons.
 * Animations: 150–250ms ease, respects prefers-reduced-motion.
 *
 * @param {import('@mui/material').PaletteMode} mode
 * @returns {import('@mui/material').ThemeOptions['components']}
 */
export function getComponentOverrides(mode) {
  const isDark = mode === 'dark';

  return {
    MuiCssBaseline: {
      styleOverrides: {
        '*, *::before, *::after': { boxSizing: 'border-box' },
        html: { WebkitFontSmoothing: 'antialiased', MozOsxFontSmoothing: 'grayscale' },
        body: {
          scrollbarWidth: 'thin',
          scrollbarColor: isDark
            ? 'rgba(255,255,255,0.15) transparent'
            : 'rgba(0,0,0,0.12) transparent',
        },
        /* Page entrance animation — respects prefers-reduced-motion */
        '@keyframes fadeUp': {
          from: { opacity: 0, transform: 'translateY(12px)' },
          to: { opacity: 1, transform: 'translateY(0)' },
        },
        '@keyframes fadeIn': {
          from: { opacity: 0 },
          to: { opacity: 1 },
        },
        '@keyframes scaleIn': {
          from: { opacity: 0, transform: 'scale(0.97)' },
          to: { opacity: 1, transform: 'scale(1)' },
        },
        '@media (prefers-reduced-motion: reduce)': {
          '*, *::before, *::after': {
            animationDuration: '0.01ms !important',
            animationIterationCount: '1 !important',
            transitionDuration: '0.01ms !important',
          },
        },
      },
    },

    MuiButton: {
      defaultProps: {
        disableElevation: true,
      },
      styleOverrides: {
        root: {
          borderRadius: 10,
          padding: '9px 20px',
          fontWeight: 600,
          transition: 'all 0.15s ease',
          '&:active': { transform: 'scale(0.98)' },
        },
        sizeSmall: { padding: '5px 14px', fontSize: '0.8125rem' },
        sizeLarge: { padding: '12px 28px', fontSize: '0.9375rem' },
        contained: {
          boxShadow: 'none',
          '&:hover': {
            boxShadow: isDark
              ? '0 4px 16px rgba(79,70,229,0.35)'
              : '0 4px 16px rgba(79,70,229,0.25)',
          },
        },
        outlined: {
          borderWidth: '1.5px',
          '&:hover': { borderWidth: '1.5px' },
        },
      },
    },

    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          borderRadius: 16,
          border: `1px solid ${isDark ? 'rgba(241,245,249,0.07)' : '#E2E8F0'}`,
          transition: 'box-shadow 0.2s ease, transform 0.2s ease',
          backgroundImage: 'none',
        },
      },
    },

    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        rounded: { borderRadius: 16 },
        elevation1: {
          boxShadow: isDark
            ? '0 1px 3px rgba(0,0,0,0.4), 0 1px 2px rgba(0,0,0,0.3)'
            : '0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04)',
        },
        elevation2: {
          boxShadow: isDark ? '0 4px 12px rgba(0,0,0,0.5)' : '0 4px 12px rgba(0,0,0,0.08)',
        },
        elevation4: {
          boxShadow: isDark ? '0 8px 32px rgba(0,0,0,0.6)' : '0 8px 32px rgba(0,0,0,0.1)',
        },
      },
    },

    MuiTextField: {
      defaultProps: {
        variant: 'outlined',
        size: 'small',
        fullWidth: true,
      },
    },

    MuiOutlinedInput: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transition: 'box-shadow 0.15s ease',
          '&.Mui-focused': {
            boxShadow: `0 0 0 3px ${isDark ? 'rgba(79,70,229,0.25)' : 'rgba(79,70,229,0.15)'}`,
          },
        },
        notchedOutline: {
          borderColor: isDark ? 'rgba(241,245,249,0.15)' : '#E2E8F0',
          borderWidth: '1.5px',
        },
        input: {
          padding: '10px 14px',
          '&::placeholder': { opacity: 0.5 },
        },
      },
    },

    MuiInputLabel: {
      defaultProps: { shrink: true },
      styleOverrides: {
        root: {
          fontWeight: 500,
          fontSize: '0.875rem',
        },
      },
    },

    MuiSelect: {
      styleOverrides: {
        select: { padding: '10px 14px' },
      },
    },

    MuiChip: {
      styleOverrides: {
        root: {
          borderRadius: 6,
          fontWeight: 600,
          fontSize: '0.75rem',
          letterSpacing: '0.01em',
          height: 24,
        },
        sizeSmall: { height: 22, fontSize: '0.6875rem' },
        sizeMedium: { height: 28 },
      },
    },

    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            fontWeight: 600,
            fontSize: '0.75rem',
            letterSpacing: '0.05em',
            textTransform: 'uppercase',
            color: isDark ? 'rgba(241,245,249,0.55)' : '#64748B',
            backgroundColor: isDark ? 'rgba(255,255,255,0.025)' : '#F8FAFC',
            borderBottom: `1px solid ${isDark ? 'rgba(241,245,249,0.08)' : '#E2E8F0'}`,
          },
        },
      },
    },

    MuiTableCell: {
      styleOverrides: {
        root: {
          fontSize: '0.875rem',
          borderColor: isDark ? 'rgba(241,245,249,0.07)' : '#F1F5F9',
          padding: '12px 16px',
        },
      },
    },

    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': {
            backgroundColor: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(79,70,229,0.025)',
          },
          transition: 'background-color 0.15s ease',
        },
      },
    },

    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          transition: 'all 0.15s ease',
          '&.Mui-selected': {
            fontWeight: 600,
          },
        },
      },
    },

    MuiTooltip: {
      defaultProps: { arrow: true },
      styleOverrides: {
        tooltip: {
          fontSize: '0.75rem',
          fontWeight: 500,
          borderRadius: 8,
          padding: '6px 10px',
        },
      },
    },

    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 20,
          border: `1px solid ${isDark ? 'rgba(241,245,249,0.08)' : '#E2E8F0'}`,
        },
      },
    },

    MuiDialogTitle: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          fontSize: '1.0625rem',
          padding: '20px 24px 8px',
        },
      },
    },

    MuiDialogContent: {
      styleOverrides: {
        root: { padding: '8px 24px 8px' },
      },
    },

    MuiDialogActions: {
      styleOverrides: {
        root: { padding: '16px 24px 20px', gap: 8 },
      },
    },

    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 10,
          fontSize: '0.875rem',
          fontWeight: 500,
        },
        standardError: { border: '1px solid rgba(239,68,68,0.2)' },
        standardSuccess: { border: '1px solid rgba(16,185,129,0.2)' },
        standardWarning: { border: '1px solid rgba(245,158,11,0.2)' },
        standardInfo: { border: '1px solid rgba(59,130,246,0.2)' },
      },
    },

    MuiAvatar: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          fontSize: '0.875rem',
        },
      },
    },

    MuiLinearProgress: {
      styleOverrides: {
        root: { borderRadius: 999 },
      },
    },

    MuiBreadcrumbs: {
      styleOverrides: {
        root: { fontSize: '0.8125rem' },
      },
    },

    MuiDrawer: {
      styleOverrides: {
        paper: { backgroundImage: 'none' },
      },
    },

    MuiAppBar: {
      styleOverrides: {
        root: { backgroundImage: 'none' },
      },
    },

    MuiDivider: {
      styleOverrides: {
        root: {
          borderColor: isDark ? 'rgba(241,245,249,0.08)' : '#E2E8F0',
        },
      },
    },

    MuiTab: {
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: '0.875rem',
          textTransform: 'none',
          minHeight: 40,
          padding: '8px 16px',
        },
      },
    },

    MuiTabs: {
      styleOverrides: {
        indicator: {
          height: 2,
          borderRadius: 2,
        },
      },
    },

    MuiSwitch: {
      styleOverrides: {
        root: {
          '& .MuiSwitch-thumb': { boxShadow: 'none' },
        },
      },
    },

    MuiMenu: {
      styleOverrides: {
        paper: {
          borderRadius: 12,
          border: `1px solid ${isDark ? 'rgba(241,245,249,0.08)' : '#E2E8F0'}`,
          boxShadow: isDark ? '0 8px 32px rgba(0,0,0,0.6)' : '0 8px 32px rgba(0,0,0,0.1)',
          backgroundImage: 'none',
        },
      },
    },

    MuiMenuItem: {
      styleOverrides: {
        root: {
          fontSize: '0.875rem',
          fontWeight: 500,
          borderRadius: 6,
          margin: '2px 4px',
          padding: '7px 10px',
          '&:hover': {
            backgroundColor: isDark ? 'rgba(255,255,255,0.06)' : 'rgba(79,70,229,0.06)',
          },
        },
      },
    },

    MuiPopover: {
      styleOverrides: {
        paper: {
          borderRadius: 12,
          border: `1px solid ${isDark ? 'rgba(241,245,249,0.08)' : '#E2E8F0'}`,
        },
      },
    },

    MuiFormHelperText: {
      styleOverrides: {
        root: { fontSize: '0.75rem', marginLeft: 0, marginTop: 4 },
      },
    },

    MuiCircularProgress: {
      styleOverrides: {
        root: { animationDuration: '0.8s' },
      },
    },

    MuiSkeleton: {
      defaultProps: { animation: 'wave' },
      styleOverrides: {
        root: { borderRadius: 8 },
        wave: {
          '&::after': {
            background: isDark
              ? 'linear-gradient(90deg, transparent, rgba(255,255,255,0.04), transparent)'
              : 'linear-gradient(90deg, transparent, rgba(0,0,0,0.03), transparent)',
          },
        },
      },
    },
  };
}
