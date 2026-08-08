/**
 * @fileoverview DepartmentSearch — debounced search input for the department list.
 */

import React, { useEffect, useRef, useState } from 'react';
import { IconButton, InputAdornment, TextField, Tooltip } from '@mui/material';
import SearchIcon from '@mui/icons-material/Search';
import ClearIcon  from '@mui/icons-material/Clear';
import { DEPT_SEARCH_DEBOUNCE_MS } from '@/constants/departmentConstants';

/**
 * @typedef {Object} DepartmentSearchProps
 * @property {string}                value
 * @property {(val: string) => void} onSearch
 * @property {string}                [placeholder]
 * @property {boolean}               [disabled]
 */

/**
 * Debounced search input for the department list toolbar.
 *
 * @param {DepartmentSearchProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentSearch({
  value,
  onSearch,
  placeholder = 'Search departments…',
  disabled = false,
}) {
  const [local, setLocal] = useState(value);
  const timer = useRef(null);

  useEffect(() => { setLocal(value); }, [value]);

  const handleChange = (e) => {
    const v = e.target.value;
    setLocal(v);
    clearTimeout(timer.current);
    timer.current = setTimeout(() => onSearch(v), DEPT_SEARCH_DEBOUNCE_MS);
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
          endAdornment: local ? (
            <InputAdornment position="end">
              <Tooltip title="Clear search">
                <IconButton size="small" onClick={handleClear} aria-label="Clear search" edge="end">
                  <ClearIcon fontSize="small" />
                </IconButton>
              </Tooltip>
            </InputAdornment>
          ) : null,
        },
      }}
      aria-label="Search departments"
      inputProps={{ 'aria-label': 'Department search input' }}
    />
  );
}
