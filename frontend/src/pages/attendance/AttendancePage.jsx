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
 *
 * Security model:
 *   The EMPLOYEE path exclusively calls GET /attendance/my — the backend
 *   resolves the caller's identity from the JWT and scopes the query to their
 *   own employee record. No employee ID is ever sent in the request, so it
 *   is impossible for an employee to retrieve another person's records by
 *   manipulating the URL or request parameters.
 *
 * Uses react-query for data fetching and MUI for the table, filters, and
 * pagination controls.
 */

import React, { useState, useCallback } from 'react';
import { Helmet } from 'react-helmet-async';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Button,
  Chip,
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
} from '@mui/material';
import AddIcon  from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';

import {
  getAttendance,
  getMyAttendance,
  createAttendance,
  updateAttendance,
} from '@/services/attendanceApi';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';

// ── Status display helpers ───────────────────────────────────────────────────

/** Maps every AttendanceStatus enum value to a MUI Chip color. */
const STATUS_COLOR_MAP = {
  PRESENT:        'success',
  ABSENT:         'error',
  HALF_DAY:       'warning',
  WORK_FROM_HOME: 'info',
  ON_LEAVE:       'default',
};

/** Human-readable labels for the filter drop-down. */
const STATUS_LABELS = {
  PRESENT:        'Present',
  ABSENT:         'Absent',
  HALF_DAY:       'Half Day',
  WORK_FROM_HOME: 'Work From Home',
  ON_LEAVE:       'On Leave',
};

/**
 * Coloured chip for an attendance status value.
 *
 * @param {{ status: string }} props
 * @returns {JSX.Element}
 */
function StatusChip({ status }) {
  return (
    <Chip
      label={STATUS_LABELS[status] ?? status ?? 'Unknown'}
      color={STATUS_COLOR_MAP[status] ?? 'default'}
      size="small"
      sx={{ fontWeight: 600 }}
    />
  );
}

// ── Table skeleton ────────────────────────────────────────────────────────────

/**
 * Renders skeleton rows while data is loading.
 *
 * @param {{ rows?: number, cols: number }} props
 */
function TableSkeleton({ rows = 8, cols }) {
  return Array.from({ length: rows }, (_, r) => (
    <TableRow key={r}>
      {Array.from({ length: cols }, (__, c) => (
        <TableCell key={c}><Skeleton variant="text" /></TableCell>
      ))}
    </TableRow>
  ));
}

// ── Attendance form dialog ────────────────────────────────────────────────────

const EMPTY_FORM = {
  employeeId:     '',
  attendanceDate: '',
  checkInTime:    '',
  checkOutTime:   '',
  status:         '',
  notes:          '',
};

/**
 * Create / Edit attendance dialog (ADMIN / HR only).
 *
 * @param {{
 *   open: boolean,
 *   mode: 'create'|'edit',
 *   defaultValues?: object,
 *   isSubmitting: boolean,
 *   onSubmit: (data: object) => void,
 *   onClose: () => void,
 * }} props
 */
function AttendanceDialog({ open, mode, defaultValues, isSubmitting, onSubmit, onClose }) {
  const [form, setForm] = useState(defaultValues ?? EMPTY_FORM);
  const [errors, setErrors] = useState({});

  // Reset form whenever the dialog opens with new defaults
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
      checkInTime:    form.checkInTime  || null,
      checkOutTime:   form.checkOutTime || null,
      status:         form.status,
      notes:          form.notes || null,
    };
    onSubmit(payload);
  };

  return (
    <Dialog open={open} onClose={isSubmitting ? undefined : onClose} maxWidth="sm" fullWidth>
      <DialogTitle>{mode === 'create' ? 'Mark Attendance' : 'Edit Attendance'}</DialogTitle>
      <DialogContent sx={{ display: 'flex', flexDirection: 'column', gap: 2, pt: 2 }}>
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
        />
        <FormControl fullWidth required error={!!errors.status} disabled={isSubmitting}>
          <InputLabel>Status</InputLabel>
          <Select name="status" value={form.status} label="Status" onChange={change}>
            {Object.entries(STATUS_LABELS).map(([v, l]) => (
              <MenuItem key={v} value={v}>{l}</MenuItem>
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
        />
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose} disabled={isSubmitting}>Cancel</Button>
        <Button
          variant="contained"
          onClick={handleSubmit}
          disabled={isSubmitting}
          startIcon={isSubmitting ? <CircularProgress size={16} color="inherit" /> : null}
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
  const isAdminOrHr      = hasAnyRole([ROLES.ADMIN, ROLES.HR, ROLES.MANAGER]);
  const canWrite         = hasAnyRole([ROLES.ADMIN, ROLES.HR]); // POST/PUT /attendance

  // ── Shared pagination state ───────────────────────────────────────────────
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(25);

  // ── Admin/HR filters ──────────────────────────────────────────────────────
  const [adminDate,       setAdminDate]       = useState('');
  const [adminStatus,     setAdminStatus]     = useState('');
  const [adminEmployeeId, setAdminEmployeeId] = useState('');

  // ── Employee filters ──────────────────────────────────────────────────────
  const [myDate,   setMyDate]   = useState('');
  const [myStatus, setMyStatus] = useState('');

  // ── Dialog state ─────────────────────────────────────────────────────────
  const [dialogMode,    setDialogMode]    = useState(null); // 'create' | 'edit' | null
  const [editTarget,    setEditTarget]    = useState(null); // AttendanceResponse | null
  const [snack, setSnack] = useState({ open: false, severity: 'success', message: '' });

  const showSnack  = useCallback((severity, message) => setSnack({ open: true, severity, message }), []);
  const closeSnack = useCallback(() => setSnack((s) => ({ ...s, open: false })), []);

  // Reset to page 0 whenever filters change
  const handleAdminDateChange       = useCallback((v) => { setAdminDate(v);       setPage(0); }, []);
  const handleAdminStatusChange     = useCallback((v) => { setAdminStatus(v);     setPage(0); }, []);
  const handleAdminEmployeeIdChange = useCallback((v) => { setAdminEmployeeId(v); setPage(0); }, []);
  const handleMyDateChange          = useCallback((v) => { setMyDate(v);          setPage(0); }, []);
  const handleMyStatusChange        = useCallback((v) => { setMyStatus(v);        setPage(0); }, []);

  // ── Query: Admin / HR path ────────────────────────────────────────────────
  const adminParams = {
    page,
    size,
    ...(adminDate       && { date:       adminDate }),
    ...(adminStatus     && { status:     adminStatus }),
    ...(adminEmployeeId && { employeeId: adminEmployeeId }),
  };

  // ── Query: Employee self-service path ─────────────────────────────────────
  // IMPORTANT: no employeeId field is ever included in this request.
  // The backend GET /attendance/my resolves the caller from the JWT.
  const myParams = {
    page,
    size,
    ...(myDate   && { date:   myDate }),
    ...(myStatus && { status: myStatus }),
  };

  const { data, isLoading, isError, error } = useQuery({
    queryKey: isAdminOrHr
      ? ['attendance', 'all', adminParams]
      : ['attendance', 'my', myParams],
    queryFn: isAdminOrHr
      ? () => getAttendance(adminParams)
      : () => getMyAttendance(myParams),
    staleTime: 60_000,
    placeholderData: (prev) => prev,
  });

  // ── Mutations ─────────────────────────────────────────────────────────────
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

  const isMutating = createMutation.isPending || updateMutation.isPending;

  // ── Dialog handlers ───────────────────────────────────────────────────────
  const openCreate = useCallback(() => {
    setEditTarget(null);
    setDialogMode('create');
  }, []);

  const openEdit = useCallback((rec) => {
    setEditTarget(rec);
    setDialogMode('edit');
  }, []);

  const handleDialogSubmit = useCallback((payload) => {
    if (dialogMode === 'create') {
      createMutation.mutate(payload);
    } else if (editTarget) {
      updateMutation.mutate({ id: editTarget.id, payload });
    }
  }, [dialogMode, editTarget, createMutation, updateMutation]);

  const handleDialogClose = useCallback(() => {
    if (!isMutating) {
      setDialogMode(null);
      setEditTarget(null);
    }
  }, [isMutating]);

  // ── Derived ───────────────────────────────────────────────────────────────
  const records       = data?.content        ?? [];
  const totalElements = data?.totalElements  ?? 0;
  const title         = isAdminOrHr ? 'Attendance' : 'My Attendance';
  const colCount      = isAdminOrHr ? (canWrite ? 7 : 6) : 5;

  return (
    <>
      <Helmet><title>{title} — Employee Portal</title></Helmet>

      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 3, flexWrap: 'wrap', gap: 1 }}>
        <Box>
          <Typography variant="h4" fontWeight={700}>{title}</Typography>
          <Typography variant="body2" color="text.secondary">
            {isAdminOrHr
              ? 'View and manage employee attendance records'
              : 'Your personal attendance history'}
          </Typography>
        </Box>
        {canWrite && (
          <Button variant="contained" startIcon={<AddIcon />} onClick={openCreate} aria-label="Mark attendance">
            Mark Attendance
          </Button>
        )}
      </Box>

      {/* ── Admin / HR / Manager filters ── */}
      {isAdminOrHr && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <TextField
              label="Filter by date"
              type="date"
              size="small"
              fullWidth
              value={adminDate}
              onChange={(e) => handleAdminDateChange(e.target.value)}
              InputLabelProps={{ shrink: true }}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6, md: 3 }}>
            <FormControl size="small" fullWidth>
              <InputLabel>Status</InputLabel>
              <Select
                label="Status"
                value={adminStatus}
                onChange={(e) => handleAdminStatusChange(e.target.value)}
              >
                <MenuItem value="">All statuses</MenuItem>
                {Object.entries(STATUS_LABELS).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
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
            />
          </Grid>
        </Grid>
      )}

      {/* ── Employee self-service filters ── */}
      {!isAdminOrHr && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
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
              >
                <MenuItem value="">All statuses</MenuItem>
                {Object.entries(STATUS_LABELS).map(([value, label]) => (
                  <MenuItem key={value} value={value}>{label}</MenuItem>
                ))}
              </Select>
            </FormControl>
          </Grid>
        </Grid>
      )}

      {/* ── Error banner ── */}
      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error?.message ?? 'Failed to load attendance records. Please try again.'}
        </Alert>
      )}

      {/* ── Records table ── */}
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
              {canWrite && <TableCell align="right"><strong>Actions</strong></TableCell>}
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableSkeleton rows={size} cols={colCount} />
            ) : records.length === 0 ? (
              <TableRow>
                <TableCell
                  colSpan={colCount}
                  align="center"
                  sx={{ py: 6, color: 'text.secondary' }}
                >
                  {isAdminOrHr
                    ? 'No attendance records match the selected filters.'
                    : 'No attendance records found for your account.'}
                </TableCell>
              </TableRow>
            ) : (
              records.map((rec) => (
                <TableRow key={rec.id} hover>
                  <TableCell>{rec.attendanceDate}</TableCell>
                  {isAdminOrHr && (
                    <TableCell>
                      {rec.employeeName ?? rec.employeeCode ?? rec.employeeId}
                    </TableCell>
                  )}
                  <TableCell>{rec.checkInTime ?? '—'}</TableCell>
                  <TableCell>{rec.checkOutTime ?? '—'}</TableCell>
                  <TableCell><StatusChip status={rec.status} /></TableCell>
                  <TableCell sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>
                    {rec.notes ?? '—'}
                  </TableCell>
                  {canWrite && (
                    <TableCell align="right">
                      <Tooltip title="Edit record">
                        <IconButton size="small" onClick={() => openEdit(rec)} aria-label="Edit attendance record">
                          <EditIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>

        {/* ── Pagination ── */}
        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_e, newPage) => setPage(newPage)}
          rowsPerPage={size}
          onRowsPerPageChange={(e) => { setSize(parseInt(e.target.value, 10)); setPage(0); }}
          rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
          labelRowsPerPage="Rows:"
          aria-label="Attendance table pagination"
        />
      </TableContainer>

      {/* ── Create / Edit Dialog (ADMIN / HR only) ── */}
      {canWrite && (
        <AttendanceDialog
          open={dialogMode !== null}
          mode={dialogMode ?? 'create'}
          defaultValues={dialogMode === 'edit' && editTarget ? {
            employeeId:     editTarget.employeeId ?? '',
            attendanceDate: editTarget.attendanceDate ?? '',
            checkInTime:    editTarget.checkInTime  ?? '',
            checkOutTime:   editTarget.checkOutTime ?? '',
            status:         editTarget.status ?? '',
            notes:          editTarget.notes  ?? '',
          } : EMPTY_FORM}
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
        <Alert onClose={closeSnack} severity={snack.severity} variant="filled" sx={{ width: '100%' }}>
          {snack.message}
        </Alert>
      </Snackbar>
    </>
  );
}
