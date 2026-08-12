/**
 * @fileoverview AdminUsersPage — user account and role management (ADMIN only).
 *
 * Displays a paginated list of all registered user accounts and allows the
 * admin to:
 *   - View email, name, current roles, and account status
 *   - Change the role of any user via a dropdown (replaces existing role)
 *   - Enable / disable an account
 *
 * Data is fetched via GET /admin/users (ROLE_ADMIN required).
 * Mutations call PUT /admin/users/:id/role and PUT /admin/users/:id/enabled.
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Alert,
  Box,
  Chip,
  CircularProgress,
  FormControl,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Skeleton,
  Snackbar,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import CheckIcon  from '@mui/icons-material/Check';
import CloseIcon  from '@mui/icons-material/Close';

import { getUsers, updateUserRole, setUserEnabled } from '@/services/adminApi';
import { ROLES } from '@/constants/roles';

// ── Constants ─────────────────────────────────────────────────────────────────

const ALL_ROLES = [
  { value: ROLES.ADMIN,    label: 'Admin' },
  { value: ROLES.HR,       label: 'HR' },
  { value: ROLES.MANAGER,  label: 'Manager' },
  { value: ROLES.EMPLOYEE, label: 'Employee' },
];

const ROLE_COLOR_MAP = {
  [ROLES.ADMIN]:    'error',
  [ROLES.HR]:       'warning',
  [ROLES.MANAGER]:  'info',
  [ROLES.EMPLOYEE]: 'default',
};

const ROWS_PER_PAGE_OPTIONS = [10, 20, 50];
const QUERY_KEY = ['admin', 'users'];

// ── Inline role-selector cell ─────────────────────────────────────────────────

/**
 * Renders the current role(s) of a user plus an in-row dropdown to change it.
 *
 * @param {{ userId: string, currentRoles: string[], onSave: (role: string) => void, isSaving: boolean }} props
 */
function RoleCell({ userId, currentRoles, onSave, isSaving }) {
  const [selected, setSelected] = useState(currentRoles[0] ?? ROLES.EMPLOYEE);
  const changed = selected !== currentRoles[0];

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <FormControl size="small" sx={{ minWidth: 130 }} disabled={isSaving}>
        <Select
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
          inputProps={{ 'aria-label': 'Select role' }}
        >
          {ALL_ROLES.map(({ value, label }) => (
            <MenuItem key={value} value={value}>{label}</MenuItem>
          ))}
        </Select>
      </FormControl>
      {changed && (
        <>
          <Tooltip title="Save role change">
            <span>
              <IconButton
                size="small"
                color="primary"
                onClick={() => onSave(selected)}
                disabled={isSaving}
                aria-label="Save role"
              >
                {isSaving ? <CircularProgress size={16} /> : <CheckIcon fontSize="small" />}
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Discard change">
            <IconButton
              size="small"
              onClick={() => setSelected(currentRoles[0] ?? ROLES.EMPLOYEE)}
              disabled={isSaving}
              aria-label="Discard role change"
            >
              <CloseIcon fontSize="small" />
            </IconButton>
          </Tooltip>
        </>
      )}
      {!changed && currentRoles.map((r) => (
        <Chip
          key={r}
          label={r.replace('ROLE_', '')}
          color={ROLE_COLOR_MAP[r] ?? 'default'}
          size="small"
          sx={{ fontWeight: 600 }}
        />
      ))}
    </Box>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

/**
 * Admin user-management page.
 *
 * @returns {JSX.Element}
 */
export default function AdminUsersPage() {
  const queryClient = useQueryClient();

  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [snack, setSnack] = useState({ open: false, severity: 'success', message: '' });

  const showSnack  = useCallback((severity, message) => setSnack({ open: true, severity, message }), []);
  const closeSnack = useCallback(() => setSnack((s) => ({ ...s, open: false })), []);

  // ── Query ─────────────────────────────────────────────────────────────────
  const { data, isLoading, isError, error } = useQuery({
    queryKey: [...QUERY_KEY, page, size],
    queryFn: () => getUsers({ page, size }),
    staleTime: 30_000,
    placeholderData: (prev) => prev,
  });

  // ── Role mutation ─────────────────────────────────────────────────────────
  const roleMutation = useMutation({
    mutationFn: ({ userId, roleName }) => updateUserRole(userId, roleName),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      showSnack('success', `Role updated to ${updated.roles[0]?.replace('ROLE_', '') ?? ''} for ${updated.email}.`);
    },
    onError: (err) => showSnack('error', err?.message ?? 'Failed to update role.'),
  });

  // ── Enable/disable mutation ────────────────────────────────────────────────
  const enabledMutation = useMutation({
    mutationFn: ({ userId, enabled }) => setUserEnabled(userId, enabled),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      showSnack('success', `${updated.email} has been ${updated.isEnabled ? 'enabled' : 'disabled'}.`);
    },
    onError: (err) => showSnack('error', err?.message ?? 'Failed to update account status.'),
  });

  const users        = data?.content       ?? [];
  const totalElements = data?.totalElements ?? 0;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <>
      <Helmet><title>User Management — Employee Portal</title></Helmet>

      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>User Management</Typography>
        <Typography variant="body2" color="text.secondary">
          View and manage all registered user accounts and their roles.
        </Typography>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error?.message ?? 'Failed to load users. Please try again.'}
        </Alert>
      )}

      <TableContainer component={Paper} variant="outlined">
        <Table size="small" aria-label="User accounts">
          <TableHead>
            <TableRow>
              <TableCell><strong>Email</strong></TableCell>
              <TableCell><strong>Name</strong></TableCell>
              <TableCell><strong>Role</strong></TableCell>
              <TableCell align="center"><strong>Enabled</strong></TableCell>
              <TableCell><strong>Joined</strong></TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading
              ? Array.from({ length: size }, (_, i) => (
                  <TableRow key={i}>
                    {Array.from({ length: 5 }, (__, j) => (
                      <TableCell key={j}><Skeleton variant="text" /></TableCell>
                    ))}
                  </TableRow>
                ))
              : users.length === 0
                ? (
                  <TableRow>
                    <TableCell colSpan={5} align="center" sx={{ py: 6, color: 'text.secondary' }}>
                      No user accounts found.
                    </TableCell>
                  </TableRow>
                )
                : users.map((user) => (
                  <TableRow key={user.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>{user.email}</Typography>
                    </TableCell>
                    <TableCell>
                      {user.firstName} {user.lastName}
                    </TableCell>
                    <TableCell>
                      <RoleCell
                        userId={user.id}
                        currentRoles={user.roles}
                        onSave={(roleName) => roleMutation.mutate({ userId: user.id, roleName })}
                        isSaving={roleMutation.isPending && roleMutation.variables?.userId === user.id}
                      />
                    </TableCell>
                    <TableCell align="center">
                      <Tooltip title={user.isEnabled ? 'Click to disable' : 'Click to enable'}>
                        <Switch
                          checked={user.isEnabled}
                          onChange={(e) =>
                            enabledMutation.mutate({ userId: user.id, enabled: e.target.checked })
                          }
                          disabled={
                            enabledMutation.isPending &&
                            enabledMutation.variables?.userId === user.id
                          }
                          size="small"
                          inputProps={{ 'aria-label': `Toggle account for ${user.email}` }}
                        />
                      </Tooltip>
                    </TableCell>
                    <TableCell sx={{ color: 'text.secondary', fontSize: '0.8rem' }}>
                      {user.createdAt
                        ? new Date(user.createdAt).toLocaleDateString()
                        : '—'}
                    </TableCell>
                  </TableRow>
                ))}
          </TableBody>
        </Table>

        <TablePagination
          component="div"
          count={totalElements}
          page={page}
          onPageChange={(_e, newPage) => setPage(newPage)}
          rowsPerPage={size}
          onRowsPerPageChange={(e) => { setSize(parseInt(e.target.value, 10)); setPage(0); }}
          rowsPerPageOptions={ROWS_PER_PAGE_OPTIONS}
          labelRowsPerPage="Rows:"
          aria-label="User table pagination"
        />
      </TableContainer>

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
