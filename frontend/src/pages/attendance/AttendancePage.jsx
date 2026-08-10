/**
 * @fileoverview AttendancePage — role-aware attendance management page.
 *
 * Behaviour differs by role:
 * - ADMIN / HR / MANAGER: full list with employee+date filters, via GET /attendance
 * - EMPLOYEE: own records only, via GET /attendance/my
 *
 * Uses react-query for data fetching and MUI for the table and filters.
 */

import React, { useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Chip,
  Grid,
  Paper,
  Skeleton,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';

import { getAttendance, getMyAttendance } from '@/services/attendanceApi';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';

// ── Status chip helper ────────────────────────────────────────────────────────

/**
 * @param {{ status: string }} props
 * @returns {JSX.Element}
 */
function StatusChip({ status }) {
  const map = {
    PRESENT: 'success',
    ABSENT:  'error',
    HALF_DAY: 'warning',
    ON_LEAVE: 'info',
    HOLIDAY:  'default',
  };
  return (
    <Chip
      label={status ?? 'UNKNOWN'}
      color={map[status] ?? 'default'}
      size="small"
      sx={{ fontWeight: 600 }}
    />
  );
}

// ── Table skeleton ────────────────────────────────────────────────────────────

function TableSkeleton({ rows = 6, cols = 5 }) {
  return Array.from({ length: rows }).map((_, r) => (
    <TableRow key={r}>
      {Array.from({ length: cols }).map((__, c) => (
        <TableCell key={c}><Skeleton variant="text" /></TableCell>
      ))}
    </TableRow>
  ));
}

// ── Page ──────────────────────────────────────────────────────────────────────

/**
 * Attendance list page (role-aware).
 *
 * @returns {JSX.Element}
 */
export default function AttendancePage() {
  const { hasAnyRole } = useAuth();
  const isAdminOrHr = hasAnyRole([ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]);

  // ── Filters (admin/hr only) ──────────────────────────────────────────────
  const [startDate, setStartDate] = useState('');
  const [endDate,   setEndDate]   = useState('');
  const [page] = useState(0);
  const [size] = useState(25);

  // ── Fetch ────────────────────────────────────────────────────────────────
  const params = {
    page,
    size,
    ...(startDate && { startDate }),
    ...(endDate   && { endDate }),
  };

  const {
    data,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: isAdminOrHr
      ? ['attendance', 'all', params]
      : ['attendance', 'my', { page, size }],
    queryFn: isAdminOrHr
      ? () => getAttendance(params)
      : () => getMyAttendance({ page, size }),
    staleTime: 60_000,
  });

  const records = data?.content ?? [];
  const title = isAdminOrHr ? 'Attendance' : 'My Attendance';

  return (
    <>
      <Helmet><title>{title} — Employee Portal</title></Helmet>

      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>{title}</Typography>
        <Typography variant="body2" color="text.secondary">
          {isAdminOrHr
            ? 'View and manage employee attendance records'
            : 'Your personal attendance history'}
        </Typography>
      </Box>

      {/* Filters — admin/HR only */}
      {isAdminOrHr && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <TextField
              label="From date"
              type="date"
              size="small"
              fullWidth
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <TextField
              label="To date"
              type="date"
              size="small"
              fullWidth
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
        </Grid>
      )}

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error?.message ?? 'Failed to load attendance records.'}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label={title}>
          <TableHead>
            <TableRow>
              <TableCell><strong>Date</strong></TableCell>
              {isAdminOrHr && <TableCell><strong>Employee</strong></TableCell>}
              <TableCell><strong>Check In</strong></TableCell>
              <TableCell><strong>Check Out</strong></TableCell>
              <TableCell><strong>Status</strong></TableCell>
              <TableCell><strong>Notes</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableSkeleton rows={8} cols={isAdminOrHr ? 6 : 5} />
            ) : records.length === 0 ? (
              <TableRow>
                <TableCell colSpan={isAdminOrHr ? 6 : 5} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                  No attendance records found.
                </TableCell>
              </TableRow>
            ) : (
              records.map((rec) => (
                <TableRow key={rec.id} hover>
                  <TableCell>{rec.attendanceDate}</TableCell>
                  {isAdminOrHr && (
                    <TableCell>{rec.employeeId}</TableCell>
                  )}
                  <TableCell>{rec.checkInTime ?? '—'}</TableCell>
                  <TableCell>{rec.checkOutTime ?? '—'}</TableCell>
                  <TableCell><StatusChip status={rec.status} /></TableCell>
                  <TableCell sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>
                    {rec.notes ?? '—'}
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {!isLoading && data?.totalElements != null && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          Showing {records.length} of {data.totalElements} records
        </Typography>
      )}
    </>
  );
}
