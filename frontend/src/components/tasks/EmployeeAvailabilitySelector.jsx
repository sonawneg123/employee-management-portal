/**
 * @fileoverview EmployeeAvailabilitySelector — employee dropdown with live
 * availability and workload indicators.
 *
 * Phase 6C: New component.
 *
 * Each option shows:
 *  - Checked-in / checked-out badge
 *  - Active task count
 *  - Overdue count
 *  - Workload warning when activeTasks >= 6
 */

import React from 'react';
import {
  Box,
  Chip,
  CircularProgress,
  FormControl,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  Tooltip,
  Typography,
} from '@mui/material';
import WarningAmberRoundedIcon from '@mui/icons-material/WarningAmberRounded';

/**
 * Employee selection dropdown with availability/workload info.
 *
 * @param {{
 *   employees: Array,
 *   value: string,
 *   onChange: (value: string) => void,
 *   error?: string,
 *   loading?: boolean,
 *   label?: string,
 * }} props
 * @returns {JSX.Element}
 */
export default function EmployeeAvailabilitySelector({
  employees = [],
  value = '',
  onChange,
  error,
  loading = false,
  label = 'Assign To',
}) {
  const labelId = 'emp-avail-selector-label';

  return (
    <FormControl fullWidth error={Boolean(error)}>
      <InputLabel id={labelId}>{label}</InputLabel>
      <Select
        labelId={labelId}
        label={label}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        disabled={loading}
        renderValue={(selected) => {
          if (!selected) return <em>Unassigned (Draft)</em>;
          const emp = employees.find((e) => e.employeeId === selected || e.id === selected);
          if (!emp) return selected;
          return emp.employeeName ?? emp.label ?? selected;
        }}
      >
        <MenuItem value="">
          <em>Unassigned (Draft)</em>
        </MenuItem>

        {loading && (
          <MenuItem disabled>
            <CircularProgress size={16} sx={{ mr: 1 }} /> Loading availability…
          </MenuItem>
        )}

        {!loading && employees.map((emp) => {
          const empId = emp.employeeId ?? emp.id;
          const empName = emp.employeeName ?? emp.label ?? empId;
          const checkedIn = emp.checkedIn ?? false;
          const activeTasks = emp.activeTasks ?? 0;
          const overdueCount = emp.overdueCount ?? 0;
          const isHighWorkload = activeTasks >= 6;

          return (
            <MenuItem key={empId} value={empId}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, width: '100%', flexWrap: 'wrap' }}>
                <Typography variant="body2" sx={{ flexGrow: 1, minWidth: 120 }}>
                  {empName}
                  {emp.employeeCode && (
                    <Typography component="span" variant="caption" color="text.secondary" sx={{ ml: 0.5 }}>
                      ({emp.employeeCode})
                    </Typography>
                  )}
                </Typography>

                {/* Check-in status */}
                <Tooltip title={checkedIn ? 'Currently checked in' : 'Not checked in — assignment blocked'}>
                  <Chip
                    label={checkedIn ? 'In' : 'Out'}
                    size="small"
                    color={checkedIn ? 'success' : 'error'}
                    variant="filled"
                    sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700 }}
                  />
                </Tooltip>

                {/* Active tasks count */}
                <Tooltip title={`${activeTasks} active task${activeTasks !== 1 ? 's' : ''}`}>
                  <Chip
                    label={`${activeTasks} active`}
                    size="small"
                    color={isHighWorkload ? 'warning' : 'default'}
                    variant="outlined"
                    sx={{ height: 18, fontSize: '0.65rem' }}
                  />
                </Tooltip>

                {/* Overdue count */}
                {overdueCount > 0 && (
                  <Tooltip title={`${overdueCount} overdue task${overdueCount !== 1 ? 's' : ''}`}>
                    <Chip
                      label={`${overdueCount} overdue`}
                      size="small"
                      color="error"
                      variant="outlined"
                      sx={{ height: 18, fontSize: '0.65rem' }}
                    />
                  </Tooltip>
                )}

                {/* High workload warning */}
                {isHighWorkload && (
                  <Tooltip title="High workload — this employee already has 6+ active tasks">
                    <WarningAmberRoundedIcon fontSize="small" color="warning" />
                  </Tooltip>
                )}
              </Box>
            </MenuItem>
          );
        })}
      </Select>
      {error && <FormHelperText>{error}</FormHelperText>}
    </FormControl>
  );
}
