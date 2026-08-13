/**
 * @fileoverview Vitest global setup file.
 *
 * Imported via the {@code setupFiles} option in {@code vite.config.js}.
 *
 * Responsibilities:
 * 1. Extends Vitest's {@code expect} with all jest-dom matchers.
 * 2. Bootstraps Day.js plugins globally so every test environment has the
 *    same plugin set as the production app — preventing "cannot read property
 *    'add' of undefined" errors when modules like {@code dateUtils.js} are
 *    imported in test environments where module isolation would otherwise
 *    prevent side-effects from running in the correct order.
 * 3. Configures Testing Library to skip aria-hidden elements in text queries,
 *    so getByText / findByText do not match elements intentionally hidden
 *    from assistive technology (e.g., decorative button label spans).
 */

import '@testing-library/jest-dom';
import { configure } from '@testing-library/dom';

// ── Testing Library global configuration ─────────────────────────────────────
// Extend the default ignore selector to skip elements that are aria-hidden.
// This mirrors how screen readers behave: aria-hidden content is invisible
// to accessibility queries, and our text queries should reflect that.
configure({ defaultIgnore: "script, style, [aria-hidden='true']" });

// ── Day.js global plugin setup ────────────────────────────────────────────────
// Must mirror every dayjs.extend() call in the source tree so that the shared
// dayjs singleton is fully configured before any test runs.

import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import localizedFormat from 'dayjs/plugin/localizedFormat';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';
import isSameOrBefore from 'dayjs/plugin/isSameOrBefore';
import isBetween from 'dayjs/plugin/isBetween';

dayjs.extend(relativeTime);
dayjs.extend(localizedFormat);
dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.extend(isSameOrBefore);
dayjs.extend(isBetween);
