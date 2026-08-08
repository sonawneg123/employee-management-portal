/**
 * @fileoverview DepartmentDetailsPage — full-page detail view for a single department.
 *
 * Fetches the department by UUID from the route param, renders:
 * - {@link DepartmentDetails}        — overview card
 * - {@link DepartmentStatisticsCard} — KPI sidebar
 * - {@link DepartmentEmployeeList}   — employees in the department
 *
 * Edit / Delete buttons are shown for ADMIN / HR roles.
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useParams } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Grid,
  Snackbar,
  Typography,
} from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import EditIcon      from '@mui/icons-material/Edit';
import DeleteIcon    from '@mui/icons-material/Delete';

import { useAuth }         from '@/contexts/AuthContext';
import { ROLES }           from '@/constants/roles';
import { ROUTES }          from '@/constants/routes';
import {
  useDepartment,
  useUpdateDepartment,
  useDeleteDepartment,
} from '@/hooks/useDepartmentHooks';

import DepartmentDetails        from '@/components/departments/DepartmentDetails';
import DepartmentStatisticsCard from '@/components/departments/DepartmentStatisticsCard';
import DepartmentEmployeeList   from '@/components/departments/DepartmentEmployeeList';
import DepartmentDialog         from '@/components/departments/DepartmentDialog';
import DeleteDepartmentDialog   from '@/components/departments/DeleteDepartmentDialog';

/**
 * Individual department detail page.
 *
 * @returns {JSX.Element}
 */
export default function DepartmentDetailsPage() {
  const { id }    = useParams();
  const navigate  = useNavigate();
  const { hasAnyRole } = useAuth();

  const canEdit   = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canDelete = hasAnyRole([ROLES.ADMIN, ROLES.HR]);

  const { data: department, isLoading, isError, error } = useDepartment(id);

  const updateMutation = useUpdateDepartment();
  const deleteMutation = useDeleteDepartment();

  const [editOpen,   setEditOpen]   = useState(false);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [snackbar,   setSnackbar]   = useState({ open: false, severity: 'success', message: '' });

  const showSnackbar  = useCallback((severity, message) => setSnackbar({ open: true, severity, message }), []);
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  const handleEditSubmit = useCallback(async (payload) => {
    try {
      await updateMutation.mutateAsync({ id, payload });
      showSnackbar('success', 'Department updated successfully.');
      setEditOpen(false);
    } catch (err) {
      if (!err?.violations) {
        showSnackbar('error', err?.message ?? 'Failed to update department.');
      }
    }
  }, [id, updateMutation, showSnackbar]);

  const handleConfirmDelete = useCallback(async () => {
    try {
      await deleteMutation.mutateAsync(id);
      showSnackbar('success', 'Department deleted.');
      navigate(ROUTES.DEPARTMENTS, { replace: true });
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to delete department.');
      setDeleteOpen(false);
    }
  }, [id, deleteMutation, navigate, showSnackbar]);

  const pageTitle = department?.name ?? 'Department Details';

  return (
    <>
      <Helmet>
        <title>{pageTitle} — Employee Portal</title>
      </Helmet>

      {/* Header */}
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
            onClick={() => navigate(ROUTES.DEPARTMENTS)}
            variant="text"
            aria-label="Back to departments list"
          >
            Back
          </Button>
          <Typography variant="h5" fontWeight={700}>
            {isLoading ? 'Loading…' : pageTitle}
          </Typography>
        </Box>

        {!isLoading && !isError && department && (
          <Box sx={{ display: 'flex', gap: 1 }}>
            {canEdit && (
              <Button
                variant="outlined"
                startIcon={<EditIcon />}
                onClick={() => setEditOpen(true)}
                aria-label="Edit department"
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
                aria-label="Delete department"
              >
                Delete
              </Button>
            )}
          </Box>
        )}
      </Box>

      {/* Error */}
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
            ? 'Department not found.'
            : (error?.message ?? 'Failed to load department details.')}
        </Alert>
      )}

      {/* Main content grid */}
      <Grid container spacing={3}>
        {/* Left: main detail card */}
        <Grid size={{ xs: 12, lg: 8 }}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <DepartmentDetails department={department} isLoading={isLoading} />
            {!isLoading && department && (
              <DepartmentEmployeeList departmentId={id} />
            )}
          </Box>
        </Grid>

        {/* Right: statistics card */}
        <Grid size={{ xs: 12, lg: 4 }}>
          <DepartmentStatisticsCard department={department} isLoading={isLoading} />
        </Grid>
      </Grid>

      {/* Edit dialog */}
      <DepartmentDialog
        open={editOpen}
        mode="edit"
        defaultValues={department}
        isSubmitting={updateMutation.isPending}
        serverErrors={updateMutation.error?.violations}
        onSubmit={handleEditSubmit}
        onClose={() => setEditOpen(false)}
      />

      {/* Delete confirmation */}
      <DeleteDepartmentDialog
        open={deleteOpen}
        department={department ?? null}
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
        <Alert onClose={closeSnackbar} severity={snackbar.severity} variant="filled" sx={{ width: '100%' }}>
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
