/**
 * @fileoverview EmployeeAvailabilitySelector — employee dropdown with live
 * availability and workload indicators.
 *
 * Phase 6C: New component.
 * Phase 6F: Added `currentAssigneeId` prop — that employee is shown as
 *           "Currently Assigned" and cannot be selected.
 * Phase 6G: Added disabled employee support + profile photo + priority-based
 *           unavailability reason display.
 *
 * Each option shows:
 *  - Profile photo or initials avatar
 *  - Checked-in / checked-out badge
 *  - Active task count
 *  - Overdue count
 *  - Workload warning when activeTasks >= 6
 *  - "Currently Assigned" label when the employee is the current assignee
 *  - "🔴 Disabled" when employee is administratively disabled
 *  - "🟠 Approved Leave Today" when on leave
 *  - "🔴 Checked Out" when checked out
 *  - "🔴 Not Checked In" when not checked in
 */

import React from 'react';
import {
  Avatar,
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
import LockRoundedIcon from '@mui/icons-material/LockRounded';
import BlockRoundedIcon from '@mui/icons-material/BlockRounded';

/**
 * Returns initials from a name string.
 *
 * @param {string} name
 * @returns {string}
 */
function getInitials(name) {
  if (!name) return '?';
  const parts = name.trim().split(/\s+/);
  if (parts.length === 1) return parts[0][0]?.toUpperCase() ?? '?';
  return ((parts[0][0] ?? '') + (parts[parts.length - 1][0] ?? '')).toUpperCase();
}

/**
 * Deterministic avatar color from name string.
 *
 * @param {string} name
 * @returns {string}
 */
function avatarColor(name) {
  const colors = ['#4F46E5', '#0EA5E9', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899'];
  if (!name) return colors[0];
  let hash = 0;
  for (let i = 0; i < name.length; i++) {
    hash = name.charCodeAt(i) + ((hash << 5) - hash);
  }
  return colors[Math.abs(hash) % colors.length];
}

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
 *   currentAssigneeId?: string,
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
  currentAssigneeId,
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

        {!loading &&
          employees.map((emp) => {
            const empId = emp.employeeId ?? emp.id;
            const empName = emp.employeeName ?? emp.label ?? empId;
            const checkedIn = emp.checkedIn ?? false;
            const onApprovedLeaveToday = emp.onApprovedLeaveToday ?? false;
            const isDisabled = emp.disabled ?? false;
            const profilePhotoUrl = emp.profilePhotoUrl ?? null;
            const unavailabilityReason = emp.unavailabilityReason ?? null;

            // availableToday from server, or derive locally as fallback
            const availableToday =
              emp.availableToday !== undefined
                ? emp.availableToday
                : !isDisabled && checkedIn && !onApprovedLeaveToday;
            const activeTasks = emp.activeTasks ?? 0;
            const overdueCount = emp.overdueCount ?? 0;
            const isHighWorkload = activeTasks >= 6;
            const isCurrentAssignee = currentAssigneeId && empId === currentAssigneeId;
            // Unavailable = disabled, on leave, or not currently checked in
            const isUnavailable = !isCurrentAssignee && !availableToday;

            // Profile avatar element
            const avatarEl = (
              <Avatar
                src={profilePhotoUrl || undefined}
                alt={empName}
                sx={{
                  width: 24,
                  height: 24,
                  fontSize: '0.6rem',
                  fontWeight: 700,
                  bgcolor: avatarColor(empName),
                  flexShrink: 0,
                  opacity: isUnavailable || isCurrentAssignee ? 0.5 : 1,
                }}
              >
                {!profilePhotoUrl && getInitials(empName)}
              </Avatar>
            );

            // Status chip content based on priority
            const statusChip = (() => {
              if (isCurrentAssignee) {
                return (
                  <Chip
                    label="Currently Assigned"
                    size="small"
                    color="default"
                    variant="outlined"
                    sx={{
                      height: 18,
                      fontSize: '0.65rem',
                      fontStyle: 'italic',
                      color: 'text.disabled',
                    }}
                  />
                );
              }
              if (isDisabled || unavailabilityReason === 'DISABLED') {
                return (
                  <Tooltip title="Employee is disabled — assignment not allowed">
                    <Chip
                      icon={<BlockRoundedIcon sx={{ fontSize: '0.75rem !important' }} />}
                      label="🔴 Disabled"
                      size="small"
                      color="error"
                      variant="filled"
                      sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700 }}
                    />
                  </Tooltip>
                );
              }
              if (onApprovedLeaveToday || unavailabilityReason === 'APPROVED_LEAVE') {
                return (
                  <Tooltip title="Employee has an approved leave today — assignment blocked">
                    <Chip
                      label="🟠 Approved Leave Today"
                      size="small"
                      color="warning"
                      variant="filled"
                      sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700 }}
                    />
                  </Tooltip>
                );
              }
              if (unavailabilityReason === 'CHECKED_OUT') {
                return (
                  <Tooltip title="Employee has already checked out today — assignment blocked">
                    <Chip
                      label="🔴 Checked Out"
                      size="small"
                      color="error"
                      variant="filled"
                      sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700 }}
                    />
                  </Tooltip>
                );
              }
              if (!checkedIn || unavailabilityReason === 'NOT_CHECKED_IN') {
                return (
                  <Tooltip title="Employee has not checked in today — assignment blocked">
                    <Chip
                      label="🔴 Not Checked In"
                      size="small"
                      color="error"
                      variant="outlined"
                      sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700 }}
                    />
                  </Tooltip>
                );
              }
              return (
                <Tooltip title="Currently checked in">
                  <Chip
                    label="🟢 Checked In"
                    size="small"
                    color="success"
                    variant="filled"
                    sx={{ height: 18, fontSize: '0.65rem', fontWeight: 700 }}
                  />
                </Tooltip>
              );
            })();

            return (
              <MenuItem
                key={empId}
                value={isCurrentAssignee ? '' : empId}
                disabled={isCurrentAssignee || isUnavailable}
                sx={
                  isCurrentAssignee || isUnavailable
                    ? {
                        opacity: 0.6,
                        cursor: 'not-allowed',
                        '&.Mui-disabled': { opacity: 0.6 },
                      }
                    : undefined
                }
                // Prevent selection via click when current assignee or unavailable
                onClick={
                  isCurrentAssignee || isUnavailable ? (e) => e.stopPropagation() : undefined
                }
              >
                <Box
                  sx={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 1,
                    width: '100%',
                    flexWrap: 'wrap',
                  }}
                >
                  {/* Avatar */}
                  {avatarEl}

                  {/* Lock icon for unavailable */}
                  {!isCurrentAssignee && isUnavailable && (
                    <LockRoundedIcon
                      fontSize="small"
                      sx={{ color: 'text.disabled', flexShrink: 0 }}
                    />
                  )}

                  <Typography
                    variant="body2"
                    sx={{
                      flexGrow: 1,
                      minWidth: 100,
                      color: isCurrentAssignee || isUnavailable ? 'text.disabled' : 'inherit',
                    }}
                  >
                    {empName}
                    {emp.employeeCode && (
                      <Typography
                        component="span"
                        variant="caption"
                        color="text.secondary"
                        sx={{ ml: 0.5 }}
                      >
                        ({emp.employeeCode})
                      </Typography>
                    )}
                  </Typography>

                  {/* Status chip */}
                  {statusChip}

                  {/* Only show workload info for available employees */}
                  {!isCurrentAssignee && !isUnavailable && (
                    <>
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
                        <Tooltip
                          title={`${overdueCount} overdue task${overdueCount !== 1 ? 's' : ''}`}
                        >
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
                    </>
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
