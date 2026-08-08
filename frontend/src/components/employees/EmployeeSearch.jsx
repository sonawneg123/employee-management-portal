/**
 * @fileoverview EmployeeSearch — debounced search input for the employee list.
 *
 * Fires `onSearch` only after the user stops typing for
 * {@link SEARCH_DEBOUNCE_MS} milliseconds to avoid hammering the API.
 */

import React, { useEffect, useRef, useState } from 'react';
import { IconButton, InputAdornment, TextField, Tooltip } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon  from '@mui/icons-material/Clear';
import { SEARCH_DEBOUNCE_MS } from '@/constants/employeeConstants';

/**
 * @typedef {Object} EmployeeSearchProps
 * @property {string}            value      - Current search string (controlled).
 * @property {(val: string) => void} onSearch   - Called with the debounced value.
 * @property {string}            [placeholder]
 * @property {boolean}           [disabled]
 */

/**
 * Debounced search input for the employee list toolbar.
 *
 * @param {EmployeeSearchProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeSearch({
  value,
  onSearch,
  placeholder = 'Search employees…',
  disabled = false,
}) {
  const [localValue, setLocalValue] = useState(value);
  const timerRef = useRef(null);

  // Keep local state in sync when the parent resets the value
  useEffect(() => {
    setLocalValue(value);
  }, [value]);

  /**
   * Handles input change — updates local state immediately, then debounces
   * the upstream onSearch callback.
   *
   * @param {React.ChangeEvent<HTMLInputElement>} e
   */
  const handleChange = (e) => {
    const newVal = e.target.value;
    setLocalValue(newVal);
    clearTimeout(timerRef.current);
    timerRef.current = setTimeout(() => onSearch(newVal), SEARCH_DEBOUNCE_MS);
  };

  /**
   * Clears the input and immediately fires onSearch('').
   */
  const handleClear = () => {
    clearTimeout(timerRef.current);
    setLocalValue('');
    onSearch('');
  };

  // Clean up the timer on unmount
  useEffect(() => () => clearTimeout(timerRef.current), []);

  return (
    <TextField
      value={localValue}
      onChange={handleChange}
      placeholder={placeholder}
      disabled={disabled}
      size="small"
      sx={{ minWidth: { xs: '100%', sm: 280 } }}
      slotProps={{
        input: {
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon fontSize="small" color="action" />
            </InputAdornment>
          ),
          endAdornment: localValue ? (
            <InputAdornment position="end">
              <Tooltip title="Clear search">
                <IconButton
                  size="small"
                  onClick={handleClear}
                  aria-label="Clear search"
                  edge="end"
                >
                  <ClearIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </InputAdornment>
          ) : null,
        },
      }}
      aria-label="Search employees"
      inputProps={{ 'aria-label': 'Employee search input' }}
    />
  );
}
