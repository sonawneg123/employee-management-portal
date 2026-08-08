/**
 * @fileoverview EmployeesPage — the main employee management list view.
 *
 * Manages all query param state (search, filters, sort, pagination) locally
 * and passes them to {@link useEmployees}. Renders:
 *
 * - {@link EmployeeToolbar}      — search, filters, sort, add, refresh, export
 * - {@link EmployeeTable}        — desktop sortable table (hidden on xs/sm)
 * - {@link EmployeeCard} list    — mobile card list  (hidden on md+)
 * - {@link EmployeePagination}   — server-side pagination
 * - {@link EmployeeDialog}       — create / edit modal
 * - {@link DeleteEmployeeDialog} — delete confirmation
 * - MUI Snackbar                 — success / error toasts
 *
 * Role guards hide the Add / Edit / Delete actions based on the current user.
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Card,
  Snackbar,
  Typography,
  useMediaQuery,
  useTheme,
} from '@mui/material';
import { useAuth }             from '@/contexts/AuthContext';
import { ROLES }               from '@/constants/roles';
import { ROUTES }              from '@/constants/routes';
import {
  EMPLOYEE_DEFAULT_PAGE_SIZE,
  EMPLOYEE_DEFAULT_SORT,
  EMPLOYEE_DEFAULT_DIRECTION,
  CSV_HEADERS,
  CSV_FIELDS,
} from '@/constants/employeeConstants';
import {
  useEmployees,
  useCreateEmployee,
  useUpdateEmployee,
  useDeleteEmployee,
} from '@/hooks/useEmployees';
import { buildCsvString, downloadCsv } from '@/utils/employeeFormatters';

import EmployeeToolbar        from '@/components/employees/EmployeeToolbar';
import EmployeeTable          from '@/components/employees/EmployeeTable';
import EmployeeCard           from '@/components/employees/EmployeeCard';
import EmployeePagination     from '@/components/employees/EmployeePagination';
import EmployeeDialog         from '@/components/employees/EmployeeDialog';
import DeleteEmployeeDialog   from '@/components/employees/DeleteEmployeeDialog';

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * @typedef {'create'|'edit'|null} DialogMode
 */

/**
 * @typedef {Object} SnackbarState
 * @property {boolean}          open
 * @property {'success'|'error'} severity
 * @property {string}           message
 */

// ── Component ─────────────────────────────────────────────────────────────────

/**
 * Employee management list page.
 *
 * @returns {JSX.Element}
 */
export default function EmployeesPage() {
  const theme     = useTheme();
  const isMobile  = useMediaQuery(theme.breakpoints.down('md'));
  const navigate  = useNavigate();
  const { user, hasAnyRole } = useAuth();

  // ── Role permissions ───────────────────────────────────────────────────────
  const canCreate = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canEdit   = hasAnyRole([ROLES.ADMIN, ROLES.HR]);
  const canDelete = hasAnyRole([ROLES.ADMIN, ROLES.HR]);

  // ── Query params state ─────────────────────────────────────────────────────
  const [page,         setPage]         = useState(0);
  const [pageSize,     setPageSize]     = useState(EMPLOYEE_DEFAULT_PAGE_SIZE);
  const [sort,         setSort]         = useState(EMPLOYEE_DEFAULT_SORT);
  const [direction,    setDirection]    = useState(EMPLOYEE_DEFAULT_DIRECTION);
  const [search,       setSearch]       = useState('');
  const [departmentId, setDepartmentId] = useState('');
  const [status,       setStatus]       = useState('');

  // ── Dialog state ───────────────────────────────────────────────────────────
  /** @type {[DialogMode, Function]} */
  const [dialogMode,    setDialogMode]    = useState(null);
  const [editEmployee,  setEditEmployee]  = useState(null);
  const [deleteTarget,  setDeleteTarget]  = useState(null);

  // ── Snackbar state ─────────────────────────────────────────────────────────
  /** @type {[SnackbarState, Function]} */
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  // ── Data hooks ─────────────────────────────────────────────────────────────
  const {
    data,
    isLoading,
    isFetching,
    isError,
    error,
    refresh,
  } = useEmployees({ page, size: pageSize, sort, direction, search, departmentId, status });

  const createMutation = useCreateEmployee();
  const updateMutation = useUpdateEmployee();
  const deleteMutation = useDeleteEmployee();

  const employees     = data?.content        ?? [];
  const totalElements = data?.totalElements  ?? 0;
  const hasFilters    = Boolean(search || departmentId || status);

  // ── Snackbar helpers ───────────────────────────────────────────────────────

  /**
   * @param {'success'|'error'} severity
   * @param {string} message
   */
  const showSnackbar = useCallback((severity, message) => {
    setSnackbar({ open: true, severity, message });
  }, []);

  const closeSnackbar = useCallback(() => {
    setSnackbar((s) => ({ ...s, open: false }));
  }, []);

  // ── Filter / sort handlers ─────────────────────────────────────────────────

  const handleSearchChange    = useCallback((v) => { setSearch(v);       setPage(0); }, []);
  const handleDepartmentChange = useCallback((v) => { setDepartmentId(v); setPage(0); }, []);
  const handleStatusChange    = useCallback((v) => { setStatus(v);       setPage(0); }, []);
  const handleSortChange      = useCallback((field, dir) => { setSort(field); setDirection(dir); setPage(0); }, []);
  const handleDirectionChange = useCallback((dir) => { setDirection(dir); setPage(0); }, []);

  const handleClearFilters = useCallback(() => {
    setSearch('');
    setDepartmentId('');
    setStatus('');
    setPage(0);
  }, []);

  // ── Dialog handlers ────────────────────────────────────────────────────────

  const handleOpenCreate = useCallback(() => {
    setEditEmployee(null);
    setDialogMode('create');
  }, []);

  const handleOpenEdit = useCallback((emp) => {
    setEditEmployee(emp);
    setDialogMode('edit');
  }, []);

  const handleCloseDialog = useCallback(() => {
    setDialogMode(null);
    setEditEmployee(null);
  }, []);

  // ── CRUD handlers ──────────────────────────────────────────────────────────

  /**
   * @param {Object} payload
   */
  const handleSubmit = useCallback(async (payload) => {
    try {
      if (dialogMode === 'create') {
        await createMutation.mutateAsync(payload);
        showSnackbar('success', 'Employee created successfully.');
      } else {
        await updateMutation.mutateAsync({ id: editEmployee.id, payload });
        showSnackbar('success', 'Employee updated successfully.');
      }
      handleCloseDialog();
    } catch (err) {
      // Server violations are handled by EmployeeForm via serverErrors prop
      if (!err?.violations) {
        showSnackbar('error', err?.message ?? 'An error occurred. Please try again.');
      }
    }
  }, [dialogMode, editEmployee, createMutation, updateMutation, showSnackbar, handleCloseDialog]);

  const handleOpenDelete = useCallback((emp) => {
    setDeleteTarget(emp);
  }, []);

  const handleCloseDelete = useCallback(() => {
    setDeleteTarget(null);
  }, []);

  const handleConfirmDelete = useCallback(async () => {
    if (!deleteTarget) return;
    try {
      await deleteMutation.mutateAsync(deleteTarget.id);
      showSnackbar('success', 'Employee deleted successfully.');
      handleCloseDelete();
    } catch (err) {
      showSnackbar('error', err?.message ?? 'Failed to delete employee.');
    }
  }, [deleteTarget, deleteMutation, showSnackbar, handleCloseDelete]);

  // ── Navigation ─────────────────────────────────────────────────────────────

  const handleView = useCallback((emp) => {
    navigate(`${ROUTES.EMPLOYEES}/${emp.id}`);
  }, [navigate]);

  // ── CSV Export ─────────────────────────────────────────────────────────────

  const handleExport = useCallback(() => {
    if (!employees.length) return;
    const csv = buildCsvString(employees, CSV_HEADERS, CSV_FIELDS);
    downloadCsv(csv, `employees-page-${page + 1}.csv`);
    showSnackbar('success', `Exported ${employees.length} employee records.`);
  }, [employees, page, showSnackbar]);

  // ── Server errors for form ─────────────────────────────────────────────────
  const serverErrors =
    (createMutation.error?.violations ?? updateMutation.error?.violations) || undefined;

  // ── Render ─────────────────────────────────────────────────────────────────

  return (
    <>
      <Helmet>
        <title>Employees — Employee Portal</title>
      </Helmet>

      {/* Page heading */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>
          Employees
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Manage all employee records
        </Typography>
      </Box>

      <Card variant="outlined">
        {/* Toolbar */}
        <EmployeeToolbar
          search={search}
          departmentId={departmentId}
          status={status}
          sort={sort}
          direction={direction}
          totalElements={totalElements}
          isFetching={isFetching}
          canCreate={canCreate}
          onSearchChange={handleSearchChange}
          onDepartmentChange={handleDepartmentChange}
          onStatusChange={handleStatusChange}
          onSortChange={(field) => handleSortChange(field, direction)}
          onDirectionChange={handleDirectionChange}
          onAdd={handleOpenCreate}
          onRefresh={refresh}
          onExport={handleExport}
          onClearFilters={handleClearFilters}
        />

        {/* Desktop table */}
        {!isMobile && (
          <EmployeeTable
            employees={employees}
            isLoading={isLoading}
            isFetching={isFetching}
            isError={isError}
            error={error}
            sort={sort}
            direction={direction}
            hasFilters={hasFilters}
            canEdit={canEdit}
            canDelete={canDelete}
            onSort={handleSortChange}
            onView={handleView}
            onEdit={handleOpenEdit}
            onDelete={handleOpenDelete}
            onRetry={refresh}
            onClearFilters={handleClearFilters}
            onAdd={handleOpenCreate}
            canCreate={canCreate}
          />
        )}

        {/* Mobile card list */}
        {isMobile && (
          <Box sx={{ p: 2 }}>
            {isLoading
              ? Array.from({ length: 6 }, (_, i) => (
                  <Box key={i} sx={{ mb: 1.5, borderRadius: 2, overflow: 'hidden' }}>
                    <Box sx={{ height: 100, bgcolor: 'action.hover', borderRadius: 2 }} />
                  </Box>
                ))
              : employees.map((emp) => (
                  <EmployeeCard
                    key={emp.id}
                    employee={emp}
                    onClick={() => handleView(emp)}
                    onMenuOpen={(e, employee) => {
                      // Reuse the same actions; open menu anchored to mobile card button
                      handleOpenEdit(employee);
                    }}
                  />
                ))}
          </Box>
        )}

        {/* Pagination */}
        <EmployeePagination
          page={page}
          pageSize={pageSize}
          totalElements={totalElements}
          onPageChange={setPage}
          onPageSizeChange={(size) => { setPageSize(size); setPage(0); }}
          disabled={isLoading || isFetching}
        />
      </Card>

      {/* Create / Edit dialog */}
      <EmployeeDialog
        open={dialogMode !== null}
        mode={dialogMode ?? 'create'}
        defaultValues={editEmployee ?? undefined}
        isSubmitting={createMutation.isPending || updateMutation.isPending}
        serverErrors={serverErrors}
        onSubmit={handleSubmit}
        onClose={handleCloseDialog}
      />

      {/* Delete confirmation */}
      <DeleteEmployeeDialog
        open={Boolean(deleteTarget)}
        employee={deleteTarget}
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
