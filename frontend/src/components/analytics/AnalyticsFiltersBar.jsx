/**
 * @fileoverview AnalyticsFiltersBar — date range, department, and employee filters
 * for the Analytics Dashboard.
 *
 * Privileged roles (ADMIN, HR, MANAGER) see all filters.
 * EMPLOYEE sees only the date range (their data is auto-scoped server-side).
 */

import React from 'react';
import {
  Box,
  Button,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
  useTheme,
} from '@mui/material';
import FilterListRoundedIcon from '@mui/icons-material/FilterListRounded';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';

// ── Preset date range options ─────────────────────────────────────────────────

const PRESETS = [
  { label: '7 days', days: 7 },
  { label: '30 days', days: 30 },
  { label: '90 days', days: 90 },
  { label: '6 months', days: 180 },
  { label: '1 year', days: 365 },
];

/**
 * Formats a Date to YYYY-MM-DD for use with input[type=date].
 *
 * @param {Date} d
 * @returns {string}
 */
function toDateInput(d) {
  return d.toISOString().slice(0, 10);
}

/**
 * @typedef {Object} AnalyticsFiltersBarProps
 * @property {Object}        filters            - Current filter state.
 * @property {string}        [filters.from]
 * @property {string}        [filters.to]
 * @property {string}        [filters.departmentId]
 * @property {Function}      onFiltersChange    - Called with new filter object.
 * @property {Function}      [onRefresh]        - Called to force-refresh all queries.
 * @property {boolean}       [isPrivileged]     - True for ADMIN/HR/MANAGER.
 * @property {Array}         [departments]      - List of { id, name } objects.
 * @property {boolean}       [isFetching]
 */

/**
 * Filter bar for the analytics dashboard.
 *
 * @param {AnalyticsFiltersBarProps} props
 * @returns {JSX.Element}
 */
export default function AnalyticsFiltersBar({
  filters,
  onFiltersChange,
  onRefresh,
  isPrivileged = false,
  departments = [],
  isFetching = false,
}) {
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const handlePreset = (days) => {
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - days);
    onFiltersChange({ ...filters, from: toDateInput(from), to: toDateInput(to) });
  };

  const handleChange = (field) => (e) => {
    onFiltersChange({ ...filters, [field]: e.target.value || undefined });
  };

  const handleReset = () => {
    const to = new Date();
    const from = new Date();
    from.setDate(from.getDate() - 30);
    onFiltersChange({ from: toDateInput(from), to: toDateInput(to) });
  };

  return (
    <Box
      sx={{
        p: { xs: 2, sm: 3 },
        mb: 3,
        borderRadius: 3,
        bgcolor: isDark ? 'rgba(19,28,46,0.6)' : 'rgba(255,255,255,0.7)',
        border: `1px solid ${isDark ? 'rgba(240,237,230,0.06)' : 'rgba(235,230,218,0.6)'}`,
        backdropFilter: 'blur(8px)',
      }}
      role="region"
      aria-label="Analytics filters"
    >
      {/* Header */}
      <Stack
        direction="row"
        alignItems="center"
        justifyContent="space-between"
        mb={2}
        flexWrap="wrap"
        gap={1}
      >
        <Stack direction="row" alignItems="center" gap={1}>
          <FilterListRoundedIcon sx={{ color: 'primary.main', fontSize: 20 }} />
          <Typography variant="subtitle2" fontWeight={600}>
            Filters
          </Typography>
        </Stack>

        <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
          {/* Quick-range chips */}
          {PRESETS.map((p) => (
            <Chip
              key={p.days}
              label={p.label}
              size="small"
              onClick={() => handlePreset(p.days)}
              sx={{
                cursor: 'pointer',
                fontWeight: 500,
                fontSize: '0.7rem',
                '&:hover': { bgcolor: 'primary.main', color: 'white' },
              }}
            />
          ))}

          <Button
            size="small"
            startIcon={<RefreshRoundedIcon />}
            onClick={onRefresh}
            disabled={isFetching}
            variant="outlined"
            sx={{ ml: 1, minWidth: 80 }}
          >
            Refresh
          </Button>
        </Stack>
      </Stack>

      {/* Filter controls */}
      <Stack direction={{ xs: 'column', sm: 'row' }} gap={2} flexWrap="wrap" alignItems="flex-end">
        <TextField
          label="From"
          type="date"
          size="small"
          value={filters.from || ''}
          onChange={handleChange('from')}
          InputLabelProps={{ shrink: true }}
          inputProps={{ 'aria-label': 'Start date' }}
          sx={{ minWidth: 150 }}
        />
        <TextField
          label="To"
          type="date"
          size="small"
          value={filters.to || ''}
          onChange={handleChange('to')}
          InputLabelProps={{ shrink: true }}
          inputProps={{ 'aria-label': 'End date' }}
          sx={{ minWidth: 150 }}
        />

        {isPrivileged && departments.length > 0 && (
          <FormControl size="small" sx={{ minWidth: 180 }}>
            <InputLabel id="dept-filter-label">Department</InputLabel>
            <Select
              labelId="dept-filter-label"
              label="Department"
              value={filters.departmentId || ''}
              onChange={handleChange('departmentId')}
              inputProps={{ 'aria-label': 'Department filter' }}
            >
              <MenuItem value="">All departments</MenuItem>
              {departments.map((d) => (
                <MenuItem key={d.id} value={d.id}>
                  {d.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        )}

        <Button
          size="small"
          variant="text"
          onClick={handleReset}
          sx={{ minWidth: 60, color: 'text.secondary' }}
        >
          Reset
        </Button>
      </Stack>
    </Box>
  );
}
