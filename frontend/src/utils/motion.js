/**
 * @fileoverview Reusable motion utilities for PeopleCore HR.
 *
 * Centralised animation helpers so that transitions remain consistent and
 * can respect the `prefers-reduced-motion` media query.
 *
 * Usage (sx prop):
 *   sx={{ ...fadeInUp(0.18, 0), ...hoverLift }}
 *
 * All values are in seconds unless noted.
 */

// ── Keyframe names ────────────────────────────────────────────────────────────
// These keyframes are already defined globally in theme/components.js.
// We reference them by name so there is no duplication.

/** Standard easing for most transitions */
export const EASE_OUT = 'cubic-bezier(0.0, 0.0, 0.2, 1)';
/** Structural width/height easing */
export const EASE_STANDARD = 'cubic-bezier(0.4, 0.0, 0.2, 1)';

// ── Page-level entrance animation ─────────────────────────────────────────────

/**
 * Returns sx properties for a staggered fade-in-up entrance.
 *
 * @param {number} [duration=0.22]  Animation duration in seconds.
 * @param {number} [delay=0]        Animation delay in seconds.
 * @returns {object}                MUI sx-compatible style object.
 */
export function fadeInUp(duration = 0.22, delay = 0) {
  return {
    animation: `fadeUp ${duration}s ease-out ${delay}s both`,
    '@media (prefers-reduced-motion: reduce)': { animation: 'none', opacity: 1 },
  };
}

/**
 * Returns sx properties for a simple fade-in.
 *
 * @param {number} [duration=0.2]
 * @param {number} [delay=0]
 * @returns {object}
 */
export function fadeIn(duration = 0.2, delay = 0) {
  return {
    animation: `fadeIn ${duration}s ease-out ${delay}s both`,
    '@media (prefers-reduced-motion: reduce)': { animation: 'none', opacity: 1 },
  };
}

/**
 * Returns sx properties for a scale-in entrance.
 *
 * @param {number} [duration=0.2]
 * @param {number} [delay=0]
 * @returns {object}
 */
export function scaleIn(duration = 0.2, delay = 0) {
  return {
    animation: `scaleIn ${duration}s ease-out ${delay}s both`,
    '@media (prefers-reduced-motion: reduce)': { animation: 'none', opacity: 1 },
  };
}

// ── Card hover effects ─────────────────────────────────────────────────────────

/**
 * Subtle hover lift for interactive cards.
 * Adds a translateY(-2px) and shadow increase on hover.
 */
export const hoverLift = {
  transition: 'transform 0.2s ease, box-shadow 0.2s ease',
  '&:hover': {
    transform: 'translateY(-2px)',
    boxShadow: '0 8px 24px rgba(26,35,66,0.10)',
  },
  '@media (prefers-reduced-motion: reduce)': {
    transition: 'none',
    '&:hover': { transform: 'none' },
  },
};

/**
 * Stronger hover lift for prominent cards (e.g., KPI cards).
 */
export const hoverLiftStrong = {
  transition: 'transform 0.2s ease, box-shadow 0.2s ease',
  '&:hover': {
    transform: 'translateY(-4px)',
    boxShadow: '0 12px 32px rgba(26,35,66,0.13)',
  },
  '@media (prefers-reduced-motion: reduce)': {
    transition: 'none',
    '&:hover': { transform: 'none' },
  },
};

// ── Button press ───────────────────────────────────────────────────────────────

/**
 * Active/press state for buttons (already in theme but exported for custom use).
 */
export const buttonPress = {
  transition: 'transform 0.15s ease',
  '&:active': { transform: 'scale(0.97)' },
};

// ── Stagger helpers ────────────────────────────────────────────────────────────

/**
 * Returns the delay (in seconds) for a staggered item at position `index`.
 *
 * @param {number} index
 * @param {number} [base=0.04]  Increment between items in seconds.
 * @param {number} [start=0]    Starting delay in seconds.
 * @returns {number}
 */
export function staggerDelay(index, base = 0.04, start = 0) {
  return start + index * base;
}
