/**
 * @fileoverview ManagerTasksPage — task management list and creation for managers/HR.
 *
 * Phase 6A: initial list + create + delete.
 * Phase 6C-6E: category filter, URGENT priority, server-side dashboard stats,
 *              employee availability selector, completion percentage stat card.
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Card,
  Chip,
  CircularProgress,
  Dialog,
  DialogContent,
  DialogTitle,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  LinearProgress,
  MenuItem,
  Paper,
  Select,
  Snackbar,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  Tooltip,
  Typography,
  alpha,
  useTheme,
} from '@mui/material';
import AddRoundedIcon from '@mui/icons-material/AddRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded';
import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import HourglassEmptyRoundedIcon from '@mui/icons-material/HourglassEmptyRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';
import PendingActionsRoundedIcon from '@mui/icons-material/PendingActionsRounded';
import PriorityHighRoundedIcon from '@mui/icons-material/PriorityHighRounded';

import { ROUTES } from '@/constants/routes';
import {
  useCreatedTasks,
  useCreateTask,
  useDeleteTask,
  useEmployeeAvailability,
  useTaskDashboardStats,
} from '@/hooks/useTaskHooks';
import TaskStatusChip from '@/components/tasks/TaskStatusChip';
import TaskPriorityChip from '@/components/tasks/TaskPriorityChip';
import TaskForm from '@/components/tasks/TaskForm';
import PageHeader from '@/components/common/PageHeader';
import { categoryLabel, TASK_CATEGORIES } from '@/components/tasks/TaskForm';

const TASK_STATUSES = ['DRAFT', 'ASSIGNED', 'IN_PROGRESS', 'SUBMITTED', 'COMPLETED', 'CHANGES_REQUESTED', 'REJECTED'];
const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

function StatCard({ icon, label, value, color, extra }) {
  const theme = useTheme();
  return (
    <Card
      variant="outlined"
      sx={{
        p: 2.5,
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        borderLeft: `4px solid ${color || theme.palette.primary.main}`,
      }}
    >
      <Box
        sx={{
          width: 44,
          height: 44,
          borderRadius: 2,
          bgcolor: alpha(color || theme.palette.primary.main, 0.12),
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          color: color || theme.palette.primary.main,
          flexShrink: 0,
        }}
      >
        {icon}
      </Box>
      <Box sx={{ flex: 1, minWidth: 0 }}>
        <Typography variant="h5" fontWeight={700}>{value ?? '—'}</Typography>
        <Typography variant="body2" color="text.secondary">{label}</Typography>
        {extra}
      </Box>
    </Card>
  );
}

const FORM_INITIAL = {
  title: '',
  description: '',
  guidelines: '',
  acceptanceCriteria: '',
  assignedEmployeeId: '',
  priority: 'MEDIUM',
  dueDate: '',
  estimatedHours: '',
  category: '',
};

/**
 * Manager / HR task management page.
 *
 * @returns {JSX.Element}
 */
export default function ManagerTasksPage() {
  const navigate = useNavigate();
  const theme = useTheme();

  // ── Filters ────────────────────────────────────────────────────────────────
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [sort, setSort] = useState('createdAt');
  const [direction, setDirection] = useState('desc');

  // ── Dialog state ───────────────────────────────────────────────────────────
  const [createOpen, setCreateOpen] = useState(false);
  const [formValues, setFormValues] = useState(FORM_INITIAL);
  const [formErrors, setFormErrors] = useState({});

  // ── Snackbar ───────────────────────────────────────────────────────────────
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });
  const showSnackbar = useCallback((severity, message) => setSnackbar({ open: true, severity, message }), []);
  const closeSnackbar = useCallback(() => setSnackbar((s) => ({ ...s, open: false })), []);

  // ── Tasks query ────────────────────────────────────────────────────────────
  const tasksQuery = useCreatedTasks({
    page,
    size: pageSize,
    sort,
    direction,
    ...(statusFilter && { status: statusFilter }),
    ...(priorityFilter && { priority: priorityFilter }),
    ...(categoryFilter && { category: categoryFilter }),
  });

  // ── Employee availability (for assignment dropdown) ────────────────────────
  const availabilityQuery = useEmployeeAvailability();
  const availabilityEmployees = availabilityQuery.data ?? [];

  // ── Server-side dashboard stats ────────────────────────────────────────────
  const statsQuery = useTaskDashboardStats();
  const stats = statsQuery.data ?? {};

  // ── Mutations ──────────────────────────────────────────────────────────────
  const createMutation = useCreateTask();
  const deleteMutation = useDeleteTask();

  const tasks = tasksQuery.data?.content ?? [];
  const totalElements = tasksQuery.data?.totalElements ?? 0;

  // ── Form handlers ──────────────────────────────────────────────────────────
  const handleFormChange = useCallback((field, value) => {
    setFormValues((prev) => ({ ...prev, [field]: value }));
    if (formErrors[field]) setFormErrors((e) => ({ ...e, [field]: undefined }));
  }, [formErrors]);

  const validateForm = () => {
    const errs = {};
    if (!formValues.title?.trim()) errs.title = 'Title is required';
    if (!formValues.dueDate) errs.dueDate = 'Due date is required';
    return errs;
  };

  const handleCreate = async () => {
    const errs = validateForm();
    if (Object.keys(errs).length > 0) { setFormErrors(errs); return; }

    const payload = {
      title: formValues.title.trim(),
      description: formValues.description || null,
      guidelines: formValues.guidelines || null,
      acceptanceCriteria: formValues.acceptanceCriteria || null,
      assignedEmployeeId: formValues.assignedEmployeeId || null,
      priority: formValues.priority || 'MEDIUM',
      dueDate: formValues.dueDate,
      estimatedHours: formValues.estimatedHours ? Number(formValues.estimatedHours) : null,
      category: formValues.category || null,
    };

    try {
      await createMutation.mutateAsync(payload);
      setCreateOpen(false);
      setFormValues(FORM_INITIAL);
      setFormErrors({});
      showSnackbar('success', 'Task created successfully.');
    } catch (err) {
      const detail = err?.response?.data?.detail ?? 'Failed to create task.';
      showSnackbar('error', detail);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this task? This action cannot be undone.')) return;
    try {
      await deleteMutation.mutateAsync(id);
      showSnackbar('success', 'Task deleted.');
    } catch {
      showSnackbar('error', 'Failed to delete task.');
    }
  };

  const handleViewDetail = (id) => navigate(ROUTES.MANAGER_TASK_DETAIL(id));

  const completionPct = stats.completionPercentage ?? null;

  return (
    <>
      <Helmet><title>Task Management | Employee Portal</title></Helmet>

      <Box sx={{ p: { xs: 2, md: 3 } }}>
        <PageHeader
          title="Task Management"
          subtitle="Create and manage tasks for your team"
          action={
            <Button
              variant="contained"
              startIcon={<AddRoundedIcon />}
              onClick={() => setCreateOpen(true)}
            >
              Create Task
            </Button>
          }
        />

        {/* ── Stat cards ──────────────────────────────────────────────────── */}
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatCard icon={<AssignmentRoundedIcon />} label="Total" value={stats.totalTasks} color={theme.palette.primary.main} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatCard icon={<PendingActionsRoundedIcon />} label="Assigned" value={stats.assigned} color={theme.palette.info.main} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatCard icon={<HourglassEmptyRoundedIcon />} label="In Progress" value={stats.inProgress} color={theme.palette.primary.main} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatCard icon={<PriorityHighRoundedIcon />} label="Urgent" value={stats.urgent} color={theme.palette.error.main} />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatCard icon={<CheckCircleOutlineRoundedIcon />} label="Completed" value={stats.completed} color={theme.palette.success.main}
              extra={
                completionPct !== null ? (
                  <Box sx={{ mt: 0.5 }}>
                    <LinearProgress variant="determinate" value={completionPct} color="success" sx={{ borderRadius: 1, height: 4 }} />
                    <Typography variant="caption" color="text.secondary">{completionPct}%</Typography>
                  </Box>
                ) : null
              }
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 4, md: 2 }}>
            <StatCard icon={<ErrorOutlineRoundedIcon />} label="Overdue" value={stats.overdue} color={theme.palette.error.main} />
          </Grid>
        </Grid>

        {/* ── Filters ─────────────────────────────────────────────────────── */}
        <Card variant="outlined" sx={{ mb: 2 }}>
          <Box sx={{ p: 2 }}>
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2} alignItems="flex-start" flexWrap="wrap">
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>Status</InputLabel>
                <Select value={statusFilter} onChange={(e) => { setStatusFilter(e.target.value); setPage(0); }} label="Status">
                  <MenuItem value="">All</MenuItem>
                  {TASK_STATUSES.map((s) => <MenuItem key={s} value={s}>{s.replace(/_/g, ' ')}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <InputLabel>Priority</InputLabel>
                <Select value={priorityFilter} onChange={(e) => { setPriorityFilter(e.target.value); setPage(0); }} label="Priority">
                  <MenuItem value="">All</MenuItem>
                  {TASK_PRIORITIES.map((p) => <MenuItem key={p} value={p}>{p}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 150 }}>
                <InputLabel>Category</InputLabel>
                <Select value={categoryFilter} onChange={(e) => { setCategoryFilter(e.target.value); setPage(0); }} label="Category">
                  <MenuItem value="">All Categories</MenuItem>
                  {TASK_CATEGORIES.map((c) => <MenuItem key={c} value={c}>{categoryLabel(c)}</MenuItem>)}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>Sort By</InputLabel>
                <Select value={sort} onChange={(e) => setSort(e.target.value)} label="Sort By">
                  <MenuItem value="createdAt">Created</MenuItem>
                  <MenuItem value="dueDate">Due Date</MenuItem>
                  <MenuItem value="priority">Priority</MenuItem>
                  <MenuItem value="status">Status</MenuItem>
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Direction</InputLabel>
                <Select value={direction} onChange={(e) => setDirection(e.target.value)} label="Direction">
                  <MenuItem value="desc">Newest First</MenuItem>
                  <MenuItem value="asc">Oldest First</MenuItem>
                </Select>
              </FormControl>
            </Stack>
          </Box>
        </Card>

        {/* ── Task table ──────────────────────────────────────────────────── */}
        {tasksQuery.isLoading ? (
          <Box sx={{ display: 'flex', justifyContent: 'center', py: 6 }}>
            <CircularProgress />
          </Box>
        ) : tasksQuery.isError ? (
          <Alert severity="error">Failed to load tasks. Please try again.</Alert>
        ) : (
          <Paper variant="outlined">
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Task</TableCell>
                    <TableCell>Assigned To</TableCell>
                    <TableCell>Priority</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Due Date</TableCell>
                    <TableCell>Created</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tasks.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                        No tasks found.
                      </TableCell>
                    </TableRow>
                  ) : (
                    tasks.map((task) => (
                      <TableRow key={task.id} hover>
                        <TableCell>
                          <Typography variant="body2" fontWeight={600} noWrap sx={{ maxWidth: 200 }}>
                            {task.title}
                          </Typography>
                          {task.category && (
                            <Chip label={categoryLabel(task.category)} size="small" variant="outlined" sx={{ mt: 0.25, height: 18, fontSize: '0.65rem' }} />
                          )}
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {task.assignedEmployeeName ?? <em style={{ color: '#888' }}>Unassigned</em>}
                          </Typography>
                        </TableCell>
                        <TableCell><TaskPriorityChip priority={task.priority} /></TableCell>
                        <TableCell><TaskStatusChip status={task.status} overdue={task.overdue} /></TableCell>
                        <TableCell>
                          <Typography variant="body2" color={task.overdue ? 'error.main' : 'text.primary'}>
                            {task.dueDate ? new Date(task.dueDate).toLocaleDateString() : '—'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="caption" color="text.secondary">
                            {new Date(task.createdAt).toLocaleDateString()}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          <Stack direction="row" spacing={0.5} justifyContent="center">
                            <Tooltip title="View Details">
                              <IconButton size="small" onClick={() => handleViewDetail(task.id)}>
                                <VisibilityRoundedIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                            <Tooltip title="Delete">
                              <IconButton
                                size="small"
                                color="error"
                                onClick={() => handleDelete(task.id)}
                                disabled={deleteMutation.isPending}
                              >
                                <DeleteOutlineRoundedIcon fontSize="small" />
                              </IconButton>
                            </Tooltip>
                          </Stack>
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
              onPageChange={(_, newPage) => setPage(newPage)}
              rowsPerPage={pageSize}
              onRowsPerPageChange={(e) => { setPageSize(parseInt(e.target.value)); setPage(0); }}
              rowsPerPageOptions={[10, 20, 50]}
            />
          </Paper>
        )}
      </Box>

      {/* ── Create task dialog ──────────────────────────────────────────────── */}
      <Dialog open={createOpen} onClose={() => setCreateOpen(false)} maxWidth="md" fullWidth>
        <DialogTitle>Create New Task</DialogTitle>
        <DialogContent sx={{ pt: 2 }}>
          <TaskForm
            values={formValues}
            errors={formErrors}
            onChange={handleFormChange}
            onSubmit={handleCreate}
            onCancel={() => { setCreateOpen(false); setFormValues(FORM_INITIAL); setFormErrors({}); }}
            employees={availabilityEmployees}
            showAvailability
            isSubmitting={createMutation.isPending}
            submitLabel="Create Task"
          />
        </DialogContent>
      </Dialog>

      {/* ── Snackbar ────────────────────────────────────────────────────────── */}
      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={closeSnackbar}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snackbar.severity} onClose={closeSnackbar} variant="filled">
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
