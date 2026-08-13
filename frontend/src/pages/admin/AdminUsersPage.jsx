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
  Avatar,
  Box,
  Card,
  Chip,
  CircularProgress,
  FormControl,
  IconButton,
  MenuItem,
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
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';
import CloseRoundedIcon from '@mui/icons-material/CloseRounded';
import PeopleRoundedIcon from '@mui/icons-material/PeopleRounded';

import { getUsers, updateUserRole, setUserEnabled } from '@/services/adminApi';
import { ROLES } from '@/constants/roles';

// ── Constants ─────────────────────────────────────────────────────────────────

const ALL_ROLES = [
  { value: ROLES.ADMIN, label: 'Admin' },
  { value: ROLES.HR, label: 'HR' },
  { value: ROLES.MANAGER, label: 'Manager' },
  { value: ROLES.EMPLOYEE, label: 'Employee' },
];

/**
 * Maps a role string to a soft badge colour/bg combo.
 */
const ROLE_BADGE = {
  [ROLES.ADMIN]: { color: '#EF4444', bg: 'rgba(239,68,68,0.1)', border: 'rgba(239,68,68,0.2)' },
  [ROLES.HR]: { color: '#4F46E5', bg: 'rgba(79,70,229,0.1)', border: 'rgba(79,70,229,0.2)' },
  [ROLES.MANAGER]: { color: '#7C3AED', bg: 'rgba(124,58,237,0.1)', border: 'rgba(124,58,237,0.2)' },
  [ROLES.EMPLOYEE]: {
    color: '#10B981',
    bg: 'rgba(16,185,129,0.1)',
    border: 'rgba(16,185,129,0.2)',
  },
};

const ROWS_PER_PAGE_OPTIONS = [10, 20, 50];
const QUERY_KEY = ['admin', 'users'];

// ── Role badge ─────────────────────────────────────────────────────────────────

/**
 * Soft-colour role badge chip.
 *
 * @param {{ role: string }} props
 */
function RoleBadge({ role }) {
  const badge = ROLE_BADGE[role] ?? {
    color: '#64748B',
    bg: 'rgba(100,116,139,0.1)',
    border: 'rgba(100,116,139,0.2)',
  };
  const label = role.replace('ROLE_', '');
  return (
    <Chip
      label={label}
      size="small"
      sx={{
        fontWeight: 700,
        fontSize: '0.7rem',
        height: 22,
        bgcolor: badge.bg,
        color: badge.color,
        border: `1px solid ${badge.border}`,
        borderRadius: '6px',
      }}
    />
  );
}

// ── Inline role-selector cell ─────────────────────────────────────────────────

/**
 * Renders the current role(s) of a user plus an in-row dropdown to change it.
 *
 * @param {{ userId: string, currentRoles: string[], onSave: (role: string) => void, isSaving: boolean }} props
 */
function RoleCell({ currentRoles, onSave, isSaving }) {
  const [selected, setSelected] = useState(currentRoles[0] ?? ROLES.EMPLOYEE);
  const changed = selected !== currentRoles[0];

  return (
    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
      <FormControl size="small" sx={{ minWidth: 120 }} disabled={isSaving}>
        <Select
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
          inputProps={{ 'aria-label': 'Select role' }}
          sx={{
            '& .MuiSelect-select': { py: '6px', fontSize: '0.8125rem' },
            borderRadius: '8px',
          }}
        >
          {ALL_ROLES.map(({ value, label }) => (
            <MenuItem key={value} value={value} sx={{ fontSize: '0.8125rem' }}>
              {label}
            </MenuItem>
          ))}
        </Select>
      </FormControl>
      {changed ? (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          <Tooltip title="Save role change">
            <span>
              <IconButton
                size="small"
                onClick={() => onSave(selected)}
                disabled={isSaving}
                aria-label="Save role"
                sx={{
                  width: 28,
                  height: 28,
                  bgcolor: 'rgba(79,70,229,0.1)',
                  color: 'primary.main',
                  borderRadius: '8px',
                  '&:hover': { bgcolor: 'rgba(79,70,229,0.2)' },
                }}
              >
                {isSaving ? (
                  <CircularProgress size={14} />
                ) : (
                  <CheckRoundedIcon sx={{ fontSize: 14 }} />
                )}
              </IconButton>
            </span>
          </Tooltip>
          <Tooltip title="Discard change">
            <IconButton
              size="small"
              onClick={() => setSelected(currentRoles[0] ?? ROLES.EMPLOYEE)}
              disabled={isSaving}
              aria-label="Discard role change"
              sx={{
                width: 28,
                height: 28,
                bgcolor: 'rgba(239,68,68,0.08)',
                color: 'error.main',
                borderRadius: '8px',
                '&:hover': { bgcolor: 'rgba(239,68,68,0.15)' },
              }}
            >
              <CloseRoundedIcon sx={{ fontSize: 14 }} />
            </IconButton>
          </Tooltip>
        </Box>
      ) : (
        <Box sx={{ display: 'flex', gap: 0.5 }}>
          {currentRoles.map((r) => (
            <RoleBadge key={r} role={r} />
          ))}
        </Box>
      )}
    </Box>
  );
}

// ── Status badge ──────────────────────────────────────────────────────────────

function StatusBadge({ enabled }) {
  return (
    <Box
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.4,
        px: 1,
        py: 0.25,
        borderRadius: '6px',
        bgcolor: enabled ? 'rgba(16,185,129,0.1)' : 'rgba(239,68,68,0.08)',
        border: '1px solid',
        borderColor: enabled ? 'rgba(16,185,129,0.2)' : 'rgba(239,68,68,0.15)',
      }}
    >
      <Box
        sx={{
          width: 6,
          height: 6,
          borderRadius: '50%',
          bgcolor: enabled ? '#10B981' : '#EF4444',
        }}
      />
      <Typography
        variant="caption"
        fontWeight={600}
        sx={{ color: enabled ? '#10B981' : '#EF4444', fontSize: '0.7rem' }}
      >
        {enabled ? 'Active' : 'Disabled'}
      </Typography>
    </Box>
  );
}

// ── User avatar ───────────────────────────────────────────────────────────────

function UserAvatar({ firstName, lastName }) {
  const initials = `${firstName?.[0] ?? ''}${lastName?.[0] ?? ''}`.toUpperCase() || '?';
  return (
    <Avatar
      sx={{
        width: 32,
        height: 32,
        background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
        fontSize: '0.75rem',
        fontWeight: 700,
        flexShrink: 0,
      }}
    >
      {initials}
    </Avatar>
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

  const showSnack = useCallback(
    (severity, message) => setSnack({ open: true, severity, message }),
    [],
  );
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
      const newRole = updated.roles[0]?.replace('ROLE_', '') ?? '';
      showSnack(
        'success',
        `Role updated to ${newRole} for ${updated.email}. The user must log out and log in again for the new role to take effect.`,
      );
    },
    onError: (err) => showSnack('error', err?.message ?? 'Failed to update role.'),
  });

  // ── Enable/disable mutation ────────────────────────────────────────────────
  const enabledMutation = useMutation({
    mutationFn: ({ userId, enabled }) => setUserEnabled(userId, enabled),
    onSuccess: (updated) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEY });
      showSnack(
        'success',
        `${updated.email} has been ${updated.isEnabled ? 'enabled' : 'disabled'}.`,
      );
    },
    onError: (err) => showSnack('error', err?.message ?? 'Failed to update account status.'),
  });

  const users = data?.content ?? [];
  const totalElements = data?.totalElements ?? 0;

  // ── Render ────────────────────────────────────────────────────────────────
  return (
    <>
      <Helmet>
        <title>User Management — PeopleCore HR</title>
      </Helmet>

      {/* Page header */}
      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box
          sx={{
            width: 44,
            height: 44,
            borderRadius: '12px',
            bgcolor: 'rgba(79,70,229,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <PeopleRoundedIcon sx={{ fontSize: 22, color: 'primary.main' }} />
        </Box>
        <Box>
          <Typography
            variant="h2"
            fontWeight={800}
            sx={{ letterSpacing: '-0.02em', mb: 0.25, lineHeight: 1.2 }}
          >
            User Management
          </Typography>
          <Typography variant="body2" color="text.secondary">
            View and manage all registered user accounts and their roles
          </Typography>
        </Box>
        {/* Summary chip */}
        {!isLoading && (
          <Box sx={{ ml: 'auto' }}>
            <Chip
              label={`${totalElements} user${totalElements !== 1 ? 's' : ''}`}
              size="small"
              sx={{
                fontWeight: 700,
                bgcolor: 'rgba(79,70,229,0.1)',
                color: 'primary.main',
                border: '1px solid rgba(79,70,229,0.2)',
              }}
            />
          </Box>
        )}
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error?.message ?? 'Failed to load users. Please try again.'}
        </Alert>
      )}

      <Card>
        <TableContainer>
          <Table size="small" aria-label="User accounts">
            <TableHead>
              <TableRow>
                <TableCell sx={{ pl: 2.5 }}>User</TableCell>
                <TableCell>Role</TableCell>
                <TableCell>Employee Record</TableCell>
                <TableCell align="center">Status</TableCell>
                <TableCell align="center">Enabled</TableCell>
                <TableCell>Joined</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                Array.from({ length: 8 }, (_, i) => (
                  <TableRow key={i}>
                    <TableCell sx={{ pl: 2.5 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <Skeleton variant="circular" width={32} height={32} />
                        <Box>
                          <Skeleton variant="text" width={120} height={18} />
                          <Skeleton variant="text" width={160} height={14} />
                        </Box>
                      </Box>
                    </TableCell>
                    {Array.from({ length: 5 }, (__, j) => (
                      <TableCell key={j}>
                        <Skeleton variant="text" width={80} />
                      </TableCell>
                    ))}
                  </TableRow>
                ))
              ) : users.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} align="center" sx={{ py: 8 }}>
                    <Box>
                      <Typography variant="body1" color="text.secondary" sx={{ mb: 0.5 }}>
                        👥 No user accounts found
                      </Typography>
                      <Typography variant="caption" color="text.disabled">
                        User accounts will appear here once created.
                      </Typography>
                    </Box>
                  </TableCell>
                </TableRow>
              ) : (
                users.map((user) => (
                  <TableRow key={user.id} hover>
                    {/* User column — avatar + name + email */}
                    <TableCell sx={{ pl: 2.5, py: 1.5 }}>
                      <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
                        <UserAvatar firstName={user.firstName} lastName={user.lastName} />
                        <Box sx={{ minWidth: 0 }}>
                          <Typography variant="body2" fontWeight={600} noWrap>
                            {user.firstName} {user.lastName}
                          </Typography>
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            noWrap
                            sx={{ display: 'block' }}
                          >
                            {user.email}
                          </Typography>
                        </Box>
                      </Box>
                    </TableCell>

                    {/* Role selector */}
                    <TableCell>
                      <RoleCell
                        currentRoles={user.roles}
                        onSave={(roleName) => roleMutation.mutate({ userId: user.id, roleName })}
                        isSaving={
                          roleMutation.isPending && roleMutation.variables?.userId === user.id
                        }
                      />
                    </TableCell>

                    {/* Employee record summary */}
                    <TableCell>
                      {user.employee ? (
                        <Box>
                          <Typography variant="caption" fontWeight={600} sx={{ display: 'block' }}>
                            {user.employee.employeeCode}
                          </Typography>
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{ display: 'block' }}
                          >
                            {user.employee.departmentName ?? '—'}
                          </Typography>
                          <Typography
                            variant="caption"
                            color="text.secondary"
                            sx={{ display: 'block' }}
                          >
                            {user.employee.jobTitle ?? '—'}
                          </Typography>
                        </Box>
                      ) : (
                        <Typography variant="caption" color="text.disabled">
                          No record
                        </Typography>
                      )}
                    </TableCell>

                    {/* Status badge */}
                    <TableCell align="center">
                      <StatusBadge enabled={user.isEnabled} />
                    </TableCell>

                    {/* Toggle switch */}
                    <TableCell align="center">
                      <Tooltip
                        title={
                          user.isEnabled ? 'Click to disable account' : 'Click to enable account'
                        }
                      >
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

                    {/* Joined date */}
                    <TableCell>
                      <Typography variant="caption" color="text.secondary">
                        {user.createdAt
                          ? new Date(user.createdAt).toLocaleDateString('en-US', {
                              year: 'numeric',
                              month: 'short',
                              day: 'numeric',
                            })
                          : '—'}
                      </Typography>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

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
          aria-label="User table pagination"
        />
      </Card>

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
          sx={{ width: '100%', borderRadius: '12px' }}
        >
          {snack.message}
        </Alert>
      </Snackbar>
    </>
  );
}
