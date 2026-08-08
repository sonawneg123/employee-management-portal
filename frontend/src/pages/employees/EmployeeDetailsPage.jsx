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
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Snackbar,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon      from '@mui/icons-material/Edit';
import DeleteIcon    from '@mui/icons-material/Delete';

import { useAuth }           from '@/contexts/AuthContext';
import { ROLES }             from '@/constants/roles';
import { ROUTES }            from '@/constants/routes';
import { useEmployee, useUpdateEmployee, useDeleteEmployee } from '@/hooks/useEmployees';
import { formatFullName }    from '@/utils/employeeFormatters';

import EmployeeDetails      from '@/components/employees/EmployeeDetails';
import EmployeeDialog       from '@/components/employees/EmployeeDialog';
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
  const { id }    = useParams();
  const navigate  = useNavigate();
  const { hasAnyRole } = useAuth();

  // ── Permissions ────────────────────────────────────────────────────────────
  const canEdit   = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canDelete = hasAnyRole([ROLES.ADMIN, ROLES.HR]);

  // ── Data ───────────────────────────────────────────────────────────────────
  const { data: employee, isLoading, isError, error, isFetching } = useEmployee(id);

  const updateMutation = useUpdateEmployee();
  const deleteMutation = useDeleteEmployee();

  // ── Dialog state ───────────────────────────────────────────────────────────
  const [editOpen,   setEditOpen]   = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);

  // ── Snackbar ───────────────────────────────────────────────────────────────
  /** @type {[SnackbarState, Function]} */
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  const showSnackbar = useCallback((severity, message) => {
    setSnackbar({ open: true, severity, message });
  }, []);

  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // ── Handlers ───────────────────────────────────────────────────────────────

  const handleEditSubmit = useCallback(async (payload) => {
    try {
      await updateMutation.mutateAsync({ id, payload });
      showSnackbar('success', 'Employee updated successfully.');
      setEditOpen(false);
    } catch (err) {
      if (!err?.violations) {
        showSnackbar('error', err?.message ?? 'Failed to update employee.');
      }
    }
  }, [id, updateMutation, showSnackbar]);

  const handleConfirmDelete = useCallback(async () => {
    try {
      await deleteMutation.mutateAsync(id);
      showSnackbar('success', 'Employee deleted.');
      navigate(ROUTES.EMPLOYEES, { replace: true });
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to delete employee.');
      setDeleteOpen(false);
    }
  }, [id, deleteMutation, navigate, showSnackbar]);

  // ── Page title ─────────────────────────────────────────────────────────────
  const pageTitle = employee
    ? formatFullName(employee.firstName, employee.lastName)
    : 'Employee Details';

  // ── Render ─────────────────────────────────────────────────────────────────
  return (
    <>
      <Helmet>
        <title>{pageTitle} — Employee Portal</title>
      </Helmet>

      {/* Header row */}
      <Box
        sx={{
          display:        'flex',
          alignItems:     'center',
          justifyContent: 'space-between',
          mb: 3,
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Button
            startIcon={<ArrowBackIcon />}
            onClick={() => navigate(ROUTES.EMPLOYEES)}
            variant="text"
            aria-label="Back to employees list"
          >
            Back
          </Button>
          <Typography variant="h5" fontWeight={700}>
            {isLoading ? 'Loading…' : pageTitle}
          </Typography>
        </Box>

        {!isLoading && !isError && employee && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            {canEdit && (
              <Button
                variant="outlined"
                startIcon={<EditIcon />}
                onClick={() => setEditOpen(true)}
                aria-label="Edit employee"
              >
                Edit
              </Button>
            )}
            {canDelete && (
              <Button
                variant="outlined"
                color="error"
                startIcon={<DeleteIcon />}
                onClick={() => setDeleteOpen(true)}
                aria-label="Delete employee"
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
          sx={{ mb: 2 }}
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
