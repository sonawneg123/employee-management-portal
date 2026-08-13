/**
 * @fileoverview EmployeeDetailsPage — full-page detail view for a single employee.
 *
 * Fetches the employee by UUID from the route param, renders {@link EmployeeDetails},
 * and provides Edit / Delete action buttons for authorised roles.
 *
 * States handled:
 * - Loading → skeleton via EmployeeDetails
 * - Not found (404) → alert with back button
 * - Error → alert with retry
 * - Loaded → EmployeeDetails + role-conditional action buttons
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useParams, useLocation } from 'react-router-dom';
import { Alert, Box, Button, Snackbar, Typography, alpha } from '@mui/material';
import ArrowBackRoundedIcon from '@mui/icons-material/ArrowBackRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import PeopleAltRoundedIcon from '@mui/icons-material/PeopleAltRounded';

import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';
import { useEmployee, useUpdateEmployee, useDeleteEmployee } from '@/hooks/useEmployees';
import { formatFullName } from '@/utils/employeeFormatters';

import EmployeeDetails from '@/components/employees/EmployeeDetails';
import EmployeeDialog from '@/components/employees/EmployeeDialog';
import DeleteEmployeeDialog from '@/components/employees/DeleteEmployeeDialog';

/**
 * @typedef {Object} SnackbarState
 * @property {boolean}           open
 * @property {'success'|'error'} severity
 * @property {string}            message
 */

/**
 * Individual employee detail page.
 *
 * @returns {JSX.Element}
 */
export default function EmployeeDetailsPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const location = useLocation();
  const { hasAnyRole } = useAuth();

  // Derive the list path by removing the /:id segment from the current URL.
  // Works for /admin/employees/:id, /hr/employees/:id, and /employees/:id.
  const listPath = location.pathname.replace(`/${id}`, '');

  // ── Permissions ────────────────────────────────────────────────────────────
  const canEdit = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canDelete = hasAnyRole([ROLES.ADMIN]); // DELETE /employees/** → ADMIN only

  // ── Data ───────────────────────────────────────────────────────────────────
  const { data: employee, isLoading, isError, error } = useEmployee(id);

  const updateMutation = useUpdateEmployee();
  const deleteMutation = useDeleteEmployee();

  // ── Dialog state ───────────────────────────────────────────────────────────
  const [editOpen, setEditOpen] = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  // ── Snackbar ───────────────────────────────────────────────────────────────
  /** @type {[SnackbarState, Function]} */
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  const showSnackbar = useCallback((severity, message) => {
    setSnackbar({ open: true, severity, message });
  }, []);

  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // ── Handlers ───────────────────────────────────────────────────────────────

  const handleEditSubmit = useCallback(
    async (payload) => {
      try {
        await updateMutation.mutateAsync({ id, payload });
        showSnackbar('success', 'Employee updated successfully.');
        setEditOpen(false);
      } catch (err) {
        if (!err?.violations) {
          showSnackbar('error', err?.message ?? 'Failed to update employee.');
        }
      }
    },
    [id, updateMutation, showSnackbar],
  );

  const handleConfirmDelete = useCallback(async () => {
    try {
      await deleteMutation.mutateAsync(id);
      showSnackbar('success', 'Employee deleted.');
      navigate(listPath, { replace: true });
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to delete employee.');
      setDeleteOpen(false);
    }
  }, [id, deleteMutation, navigate, showSnackbar, listPath]);

  // ── Page title ─────────────────────────────────────────────────────────────
  const pageTitle = employee
    ? formatFullName(employee.firstName, employee.lastName)
    : 'Employee Details';

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <>
      <Helmet>
        <title>{pageTitle} — PeopleCore HR</title>
      </Helmet>

      {/* Header row */}
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
            <PeopleAltRoundedIcon />
          </Box>
          <Box>
            <Box sx={{ mb: 0.5 }}>
              <Button
                startIcon={<ArrowBackRoundedIcon />}
                onClick={() => navigate(listPath)}
                variant="outlined"
                size="small"
                aria-label="Back to employees list"
                sx={{ borderRadius: '8px', fontWeight: 600 }}
              >
                Back
              </Button>
            </Box>
            <Typography variant="h2" fontWeight={800} sx={{ letterSpacing: '-0.02em' }}>
              {isLoading ? 'Loading…' : pageTitle}
            </Typography>
          </Box>
        </Box>

        {!isLoading && !isError && employee && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            {canEdit && (
              <Button
                variant="outlined"
                startIcon={<EditRoundedIcon />}
                onClick={() => setEditOpen(true)}
                aria-label="Edit employee"
                sx={{ borderRadius: '8px', fontWeight: 600 }}
              >
                Edit
              </Button>
            )}
            {canDelete && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteRoundedIcon />}
                onClick={() => setDeleteOpen(true)}
                aria-label="Delete employee"
                sx={{ borderRadius: '8px', fontWeight: 600 }}
              >
                Delete
              </Button>
            )}
          </Box>
        )}
      </Box>

      {/* 404 / Error states */}
      {isError && (
        <Alert
          severity={error?.status === 404 ? 'warning' : 'error'}
          action={
            error?.status !== 404 && (
              <Button color="inherit" size="small" onClick={() => window.location.reload()}>
                Retry
              </Button>
            )
          }
          sx={{ mb: 3, borderRadius: '10px' }}
        >
          {error?.status === 404
            ? 'Employee not found. They may have been deleted.'
            : (error?.message ?? 'Failed to load employee details.')}
        </Alert>
      )}

      {/* Main detail card */}
      <EmployeeDetails employee={employee} isLoading={isLoading} />

      {/* Edit dialog */}
      <EmployeeDialog
        open={editOpen}
        mode="edit"
        defaultValues={employee}
        isSubmitting={updateMutation.isPending}
        serverErrors={updateMutation.error?.violations}
        onSubmit={handleEditSubmit}
        onClose={() => setEditOpen(false)}
      />

      {/* Delete confirmation */}
      <DeleteEmployeeDialog
        open={deleteOpen}
        employee={employee ?? null}
        isDeleting={deleteMutation.isPending}
        onConfirm={handleConfirmDelete}
        onCancel={() => setDeleteOpen(false)}
      />

      {/* Snackbar */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={closeSnackbar}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
