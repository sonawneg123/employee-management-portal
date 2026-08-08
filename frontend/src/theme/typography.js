/**
 * @fileoverview Material UI typography configuration.
 *
 * Uses the Inter font loaded from Google Fonts in {@code index.html}.
 * All font sizes follow a consistent modular scale.
 */

/** @type {import('@mui/material').ThemeOptions['typography']} */
const typography = {
  fontFamily: [
    'Inter',
    '-apple-system',
    'BlinkMacSystemFont',
    '"Segoe UI"',
    'Roboto',
    '"Helvetica Neue"',
    'Arial',
    'sans-serif',
  ].join(','),

  h1: { fontSize: '2.25rem', fontWeight: 700, lineHeight: 1.2 },
  h2: { fontSize: '1.875rem', fontWeight: 700, lineHeight: 1.25 },
  h3: { fontSize: '1.5rem',   fontWeight: 600, lineHeight: 1.3 },
  h4: { fontSize: '1.25rem',  fontWeight: 600, lineHeight: 1.35 },
  h5: { fontSize: '1.125rem', fontWeight: 600, lineHeight: 1.4 },
  h6: { fontSize: '1rem',     fontWeight: 600, lineHeight: 1.5 },

  subtitle1: { fontSize: '0.9375rem', fontWeight: 500, lineHeight: 1.6 },
  subtitle2: { fontSize: '0.875rem',  fontWeight: 500, lineHeight: 1.6 },

  body1: { fontSize: '0.9375rem', fontWeight: 400, lineHeight: 1.6 },
  body2: { fontSize: '0.875rem',  fontWeight: 400, lineHeight: 1.6 },

  button: { fontSize: '0.875rem', fontWeight: 600, textTransform: 'none', letterSpacing: '0.01em' },

  caption:   { fontSize: '0.75rem', fontWeight: 400, lineHeight: 1.5 },
  overline:  { fontSize: '0.6875rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.08em' },
};

export default typography;
