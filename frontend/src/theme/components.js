/**
 * @fileoverview Material UI component overrides — PeopleCore HR SaaS design system.
 *
 * Design tokens (premium SaaS reference):
 * - Cards: 20px radius, white background, soft shadow, minimal border
 * - Inputs: 12px radius, soft focus ring (gold/navy)
 * - Buttons: 10–50px radius (pill for primary CTAs), no elevation on contained
 * - Dark navy primary, gold secondary accent
 * - Warm cream backgrounds
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
            ? 'rgba(255,255,255,0.12) transparent'
            : 'rgba(26,35,66,0.15) transparent',
        },
        '@keyframes fadeUp': {
          from: { opacity: 0, transform: 'translateY(16px)' },
          to: { opacity: 1, transform: 'translateY(0)' },
        },
        '@keyframes fadeIn': {
          from: { opacity: 0 },
          to: { opacity: 1 },
        },
        '@keyframes scaleIn': {
          from: { opacity: 0, transform: 'scale(0.96)' },
          to: { opacity: 1, transform: 'scale(1)' },
        },
        '@keyframes slideInRight': {
          from: { opacity: 0, transform: 'translateX(16px)' },
          to: { opacity: 1, transform: 'translateX(0)' },
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
          borderRadius: 50,
          padding: '10px 24px',
          fontWeight: 600,
          transition: 'all 0.18s ease',
          '&:active': { transform: 'scale(0.97)' },
          letterSpacing: '0.01em',
        },
        sizeSmall: { padding: '7px 16px', fontSize: '0.8125rem', borderRadius: 50 },
        sizeLarge: { padding: '14px 32px', fontSize: '0.9375rem', borderRadius: 50 },
        contained: {
          boxShadow: 'none',
          background: isDark ? '#8B9FD4' : '#1A2342',
          color: '#ffffff',
          '&:hover': {
            background: isDark ? '#B0C0E8' : '#2D3A6B',
            boxShadow: isDark
              ? '0 6px 20px rgba(139,159,212,0.35)'
              : '0 6px 20px rgba(26,35,66,0.3)',
            transform: 'translateY(-1px)',
          },
        },
        containedSecondary: {
          background: '#F5C518',
          color: '#1A2342',
          '&:hover': {
            background: '#FFD966',
            boxShadow: '0 6px 20px rgba(245,197,24,0.4)',
            transform: 'translateY(-1px)',
          },
        },
        containedWarning: {
          color: '#fff',
          background: 'linear-gradient(135deg, #F59E0B, #D97706)',
          '&:hover': {
            background: 'linear-gradient(135deg, #D97706, #B45309)',
            boxShadow: '0 4px 16px rgba(245,158,11,0.35)',
          },
        },
        outlined: {
          borderWidth: '1.5px',
          borderColor: isDark ? 'rgba(240,237,230,0.2)' : '#D1CBB8',
          color: isDark ? '#F0EDE6' : '#1A2342',
          '&:hover': {
            borderWidth: '1.5px',
            borderColor: isDark ? 'rgba(240,237,230,0.4)' : '#1A2342',
            background: isDark ? 'rgba(240,237,230,0.05)' : 'rgba(26,35,66,0.04)',
          },
        },
        text: {
          color: isDark ? '#8B9FD4' : '#1A2342',
          '&:hover': {
            background: isDark ? 'rgba(139,159,212,0.08)' : 'rgba(26,35,66,0.05)',
          },
        },
      },
    },

    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          borderRadius: 20,
          border: `1px solid ${isDark ? 'rgba(240,237,230,0.06)' : '#EBE6DA'}`,
          transition: 'box-shadow 0.22s ease, transform 0.22s ease',
          backgroundImage: 'none',
          backgroundColor: isDark ? '#131C2E' : '#FFFFFF',
          boxShadow: isDark
            ? '0 2px 8px rgba(0,0,0,0.35)'
            : '0 2px 12px rgba(26,35,66,0.05), 0 1px 4px rgba(26,35,66,0.03)',
          '&:hover': {
            boxShadow: isDark ? '0 8px 32px rgba(0,0,0,0.5)' : '0 8px 32px rgba(26,35,66,0.1)',
          },
        },
      },
    },

    MuiPaper: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: { backgroundImage: 'none' },
        rounded: { borderRadius: 20 },
        elevation1: {
          boxShadow: isDark
            ? '0 1px 3px rgba(0,0,0,0.4), 0 1px 2px rgba(0,0,0,0.3)'
            : '0 1px 4px rgba(26,35,66,0.06), 0 1px 2px rgba(26,35,66,0.04)',
        },
        elevation2: {
          boxShadow: isDark ? '0 4px 12px rgba(0,0,0,0.5)' : '0 4px 18px rgba(26,35,66,0.08)',
        },
        elevation4: {
          boxShadow: isDark ? '0 8px 32px rgba(0,0,0,0.6)' : '0 8px 36px rgba(26,35,66,0.1)',
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
          borderRadius: 12,
          backgroundColor: isDark ? 'rgba(255,255,255,0.03)' : '#FDFAF4',
          transition: 'box-shadow 0.18s ease',
          '&.Mui-focused': {
            boxShadow: isDark ? '0 0 0 3px rgba(139,159,212,0.2)' : '0 0 0 3px rgba(26,35,66,0.1)',
          },
          '&:hover .MuiOutlinedInput-notchedOutline': {
            borderColor: isDark ? 'rgba(240,237,230,0.35)' : '#1A2342',
          },
        },
        notchedOutline: {
          borderColor: isDark ? 'rgba(240,237,230,0.12)' : '#E8E3D8',
          borderWidth: '1.5px',
        },
        input: {
          padding: '10px 14px',
          '&::placeholder': { opacity: 0.45 },
        },
      },
    },

    MuiInputLabel: {
      defaultProps: { shrink: true },
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: '0.8125rem',
          color: isDark ? 'rgba(240,237,230,0.6)' : '#374151',
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
          borderRadius: 8,
          fontWeight: 600,
          fontSize: '0.75rem',
          letterSpacing: '0.01em',
          height: 26,
        },
        sizeSmall: { height: 22, fontSize: '0.6875rem', borderRadius: 6 },
        sizeMedium: { height: 30 },
      },
    },

    MuiTableHead: {
      styleOverrides: {
        root: {
          '& .MuiTableCell-head': {
            fontWeight: 700,
            fontSize: '0.7rem',
            letterSpacing: '0.08em',
            textTransform: 'uppercase',
            color: isDark ? 'rgba(240,237,230,0.5)' : '#7A7468',
            backgroundColor: isDark ? 'rgba(255,255,255,0.02)' : '#FAF7F0',
            borderBottom: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA'}`,
          },
        },
      },
    },

    MuiTableCell: {
      styleOverrides: {
        root: {
          fontSize: '0.875rem',
          borderColor: isDark ? 'rgba(240,237,230,0.06)' : '#F0EBE0',
          padding: '12px 16px',
        },
      },
    },

    MuiTableRow: {
      styleOverrides: {
        root: {
          '&:hover': {
            backgroundColor: isDark ? 'rgba(255,255,255,0.03)' : 'rgba(26,35,66,0.025)',
          },
          transition: 'background-color 0.15s ease',
        },
      },
    },

    MuiTableContainer: {
      styleOverrides: {
        root: {
          borderRadius: 16,
          border: `1px solid ${isDark ? 'rgba(240,237,230,0.06)' : '#EBE6DA'}`,
          overflow: 'hidden',
        },
      },
    },

    MuiListItemButton: {
      styleOverrides: {
        root: {
          borderRadius: 12,
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
          backgroundColor: isDark ? '#2D3A6B' : '#1A2342',
        },
        arrow: {
          color: isDark ? '#2D3A6B' : '#1A2342',
        },
      },
    },

    MuiDialog: {
      styleOverrides: {
        paper: {
          borderRadius: 24,
          border: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA'}`,
          boxShadow: isDark ? '0 25px 60px rgba(0,0,0,0.75)' : '0 25px 60px rgba(26,35,66,0.14)',
          backgroundColor: isDark ? '#131C2E' : '#FFFFFF',
        },
      },
    },

    MuiDialogTitle: {
      styleOverrides: {
        root: {
          fontWeight: 700,
          fontSize: '1.0625rem',
          padding: '22px 28px 8px',
          color: isDark ? '#F0EDE6' : '#1A2342',
        },
      },
    },

    MuiDialogContent: {
      styleOverrides: {
        root: { padding: '8px 28px 8px' },
      },
    },

    MuiDialogActions: {
      styleOverrides: {
        root: { padding: '16px 28px 22px', gap: 8 },
      },
    },

    MuiAlert: {
      styleOverrides: {
        root: {
          borderRadius: 14,
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
        bar: { borderRadius: 999 },
      },
    },

    MuiBreadcrumbs: {
      styleOverrides: {
        root: { fontSize: '0.8125rem' },
      },
    },

    MuiDrawer: {
      styleOverrides: {
        paper: {
          backgroundImage: 'none',
          backgroundColor: isDark ? '#0C1220' : '#FFFFFF',
        },
      },
    },

    MuiAppBar: {
      styleOverrides: {
        root: {
          backgroundImage: 'none',
          backgroundColor: isDark ? '#0C1220' : '#FFFFFF',
        },
      },
    },

    MuiDivider: {
      styleOverrides: {
        root: {
          borderColor: isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA',
        },
      },
    },

    MuiTab: {
      styleOverrides: {
        root: {
          fontWeight: 600,
          fontSize: '0.875rem',
          textTransform: 'none',
          minHeight: 44,
          padding: '8px 20px',
          borderRadius: '10px 10px 0 0',
          color: isDark ? 'rgba(240,237,230,0.55)' : '#7A7468',
          '&.Mui-selected': {
            color: isDark ? '#F0EDE6' : '#1A2342',
          },
        },
      },
    },

    MuiTabs: {
      styleOverrides: {
        root: {
          minHeight: 44,
        },
        indicator: {
          height: 2,
          borderRadius: 2,
          backgroundColor: isDark ? '#8B9FD4' : '#1A2342',
        },
      },
    },

    MuiSwitch: {
      styleOverrides: {
        root: {
          '& .MuiSwitch-thumb': { boxShadow: 'none' },
          '& .Mui-checked': {
            '& .MuiSwitch-thumb': {
              backgroundColor: '#F5C518',
            },
          },
        },
        track: {
          '$checked$checked + &': {
            backgroundColor: '#F5C518',
          },
        },
      },
    },

    MuiMenu: {
      styleOverrides: {
        paper: {
          borderRadius: 16,
          border: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA'}`,
          boxShadow: isDark ? '0 8px 32px rgba(0,0,0,0.6)' : '0 8px 36px rgba(26,35,66,0.1)',
          backgroundImage: 'none',
          backgroundColor: isDark ? '#131C2E' : '#FFFFFF',
        },
      },
    },

    MuiMenuItem: {
      styleOverrides: {
        root: {
          fontSize: '0.875rem',
          fontWeight: 500,
          borderRadius: 10,
          margin: '2px 6px',
          padding: '8px 12px',
          '&:hover': {
            backgroundColor: isDark ? 'rgba(240,237,230,0.06)' : 'rgba(26,35,66,0.05)',
          },
          '&.Mui-selected': {
            backgroundColor: isDark ? 'rgba(26,35,66,0.5)' : 'rgba(26,35,66,0.06)',
            '&:hover': {
              backgroundColor: isDark ? 'rgba(26,35,66,0.65)' : 'rgba(26,35,66,0.1)',
            },
          },
        },
      },
    },

    MuiPopover: {
      styleOverrides: {
        paper: {
          borderRadius: 16,
          border: `1px solid ${isDark ? 'rgba(240,237,230,0.07)' : '#EBE6DA'}`,
          backgroundColor: isDark ? '#131C2E' : '#FFFFFF',
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
        colorPrimary: {
          color: isDark ? '#8B9FD4' : '#1A2342',
        },
      },
    },

    MuiSkeleton: {
      defaultProps: { animation: 'wave' },
      styleOverrides: {
        root: {
          borderRadius: 10,
          backgroundColor: isDark ? 'rgba(240,237,230,0.06)' : 'rgba(26,35,66,0.05)',
        },
        wave: {
          '&::after': {
            background: isDark
              ? 'linear-gradient(90deg, transparent, rgba(255,255,255,0.04), transparent)'
              : 'linear-gradient(90deg, transparent, rgba(245,197,24,0.06), transparent)',
          },
        },
      },
    },

    MuiBadge: {
      styleOverrides: {
        badge: {
          fontWeight: 700,
          fontSize: '0.65rem',
        },
      },
    },

    MuiFab: {
      styleOverrides: {
        root: {
          boxShadow: isDark ? '0 8px 24px rgba(0,0,0,0.5)' : '0 8px 24px rgba(26,35,66,0.25)',
          '&:hover': {
            boxShadow: isDark ? '0 12px 36px rgba(0,0,0,0.65)' : '0 12px 36px rgba(26,35,66,0.35)',
          },
        },
      },
    },
  };
}
