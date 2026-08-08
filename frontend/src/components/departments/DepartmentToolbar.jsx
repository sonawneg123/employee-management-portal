/**
 * @fileoverview DepartmentToolbar — top action bar for the department list.
 *
 * Contains the search input, sort filters, and action buttons (Add, Refresh, Export CSV).
 */

import React from 'react';
import {
  Box,
  Button,
  Divider,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import AddIcon      from '@mui/icons-material/Add';
import RefreshIcon  from '@mui/icons-material/Refresh';
import DownloadIcon from '@mui/icons-material/Download';
import DepartmentSearch  from './DepartmentSearch';
import DepartmentFilters from './DepartmentFilters';

/**
 * @typedef {Object} DepartmentToolbarProps
 * @property {string}            search
 * @property {string}            sort
 * @property {'asc'|'desc'}      direction
 * @property {number}            totalElements
 * @property {boolean}           isFetching
 * @property {boolean}           canCreate
 * @property {(v: string) => void} onSearchChange
 * @property {(v: string) => void} onSortChange
 * @property {(v: string) => void} onDirectionChange
 * @property {() => void}        onAdd
 * @property {() => void}        onRefresh
 * @property {() => void}        onExport
 * @property {() => void}        onClearSearch
 */

/**
 * Toolbar above the department table.
 *
 * @param {DepartmentToolbarProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentToolbar({
  search,
  sort,
  direction,
  totalElements,
  isFetching,
  canCreate,
  onSearchChange,
  onSortChange,
  onDirectionChange,
  onAdd,
  onRefresh,
  onExport,
  onClearSearch,
}) {
  return (
    <Box sx={{ p: 2 }}>
      {/* Row 1: search + filters */}
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1.5, alignItems: 'center', mb: 1.5 }}>
        <DepartmentSearch
          value={search}
          onSearch={onSearchChange}
          disabled={isFetching}
        />
        <DepartmentFilters
          sort={sort}
          direction={direction}
          onSortChange={onSortChange}
          onDirectionChange={onDirectionChange}
          disabled={isFetching}
        />
        {search && (
          <Button
            size="small"
            variant="text"
            color="secondary"
            onClick={onClearSearch}
            aria-label="Clear search"
          >
            Clear
          </Button>
        )}
      </Box>

      <Divider />

      {/* Row 2: count + actions */}
      <Box
        sx={{
          display:        'flex',
          alignItems:     'center',
          justifyContent: 'space-between',
          mt: 1.5,
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <Typography variant="body2" color="text.secondary">
          {totalElements != null
            ? `${totalElements} department${totalElements !== 1 ? 's' : ''}`
            : ''}
        </Typography>

        <Box sx={{ display: 'flex', gap: 1 }}>
          <Tooltip title="Refresh">
            <IconButton
              size="small"
              onClick={onRefresh}
              disabled={isFetching}
              aria-label="Refresh department list"
              sx={{
                animation: isFetching ? 'spin 1s linear infinite' : 'none',
                '@keyframes spin': {
                  '0%':   { transform: 'rotate(0deg)' },
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
              aria-label="Export departments to CSV"
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
              aria-label="Add new department"
            >
              Add Department
            </Button>
          )}
        </Box>
      </Box>
    </Box>
  );
}
