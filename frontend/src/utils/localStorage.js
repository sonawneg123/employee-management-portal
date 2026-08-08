/**
 * @fileoverview localStorage utility — type-safe read/write/remove helpers.
 *
 * All interactions with {@code window.localStorage} are centralised here so
 * that the rest of the application never touches localStorage directly and
 * so that error handling is consistent across the app.
 */

/**
 * Persists a serialisable value under the given key.
 *
 * @template T
 * @param {string} key   - The storage key.
 * @param {T}      value - The value to persist. Must be JSON-serialisable.
 * @returns {void}
 */
export function setItem(key, value) {
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch (err) {
    console.error(`[localStorage] Failed to set "${key}":`, err);
  }
}

/**
 * Retrieves and deserialises a value from storage.
 *
 * @template T
 * @param {string} key                - The storage key.
 * @param {T}      [defaultValue=null] - Value returned when the key is absent
 *                                       or the stored JSON is malformed.
 * @returns {T} The stored value or {@code defaultValue}.
 */
export function getItem(key, defaultValue = null) {
  try {
    const raw = localStorage.getItem(key);
    if (raw === null) return defaultValue;
    return /** @type {T} */ (JSON.parse(raw));
  } catch (err) {
    console.error(`[localStorage] Failed to get "${key}":`, err);
    return defaultValue;
  }
}

/**
 * Removes a specific key from storage.
 *
 * @param {string} key - The storage key to remove.
 * @returns {void}
 */
export function removeItem(key) {
  try {
    localStorage.removeItem(key);
  } catch (err) {
    console.error(`[localStorage] Failed to remove "${key}":`, err);
  }
}

/**
 * Clears all keys from localStorage.
 * Should only be called on explicit user logout.
 *
 * @returns {void}
 */
export function clearAll() {
  try {
    localStorage.clear();
  } catch (err) {
    console.error('[localStorage] Failed to clear storage:', err);
  }
}
