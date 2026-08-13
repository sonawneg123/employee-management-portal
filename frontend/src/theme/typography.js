/**
 * @fileoverview Material UI typography configuration.
 *
 * Uses Inter (300–800 weights) loaded in index.html.
 * Scale: Page title → Section → Card title → Metric → Body → Supporting → Label.
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

  // Page title
  h1: { fontSize: '2rem', fontWeight: 800, lineHeight: 1.2, letterSpacing: '-0.02em' },
  // Section heading
  h2: { fontSize: '1.625rem', fontWeight: 700, lineHeight: 1.25, letterSpacing: '-0.015em' },
  // Card / panel heading
  h3: { fontSize: '1.375rem', fontWeight: 700, lineHeight: 1.3, letterSpacing: '-0.01em' },
  // Sub-section heading
  h4: { fontSize: '1.125rem', fontWeight: 700, lineHeight: 1.35, letterSpacing: '-0.005em' },
  // Card title
  h5: { fontSize: '1rem', fontWeight: 600, lineHeight: 1.4 },
  // Metric value
  h6: { fontSize: '0.9375rem', fontWeight: 600, lineHeight: 1.5 },

  subtitle1: { fontSize: '0.9375rem', fontWeight: 500, lineHeight: 1.6 },
  subtitle2: { fontSize: '0.875rem', fontWeight: 500, lineHeight: 1.6, color: 'inherit' },

  body1: { fontSize: '0.9375rem', fontWeight: 400, lineHeight: 1.6 },
  body2: { fontSize: '0.875rem', fontWeight: 400, lineHeight: 1.6 },

  button: {
    fontSize: '0.875rem',
    fontWeight: 600,
    textTransform: 'none',
    letterSpacing: '0.005em',
  },

  caption: { fontSize: '0.75rem', fontWeight: 400, lineHeight: 1.5 },
  overline: {
    fontSize: '0.6875rem',
    fontWeight: 700,
    textTransform: 'uppercase',
    letterSpacing: '0.1em',
  },
};

export default typography;
