/**
 * @fileoverview DepartmentsPage — the main department management list view.
 *
 * Manages all query-param state (search, sort, pagination) locally and
 * passes them to {@link useDepartmentList}. Renders:
 *
 * - {@link DepartmentToolbar}        — search, sort, add, refresh, export
 * - {@link DepartmentTable}          — desktop sortable table
 * - {@link DepartmentCard} list      — mobile card list
 * - {@link DepartmentPagination}     — server-side pagination
 * - {@link DepartmentDialog}         — create / edit modal
 * - {@link DeleteDepartmentDialog}   — delete confirmation
 * - MUI Snackbar                     — success / error toasts
 *
 * Role guards:
 *   ADMIN / HR  → full CRUD
 *   MANAGER     → read-only
 *   EMPLOYEE    → redirected by ProtectedRoute (no access)
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  Alert,
  Box,
  Card,
  Snackbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useAuth }         from '@/contexts/AuthContext';
import { ROLES }           from '@/constants/roles';
import {
  DEPARTMENT_DEFAULT_PAGE_SIZE,
  DEPARTMENT_DEFAULT_SORT,
  DEPARTMENT_DEFAULT_DIRECTION,
  DEPT_CSV_HEADERS,
  DEPT_CSV_FIELDS,
} from '@/constants/departmentConstants';
import {
  useDepartmentList,
  useCreateDepartment,
  useUpdateDepartment,
  useDeleteDepartment,
} from '@/hooks/useDepartmentHooks';
import { buildDeptCsvString, downloadDeptCsv } from '@/utils/departmentFormatters';

import DepartmentToolbar      from '@/components/departments/DepartmentToolbar';
import DepartmentTable        from '@/components/departments/DepartmentTable';
import DepartmentCard         from '@/components/departments/DepartmentCard';
import DepartmentPagination   from '@/components/departments/DepartmentPagination';
import DepartmentDialog       from '@/components/departments/DepartmentDialog';
import DeleteDepartmentDialog from '@/components/departments/DeleteDepartmentDialog';

/**
 * @typedef {'create'|'edit'|null} DialogMode
 */

/**
 * Department management list page.
 *
 * @returns {JSX.Element}
 */
export default function DepartmentsPage() {
  const theme    = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down('md'));
  const navigate = useNavigate();
  const location = useLocation();
  const { hasAnyRole } = useAuth();

  // Derive the list base path from the current location so that navigation
  // to the detail view stays within the same role-scoped prefix
  // (/admin/departments, /hr/departments, or /departments).
  const listBase = location.pathname.replace(/\/$/, '');

  // ── Role permissions ───────────────────────────────────────────────────────
  const canCreate = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canEdit   = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canDelete = hasAnyRole([ROLES.ADMIN]);           // DELETE /departments/** → ADMIN only

  // ── Query params ───────────────────────────────────────────────────────────
  const [page,      setPage]      = useState(0);
  const [pageSize,  setPageSize]  = useState(DEPARTMENT_DEFAULT_PAGE_SIZE);
  const [sort,      setSort]      = useState(DEPARTMENT_DEFAULT_SORT);
  const [direction, setDirection] = useState(DEPARTMENT_DEFAULT_DIRECTION);
  const [search,    setSearch]    = useState('');

  // ── Dialog state ───────────────────────────────────────────────────────────
  /** @type {[DialogMode, Function]} */
  const [dialogMode,   setDialogMode]   = useState(null);
  const [editDept,     setEditDept]     = useState(null);
  const [deleteDept,   setDeleteDept]   = useState(null);

  // ── Snackbar state ─────────────────────────────────────────────────────────
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  // ── Data ───────────────────────────────────────────────────────────────────
  const {
    data,
    isLoading,
    isFetching,
    isError,
    error,
    refresh,
  } = useDepartmentList({ page, size: pageSize, sort, direction, search });

  const createMutation = useCreateDepartment();
  const updateMutation = useUpdateDepartment();
  const deleteMutation = useDeleteDepartment();

  const departments   = data?.content        ?? [];
  const totalElements = data?.totalElements  ?? 0;

  // ── Snackbar helpers ───────────────────────────────────────────────────────

  const showSnackbar = useCallback((severity, message) => {
    setSnackbar({ open: true, severity, message });
  }, []);

  const closeSnackbar = useCallback(() => {
    setSnackbar((s) => ({ ...s, open: false }));
  }, []);

  // ── Filter / sort handlers ─────────────────────────────────────────────────

  const handleSearchChange    = useCallback((v) => { setSearch(v);  setPage(0); }, []);
  const handleSortChange      = useCallback((field, dir) => { setSort(field); setDirection(dir); setPage(0); }, []);
  const handleDirectionChange = useCallback((dir) => { setDirection(dir); setPage(0); }, []);
  const handleClearSearch     = useCallback(() => { setSearch(''); setPage(0); }, []);

  // ── Dialog handlers ────────────────────────────────────────────────────────

  const handleOpenCreate = useCallback(() => {
    setEditDept(null);
    setDialogMode('create');
  }, []);

  const handleOpenEdit = useCallback((dept) => {
    setEditDept(dept);
    setDialogMode('edit');
  }, []);

  const handleCloseDialog = useCallback(() => {
    setDialogMode(null);
    setEditDept(null);
  }, []);

  // ── CRUD handlers ──────────────────────────────────────────────────────────

  const handleSubmit = useCallback(async (payload) => {
    try {
      if (dialogMode === 'create') {
        await createMutation.mutateAsync(payload);
        showSnackbar('success', 'Department created successfully.');
      } else {
        await updateMutation.mutateAsync({ id: editDept.id, payload });
        showSnackbar('success', 'Department updated successfully.');
      }
      handleCloseDialog();
    } catch (err) {
      if (!err?.violations) {
        showSnackbar('error', err?.message ?? 'An error occurred. Please try again.');
      }
    }
  }, [dialogMode, editDept, createMutation, updateMutation, showSnackbar, handleCloseDialog]);

  const handleOpenDelete  = useCallback((dept) => setDeleteDept(dept), []);
  const handleCloseDelete = useCallback(() => setDeleteDept(null), []);

  const handleConfirmDelete = useCallback(async () => {
    if (!deleteDept) return;
    try {
      await deleteMutation.mutateAsync(deleteDept.id);
      showSnackbar('success', 'Department deleted successfully.');
      handleCloseDelete();
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to delete department.');
    }
  }, [deleteDept, deleteMutation, showSnackbar, handleCloseDelete]);

  const handleView = useCallback((dept) => {
    navigate(`${listBase}/${dept.id}`);
  }, [navigate, listBase]);

  // ── CSV Export ─────────────────────────────────────────────────────────────

  const handleExport = useCallback(() => {
    if (!departments.length) return;
    const csv = buildDeptCsvString(departments, DEPT_CSV_HEADERS, DEPT_CSV_FIELDS);
    downloadDeptCsv(csv, `departments-page-${page + 1}.csv`);
    showSnackbar('success', `Exported ${departments.length} department records.`);
  }, [departments, page, showSnackbar]);

  // ── Server errors for form ─────────────────────────────────────────────────
  const serverErrors =
    (createMutation.error?.violations ?? updateMutation.error?.violations) || undefined;

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <>
      <Helmet>
        <title>Departments — Employee Portal</title>
      </Helmet>

      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>Departments</Typography>
        <Typography variant="body2" color="text.secondary">
          Manage all organisational departments
        </Typography>
      </Box>

      <Card variant="outlined">
        <DepartmentToolbar
          search={search}
          sort={sort}
          direction={direction}
          totalElements={totalElements}
          isFetching={isFetching}
          canCreate={canCreate}
          onSearchChange={handleSearchChange}
          onSortChange={(field) => handleSortChange(field, direction)}
          onDirectionChange={handleDirectionChange}
          onAdd={handleOpenCreate}
          onRefresh={refresh}
          onExport={handleExport}
          onClearSearch={handleClearSearch}
        />

        {/* Desktop table */}
        {!isMobile && (
          <DepartmentTable
            departments={departments}
            isLoading={isLoading}
            isFetching={isFetching}
            isError={isError}
            error={error}
            sort={sort}
            direction={direction}
            hasSearch={Boolean(search)}
            canEdit={canEdit}
            canDelete={canDelete}
            onSort={handleSortChange}
            onView={handleView}
            onEdit={handleOpenEdit}
            onDelete={handleOpenDelete}
            onRetry={refresh}
            onClearSearch={handleClearSearch}
            onAdd={handleOpenCreate}
            canCreate={canCreate}
          />
        )}

        {/* Mobile card list */}
        {isMobile && (
          <Box sx={{ p: 2 }}>
            {isLoading
              ? Array.from({ length: 5 }, (_, i) => (
                  <Box key={i} sx={{ mb: 1.5, height: 90, bgcolor: 'action.hover', borderRadius: 2 }} />
                ))
              : departments.map((dept) => (
                  <DepartmentCard
                    key={dept.id}
                    department={dept}
                    onClick={() => handleView(dept)}
                    onMenuOpen={(e, d) => handleOpenEdit(d)}
                  />
                ))}
          </Box>
        )}

        <DepartmentPagination
          page={page}
          pageSize={pageSize}
          totalElements={totalElements}
          onPageChange={setPage}
          onPageSizeChange={(size) => { setPageSize(size); setPage(0); }}
          disabled={isLoading || isFetching}
        />
      </Card>

      {/* Create / Edit dialog */}
      <DepartmentDialog
        open={dialogMode !== null}
        mode={dialogMode ?? 'create'}
        defaultValues={editDept ?? undefined}
        isSubmitting={createMutation.isPending || updateMutation.isPending}
        serverErrors={serverErrors}
        onSubmit={handleSubmit}
        onClose={handleCloseDialog}
      />

      {/* Delete confirmation */}
      <DeleteDepartmentDialog
        open={Boolean(deleteDept)}
        department={deleteDept}
        isDeleting={deleteMutation.isPending}
        onConfirm={handleConfirmDelete}
        onCancel={handleCloseDelete}
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
