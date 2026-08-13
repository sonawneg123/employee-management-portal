/**
 * @fileoverview LeaveSearch — debounced search input for the leave list.
 */

import React, { useEffect, useRef, useState } from 'react';
import { IconButton, InputAdornment, TextField, Tooltip } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon from '@mui/icons-material/Clear';
import { LEAVE_SEARCH_DEBOUNCE_MS } from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeaveSearchProps
 * @property {string}                value
 * @property {(val: string) => void} onSearch
 * @property {boolean}               [disabled]
 */

/**
 * Debounced search input for the leave list.
 *
 * @param {LeaveSearchProps} props
 * @returns {JSX.Element}
 */
export default function LeaveSearch({ value, onSearch, disabled = false }) {
  const [local, setLocal] = useState(value);
  const timer = useRef(null);

  useEffect(() => {
    setLocal(value);
  }, [value]);

  const handleChange = (e) => {
    const v = e.target.value;
    setLocal(v);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => onSearch(v), LEAVE_SEARCH_DEBOUNCE_MS);
  };

  const handleClear = () => {
    clearTimeout(timer.current);
    setLocal('');
    onSearch('');
  };
  useEffect(() => () => clearTimeout(timer.current), []);

  return (
    <TextField
      value={local}
      onChange={handleChange}
      placeholder="Search employee or reason…"
      disabled={disabled}
      size="small"
      sx={{ minWidth: { xs: '100%', sm: 260 } }}
      slotProps={{
        input: {
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon fontSize="small" color="action" />
            </InputAdornment>
          ),
          endAdornment: local ? (
            <InputAdornment position="end">
              <Tooltip title="Clear">
                <IconButton size="small" onClick={handleClear} aria-label="Clear search" edge="end">
                  <ClearIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </InputAdornment>
          ) : null,
        },
      }}
      aria-label="Search leave requests"
      inputProps={{ 'aria-label': 'Leave search input' }}
    />
  );
}
