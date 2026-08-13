/**
 * @fileoverview LeaveToolbar — action bar above the leave list.
 */

import React from 'react';
import {
  Box,
  Button,
  Divider,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Select,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon from '@mui/icons-material/Add';
import RefreshIcon from '@mui/icons-material/Refresh';
import DownloadIcon from '@mui/icons-material/Download';
import LeaveSearch from './LeaveSearch';
import LeaveFilters from './LeaveFilters';
import {
  LEAVE_SORT_OPTIONS,
  LEAVE_DEFAULT_SORT,
  LEAVE_DEFAULT_DIRECTION,
} from '@/constants/leaveConstants';

/**
 * @typedef {Object} LeaveToolbarProps
 * @property {string}            search
 * @property {string}            status
 * @property {string}            type
 * @property {string}            sort
 * @property {'asc'|'desc'}      direction
 * @property {number}            totalElements
 * @property {boolean}           isFetching
 * @property {boolean}           canCreate
 * @property {(v: string) => void} onSearchChange
 * @property {(v: string) => void} onStatusChange
 * @property {(v: string) => void} onTypeChange
 * @property {(v: string) => void} onSortChange
 * @property {(v: string) => void} onDirectionChange
 * @property {() => void}        onAdd
 * @property {() => void}        onRefresh
 * @property {() => void}        onExport
 * @property {() => void}        onClearFilters
 */

/**
 * Toolbar above the leave management table.
 *
 * @param {LeaveToolbarProps} props
 * @returns {JSX.Element}
 */
export default function LeaveToolbar({
  search,
  status,
  type,
  sort,
  direction,
  totalElements,
  isFetching,
  canCreate,
  onSearchChange,
  onStatusChange,
  onTypeChange,
  onSortChange,
  onDirectionChange,
  onAdd,
  onRefresh,
  onExport,
  onClearFilters,
}) {
  const hasActiveFilters = Boolean(search || status || type);

  return (
    <Box sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, alignItems: 'center', mb: 1.5 }}>
        <LeaveSearch value={search} onSearch={onSearchChange} disabled={isFetching} />
        <LeaveFilters
          status={status}
          type={type}
          onStatusChange={onStatusChange}
          onTypeChange={onTypeChange}
          disabled={isFetching}
        />
        {hasActiveFilters && (
          <Button size="small" variant="text" color="secondary" onClick={onClearFilters}>
            Clear
          </Button>
        )}

        <Box sx={{ display: 'flex', gap: 1, ml: { sm: 'auto' }, flexShrink: 0 }}>
          <FormControl size="small" sx={{ minWidth: 150 }} disabled={isFetching}>
            <InputLabel id="leave-sort-label">Sort</InputLabel>
            <Select
              labelId="leave-sort-label"
              value={sort ?? LEAVE_DEFAULT_SORT}
              label="Sort"
              onChange={(e) => onSortChange(e.target.value)}
              aria-label="Sort by"
            >
              {LEAVE_SORT_OPTIONS.map((o) => (
                <MenuItem key={o.value} value={o.value}>
                  {o.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 100 }} disabled={isFetching}>
            <InputLabel id="leave-dir-label">Order</InputLabel>
            <Select
              labelId="leave-dir-label"
              value={direction ?? LEAVE_DEFAULT_DIRECTION}
              label="Order"
              onChange={(e) => onDirectionChange(e.target.value)}
              aria-label="Sort direction"
            >
              <MenuItem value="asc">Oldest first</MenuItem>
              <MenuItem value="desc">Newest first</MenuItem>
            </Select>
          </FormControl>
        </Box>
      </Box>

      <Divider />

      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          mt: 1.5,
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <Typography variant="body2" color="text.secondary">
          {totalElements != null ? `${totalElements} request${totalElements !== 1 ? 's' : ''}` : ''}
        </Typography>

        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Refresh">
            <IconButton
              size="small"
              onClick={onRefresh}
              disabled={isFetching}
              aria-label="Refresh"
              sx={{
                animation: isFetching ? 'spin 1s linear infinite' : 'none',
                '@keyframes spin': {
                  '0%': { transform: 'rotate(0deg)' },
                  '100%': { transform: 'rotate(360deg)' },
                },
              }}
            >
              <RefreshIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          <Tooltip title="Export CSV">
            <IconButton
              size="small"
              onClick={onExport}
              disabled={isFetching || !totalElements}
              aria-label="Export CSV"
            >
              <DownloadIcon fontSize="small" />
            </IconButton>
          </Tooltip>

          {canCreate && (
            <Button
              variant="contained"
              size="small"
              startIcon={<AddIcon />}
              onClick={onAdd}
              aria-label="Request leave"
            >
              Request Leave
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );
}
