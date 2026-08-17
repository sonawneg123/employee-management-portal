/**
 * @fileoverview AttendancePage — role-aware attendance management page.
 *
 * Behaviour differs by role:
 * - ADMIN / HR: full list with employee + date + status filters,
 *   via GET /attendance (paginated). Mark Attendance button to create records.
 *   Edit icon on each row to update existing records.
 * - MANAGER: same read-only list view, no create/edit.
 * - EMPLOYEE: own records only, via GET /attendance/my, with date and
 *   status filters and full pagination support.
 */

import React, { useState, useCallback } from 'react';
import { Helmet } from 'react-helmet-async';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Skeleton,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Tooltip,
  Typography,
  alpha,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import EventNoteRoundedIcon from '@mui/icons-material/EventNoteRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import LoginRoundedIcon from '@mui/icons-material/LoginRounded';
import LogoutRoundedIcon from '@mui/icons-material/LogoutRounded';

import {
  getAttendance,
  getMyAttendance,
  createAttendance,
  updateAttendance,
  checkIn,
  checkOut,
} from '@/services/attendanceApi';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';

// ── Status display helpers ───────────────────────────────────────────────────

/** Soft badge palette per status */
const STATUS_PALETTE = {
  PRESENT: { bg: '#D1FAE5', color: '#065F46', dot: '#10B981' },
  ABSENT: { bg: '#FEE2E2', color: '#991B1B', dot: '#EF4444' },
  HALF_DAY: { bg: '#FEF3C7', color: '#92400E', dot: '#F59E0B' },
  WORK_FROM_HOME: { bg: '#DBEAFE', color: '#1E40AF', dot: '#3B82F6' },
  ON_LEAVE: { bg: '#EDE9FE', color: '#5B21B6', dot: '#7C3AED' },
};

/** Human-readable labels for the filter drop-down. */
const STATUS_LABELS = {
  PRESENT: 'Present',
  ABSENT: 'Absent',
  HALF_DAY: 'Half Day',
  WORK_FROM_HOME: 'Work From Home',
  ON_LEAVE: 'On Leave',
};

/**
 * Soft badge for an attendance status value.
 *
 * @param {{ status: string }} props
 * @returns {JSX.Element}
 */
function StatusChip({ status }) {
  const palette = STATUS_PALETTE[status] ?? { bg: '#F1F5F9', color: '#475569', dot: '#94A3B8' };
  const label = STATUS_LABELS[status] ?? status ?? 'Unknown';

  return (
    <Box
      component="span"
      aria-label={`Status: ${label}`}
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '5px',
        px: '10px',
        py: '4px',
        borderRadius: '20px',
        bgcolor: palette.bg,
        color: palette.color,
        fontSize: '0.75rem',
        fontWeight: 600,
        letterSpacing: '0.02em',
        lineHeight: 1.4,
        whiteSpace: 'nowrap',
      }}
    >
      <Box
        component="span"
        sx={{ width: 6, height: 6, borderRadius: '50%', bgcolor: palette.dot, flexShrink: 0 }}
      />
      {label}
    </Box>
  );
}

// ── Table skeleton ────────────────────────────────────────────────────────────

/**
 * @param {{ rows?: number, cols: number }} props
 */
function TableSkeleton({ rows = 8, cols }) {
  return Array.from({ length: rows }, (_, r) => (
    <TableRow key={r}>
      {Array.from({ length: cols }, (__, c) => (
        <TableCell key={c}>
          <Skeleton variant="text" sx={{ borderRadius: '6px' }} />
        </TableCell>
      ))}
    </TableRow>
  ));
}

// ── Attendance form dialog ────────────────────────────────────────────────────

const EMPTY_FORM = {
  employeeId: '',
  attendanceDate: '',
  checkInTime: '',
  checkOutTime: '',
  status: '',
  notes: '',
};

/**
 * Create / Edit attendance dialog (ADMIN / HR only).
 */
function AttendanceDialog({ open, mode, defaultValues, isSubmitting, onSubmit, onClose }) {
  const [form, setForm] = useState(defaultValues ?? EMPTY_FORM);
  const [errors, setErrors] = useState({});

  React.useEffect(() => {
    if (open) {
      setForm(defaultValues ?? EMPTY_FORM);
      setErrors({});
    }
  }, [open, defaultValues]);

  const change = useCallback((e) => {
    const { name, value } = e.target;
    setForm((f) => ({ ...f, [name]: value }));
    setErrors((e) => ({ ...e, [name]: '' }));
  }, []);

  const validate = () => {
    const errs = {};
    if (mode === 'create' && !form.employeeId.trim()) errs.employeeId = 'Employee ID is required';
    if (!form.attendanceDate) errs.attendanceDate = 'Date is required';
    if (!form.status) errs.status = 'Status is required';
    setErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleSubmit = () => {
    if (!validate()) return;
    const payload = {
      ...(mode === 'create' && { employeeId: form.employeeId }),
      attendanceDate: form.attendanceDate,
      checkInTime: form.checkInTime || null,
      checkOutTime: form.checkOutTime || null,
      status: form.status,
      notes: form.notes || null,
    };
    onSubmit(payload);
  };

  return (
    <Dialog
      open={open}
      onClose={isSubmitting ? undefined : onClose}
      maxWidth="sm"
      fullWidth
      PaperProps={{ sx: { borderRadius: '16px' } }}
    >
      <DialogTitle sx={{ fontWeight: 700, pb: 1 }}>
        {mode === 'create' ? '📋 Mark Attendance' : '✏️ Edit Attendance'}
      </DialogTitle>
      <DialogContent
        sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: '16px !important' }}
      >
        {mode === 'create' && (
          <TextField
            label="Employee ID (UUID)"
            name="employeeId"
            value={form.employeeId}
            onChange={change}
            error={!!errors.employeeId}
            helperText={errors.employeeId}
            fullWidth
            required
            disabled={isSubmitting}
            placeholder="e.g. 3fa85f64-5717-4562-b3fc-2c963f66afa6"
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
          />
        )}
        <TextField
          label="Attendance Date"
          name="attendanceDate"
          type="date"
          value={form.attendanceDate}
          onChange={change}
          error={!!errors.attendanceDate}
          helperText={errors.attendanceDate}
          fullWidth
          required
          disabled={isSubmitting || mode === 'edit'}
          InputLabelProps={{ shrink: true }}
          sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
        />
        <FormControl fullWidth required error={!!errors.status} disabled={isSubmitting}>
          <InputLabel>Status</InputLabel>
          <Select
            name="status"
            value={form.status}
            label="Status"
            onChange={change}
            sx={{ borderRadius: '10px' }}
          >
            {Object.entries(STATUS_LABELS).map(([v, l]) => (
              <MenuItem key={v} value={v}>
                {l}
              </MenuItem>
            ))}
          </Select>
          {errors.status && (
            <Typography variant="caption" color="error" sx={{ mt: 0.5, ml: 1.5 }}>
              {errors.status}
            </Typography>
          )}
        </FormControl>
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Check-in Time"
              name="checkInTime"
              type="time"
              value={form.checkInTime}
              onChange={change}
              fullWidth
              disabled={isSubmitting}
              InputLabelProps={{ shrink: true }}
              inputProps={{ step: 60 }}
              sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <TextField
              label="Check-out Time"
              name="checkOutTime"
              type="time"
              value={form.checkOutTime}
              onChange={change}
              fullWidth
              disabled={isSubmitting}
              InputLabelProps={{ shrink: true }}
              inputProps={{ step: 60 }}
              sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
            />
          </Grid>
        </Grid>
        <TextField
          label="Notes"
          name="notes"
          value={form.notes}
          onChange={change}
          fullWidth
          multiline
          rows={2}
          disabled={isSubmitting}
          placeholder="Optional notes…"
          sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2.5, gap: 1 }}>
        <Button
          onClick={onClose}
          disabled={isSubmitting}
          variant="outlined"
          sx={{ borderRadius: '8px', fontWeight: 600 }}
        >
          Cancel
        </Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isSubmitting}
          startIcon={isSubmitting ? <CircularProgress size={16} color="inherit" /> : null}
          sx={{ borderRadius: '8px', fontWeight: 600 }}
        >
          {isSubmitting ? 'Saving…' : mode === 'create' ? 'Mark Attendance' : 'Save Changes'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

const ROWS_PER_PAGE_OPTIONS = [10, 25, 50];

/**
 * Attendance list page (role-aware).
 *
 * @returns {JSX.Element}
 */
export default function AttendancePage() {
  const queryClient = useQueryClient();
  const { hasAnyRole } = useAuth();
  const isAdminOrHr = hasAnyRole([ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]);
  const canWrite = hasAnyRole([ROLES.ADMIN, ROLES.HR]);

  // ── Shared pagination state ───────────────────────────────────────────────
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);

  // ── Admin/HR filters ──────────────────────────────────────────────────────
  const [adminDate, setAdminDate] = useState('');
  const [adminStatus, setAdminStatus] = useState('');
  const [adminEmployeeId, setAdminEmployeeId] = useState('');

  // ── Employee filters ──────────────────────────────────────────────────────
  const [myDate, setMyDate] = useState('');
  const [myStatus, setMyStatus] = useState('');

  // ── Dialog state ─────────────────────────────────────────────────────────
  const [dialogMode, setDialogMode] = useState(null);
  const [editTarget, setEditTarget] = useState(null);
  const [snack, setSnack] = useState({ open: false, severity: 'success', message: '' });

  const showSnack = useCallback(
    (severity, message) => setSnack({ open: true, severity, message }),
    [],
  );
  const closeSnack = useCallback(() => setSnack((s) => ({ ...s, open: false })), []);

  const handleAdminDateChange = useCallback((v) => {
    setAdminDate(v);
    setPage(0);
  }, []);
  const handleAdminStatusChange = useCallback((v) => {
    setAdminStatus(v);
    setPage(0);
  }, []);
  const handleAdminEmployeeIdChange = useCallback((v) => {
    setAdminEmployeeId(v);
    setPage(0);
  }, []);
  const handleMyDateChange = useCallback((v) => {
    setMyDate(v);
    setPage(0);
  }, []);
  const handleMyStatusChange = useCallback((v) => {
    setMyStatus(v);
    setPage(0);
  }, []);

  const adminParams = {
    page,
    size,
    ...(adminDate && { date: adminDate }),
    ...(adminStatus && { status: adminStatus }),
    ...(adminEmployeeId && { employeeId: adminEmployeeId }),
  };

  const myParams = {
    page,
    size,
    ...(myDate && { date: myDate }),
    ...(myStatus && { status: myStatus }),
  };

  const { data, isLoading, isError, error } = useQuery({
    queryKey: isAdminOrHr ? ['attendance', 'all', adminParams] : ['attendance', 'my', myParams],
    queryFn: isAdminOrHr ? () => getAttendance(adminParams) : () => getMyAttendance(myParams),
    staleTime: 60_000,
    placeholderData: (prev) => prev,
  });

  const createMutation = useMutation({
    mutationFn: (payload) => createAttendance(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendance', 'all'] });
      setDialogMode(null);
      showSnack('success', 'Attendance record created.');
    },
    onError: (err) => showSnack('error', err?.message ?? 'Failed to create record.'),
  });

  const updateMutation = useMutation({
    mutationFn: ({ id, payload }) => updateAttendance(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['attendance', 'all'] });
      setDialogMode(null);
      setEditTarget(null);
      showSnack('success', 'Attendance record updated.');
    },
    onError: (err) => showSnack('error', err?.message ?? 'Failed to update record.'),
  });

  // ── Employee self-service check-in / check-out mutations ─────────────────
  const checkInMutation = useMutation({
    mutationFn: checkIn,
    onSuccess: () => {
      // Invalidate attendance queries so useTodayAttendance picks up the new record
      queryClient.invalidateQueries({ queryKey: ['attendance', 'my'] });
      // Also invalidate task availability so the manager selector refreshes
      queryClient.invalidateQueries({ queryKey: ['tasks', 'availability'] });
      // Invalidate employee's own task list so actions are immediately unblocked
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      showSnack('success', 'Checked in successfully.');
    },
    onError: (err) => showSnack('error', err?.response?.data?.detail ?? err?.message ?? 'Check-in failed.'),
  });

  const checkOutMutation = useMutation({
    mutationFn: checkOut,
    onSuccess: () => {
      // Invalidate attendance queries so useTodayAttendance picks up the checkout
      queryClient.invalidateQueries({ queryKey: ['attendance', 'my'] });
      // Also invalidate task availability so the manager selector refreshes
      queryClient.invalidateQueries({ queryKey: ['tasks', 'availability'] });
      // Invalidate employee's own task list so actions are immediately blocked
      queryClient.invalidateQueries({ queryKey: ['tasks'] });
      showSnack('success', 'Checked out successfully.');
    },
    onError: (err) => showSnack('error', err?.response?.data?.detail ?? err?.message ?? 'Check-out failed.'),
  });

  const isMutating = createMutation.isPending || updateMutation.isPending;

  const openCreate = useCallback(() => {
    setEditTarget(null);
    setDialogMode('create');
  }, []);
  const openEdit = useCallback((rec) => {
    setEditTarget(rec);
    setDialogMode('edit');
  }, []);

  const handleDialogSubmit = useCallback(
    (payload) => {
      if (dialogMode === 'create') {
        createMutation.mutate(payload);
      } else if (editTarget) {
        updateMutation.mutate({ id: editTarget.id, payload });
      }
    },
    [dialogMode, editTarget, createMutation, updateMutation],
  );

  const handleDialogClose = useCallback(() => {
    if (!isMutating) {
      setDialogMode(null);
      setEditTarget(null);
    }
  }, [isMutating]);

  const records = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;
  const title = isAdminOrHr ? 'Attendance' : 'My Attendance';
  const colCount = isAdminOrHr ? (canWrite ? 7 : 6) : 5;

  return (
    <>
      <Helmet>
        <title>{title} — PeopleCore HR</title>
      </Helmet>

      {/* ── Page header ── */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          mb: 4,
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: '12px',
              bgcolor: (t) => alpha(t.palette.primary.main, 0.1),
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'primary.main',
            }}
          >
            {isAdminOrHr ? <EventNoteRoundedIcon /> : <AccessTimeRoundedIcon />}
          </Box>
          <Box>
            <Typography variant="h2" fontWeight={800} sx={{ letterSpacing: '-0.02em' }}>
              {title}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {isAdminOrHr
                ? 'View and manage employee attendance records'
                : 'Your personal attendance history'}
            </Typography>
          </Box>
        </Box>
        {canWrite && (
          <Button
            variant="contained"
            startIcon={<AddRoundedIcon />}
            onClick={openCreate}
            aria-label="Mark attendance"
            sx={{ height: 44, borderRadius: '10px', px: 3, fontWeight: 600 }}
          >
            Mark Attendance
          </Button>
        )}
        {/* Employee self-service check-in / check-out */}
        {!isAdminOrHr && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            <Button
              variant="contained"
              color="success"
              startIcon={
                checkInMutation.isPending ? (
                  <CircularProgress size={16} color="inherit" />
                ) : (
                  <LoginRoundedIcon />
                )
              }
              onClick={() => checkInMutation.mutate()}
              disabled={checkInMutation.isPending || checkOutMutation.isPending}
              aria-label="Check in for today"
              sx={{ height: 44, borderRadius: '10px', px: 3, fontWeight: 600 }}
            >
              Check In
            </Button>
            <Button
              variant="outlined"
              color="warning"
              startIcon={
                checkOutMutation.isPending ? (
                  <CircularProgress size={16} color="inherit" />
                ) : (
                  <LogoutRoundedIcon />
                )
              }
              onClick={() => checkOutMutation.mutate()}
              disabled={checkInMutation.isPending || checkOutMutation.isPending}
              aria-label="Check out for today"
              sx={{ height: 44, borderRadius: '10px', px: 3, fontWeight: 600 }}
            >
              Check Out
            </Button>
          </Box>
        )}
      </Box>

      {/* ── Admin / HR / Manager filters ── */}
      {isAdminOrHr && (
        <Paper
          variant="outlined"
          sx={{
            p: 2,
            mb: 3,
            borderRadius: '12px',
            borderColor: 'divider',
          }}
        >
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                label="Filter by date"
                type="date"
                size="small"
                fullWidth
                value={adminDate}
                onChange={(e) => handleAdminDateChange(e.target.value)}
                InputLabelProps={{ shrink: true }}
                sx={{ '& .MuiOutlinedInput-root': { borderRadius: '8px' } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <FormControl size="small" fullWidth>
                <InputLabel>Status</InputLabel>
                <Select
                  label="Status"
                  value={adminStatus}
                  onChange={(e) => handleAdminStatusChange(e.target.value)}
                  sx={{ borderRadius: '8px' }}
                >
                  <MenuItem value="">All statuses</MenuItem>
                  {Object.entries(STATUS_LABELS).map(([value, label]) => (
                    <MenuItem key={value} value={value}>
                      {label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 4 }}>
              <TextField
                label="Employee ID (UUID)"
                size="small"
                fullWidth
                value={adminEmployeeId}
                onChange={(e) => handleAdminEmployeeIdChange(e.target.value)}
                placeholder="Filter by employee UUID…"
                inputProps={{ 'aria-label': 'Filter by employee ID' }}
                sx={{ '& .MuiOutlinedInput-root': { borderRadius: '8px' } }}
              />
            </Grid>
          </Grid>
        </Paper>
      )}

      {/* ── Employee self-service filters ── */}
      {!isAdminOrHr && (
        <Paper
          variant="outlined"
          sx={{ p: 2, mb: 3, borderRadius: '12px', borderColor: 'divider' }}
        >
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <TextField
                label="Filter by date"
                type="date"
                size="small"
                fullWidth
                value={myDate}
                onChange={(e) => handleMyDateChange(e.target.value)}
                InputLabelProps={{ shrink: true }}
                inputProps={{ 'aria-label': 'Filter attendance by date' }}
                sx={{ '& .MuiOutlinedInput-root': { borderRadius: '8px' } }}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6, md: 3 }}>
              <FormControl size="small" fullWidth>
                <InputLabel id="my-status-label">Status</InputLabel>
                <Select
                  labelId="my-status-label"
                  label="Status"
                  value={myStatus}
                  onChange={(e) => handleMyStatusChange(e.target.value)}
                  inputProps={{ 'aria-label': 'Filter attendance by status' }}
                  sx={{ borderRadius: '8px' }}
                >
                  <MenuItem value="">All statuses</MenuItem>
                  {Object.entries(STATUS_LABELS).map(([value, label]) => (
                    <MenuItem key={value} value={value}>
                      {label}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </Paper>
      )}

      {/* ── Error banner ── */}
      {isError && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: '10px' }}>
          {error?.message ?? 'Failed to load attendance records. Please try again.'}
        </Alert>
      )}

      {/* ── Records table ── */}
      <Paper
        variant="outlined"
        sx={{ borderRadius: '12px', borderColor: 'divider', overflow: 'hidden' }}
      >
        <TableContainer>
          <Table size="small" aria-label={title}>
            <TableHead>
              <TableRow sx={{ bgcolor: 'background.default' }}>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.75rem',
                    letterSpacing: '0.05em',
                    color: 'text.secondary',
                    textTransform: 'uppercase',
                  }}
                >
                  Date
                </TableCell>
                {isAdminOrHr && (
                  <TableCell
                    sx={{
                      fontWeight: 700,
                      fontSize: '0.75rem',
                      letterSpacing: '0.05em',
                      color: 'text.secondary',
                      textTransform: 'uppercase',
                    }}
                  >
                    Employee
                  </TableCell>
                )}
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.75rem',
                    letterSpacing: '0.05em',
                    color: 'text.secondary',
                    textTransform: 'uppercase',
                  }}
                >
                  Check In
                </TableCell>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.75rem',
                    letterSpacing: '0.05em',
                    color: 'text.secondary',
                    textTransform: 'uppercase',
                  }}
                >
                  Check Out
                </TableCell>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.75rem',
                    letterSpacing: '0.05em',
                    color: 'text.secondary',
                    textTransform: 'uppercase',
                  }}
                >
                  Status
                </TableCell>
                <TableCell
                  sx={{
                    fontWeight: 700,
                    fontSize: '0.75rem',
                    letterSpacing: '0.05em',
                    color: 'text.secondary',
                    textTransform: 'uppercase',
                  }}
                >
                  Notes
                </TableCell>
                {canWrite && (
                  <TableCell
                    align="right"
                    sx={{
                      fontWeight: 700,
                      fontSize: '0.75rem',
                      letterSpacing: '0.05em',
                      color: 'text.secondary',
                      textTransform: 'uppercase',
                    }}
                  >
                    Actions
                  </TableCell>
                )}
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                <TableSkeleton rows={size} cols={colCount} />
              ) : records.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={colCount} align="center" sx={{ py: 8 }}>
                    <Box
                      sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: 1.5,
                      }}
                    >
                      <Typography fontSize="2.5rem" role="img" aria-label="clock">
                        ⏰
                      </Typography>
                      <Typography variant="h6" fontWeight={700} color="text.secondary">
                        No attendance records
                      </Typography>
                      <Typography variant="body2" color="text.disabled">
                        {isAdminOrHr
                          ? 'No attendance records match the selected filters.'
                          : 'No attendance records found for your account.'}
                      </Typography>
                    </Box>
                  </TableCell>
                </TableRow>
              ) : (
                records.map((rec) => (
                  <TableRow
                    key={rec.id}
                    hover
                    sx={{
                      '&:hover': { bgcolor: (t) => alpha(t.palette.primary.main, 0.03) },
                      transition: 'background-color 150ms ease',
                    }}
                  >
                    <TableCell sx={{ fontWeight: 500 }}>{rec.attendanceDate}</TableCell>
                    {isAdminOrHr && (
                      <TableCell sx={{ fontWeight: 500 }}>
                        {rec.employeeName ?? rec.employeeCode ?? rec.employeeId}
                      </TableCell>
                    )}
                    <TableCell sx={{ color: 'text.secondary', fontFamily: 'monospace' }}>
                      {rec.checkInTime ?? '—'}
                    </TableCell>
                    <TableCell sx={{ color: 'text.secondary', fontFamily: 'monospace' }}>
                      {rec.checkOutTime ?? '—'}
                    </TableCell>
                    <TableCell>
                      <StatusChip status={rec.status} />
                    </TableCell>
                    <TableCell sx={{ color: 'text.secondary', fontSize: '0.8rem', maxWidth: 200 }}>
                      {rec.notes ?? '—'}
                    </TableCell>
                    {canWrite && (
                      <TableCell align="right">
                        <Tooltip title="Edit record">
                          <IconButton
                            size="small"
                            onClick={() => openEdit(rec)}
                            aria-label="Edit attendance record"
                            sx={{ color: 'primary.main' }}
                          >
                            <EditRoundedIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      </TableCell>
                    )}
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        {/* ── Pagination ── */}
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_e, newPage) => setPage(newPage)}
          rowsPerPage={size}
          onRowsPerPageChange={(e) => {
            setSize(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
          labelRowsPerPage="Rows:"
          aria-label="Attendance table pagination"
          sx={{ borderTop: '1px solid', borderColor: 'divider' }}
        />
      </Paper>

      {/* ── Create / Edit Dialog (ADMIN / HR only) ── */}
      {canWrite && (
        <AttendanceDialog
          open={dialogMode !== null}
          mode={dialogMode ?? 'create'}
          defaultValues={
            dialogMode === 'edit' && editTarget
              ? {
                  employeeId: editTarget.employeeId ?? '',
                  attendanceDate: editTarget.attendanceDate ?? '',
                  checkInTime: editTarget.checkInTime ?? '',
                  checkOutTime: editTarget.checkOutTime ?? '',
                  status: editTarget.status ?? '',
                  notes: editTarget.notes ?? '',
                }
              : EMPTY_FORM
          }
          isSubmitting={isMutating}
          onSubmit={handleDialogSubmit}
          onClose={handleDialogClose}
        />
      )}

      {/* ── Snackbar ── */}
      <Snackbar
        open={snack.open}
        autoHideDuration={4000}
        onClose={closeSnack}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={closeSnack}
          severity={snack.severity}
          variant="filled"
          sx={{ width: '100%', borderRadius: '10px' }}
        >
          {snack.message}
        </Alert>
      </Snackbar>
    </>
  );
}
