/**
 * @fileoverview EmployeeTasksPage — self-service task list for employees.
 *
 * Phase 6A: initial list with status/priority filtering.
 * Phase 6C-6E: URGENT priority, category filter, category column in table.
 */

import React, { useCallback, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import {
  Alert,
  Box,
  Card,
  Chip,
  CircularProgress,
  FormControl,
  Grid,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
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
import VisibilityRoundedIcon from '@mui/icons-material/VisibilityRounded';
import AssignmentRoundedIcon from '@mui/icons-material/AssignmentRounded';
import CheckCircleOutlineRoundedIcon from '@mui/icons-material/CheckCircleOutlineRounded';
import HourglassEmptyRoundedIcon from '@mui/icons-material/HourglassEmptyRounded';
import ErrorOutlineRoundedIcon from '@mui/icons-material/ErrorOutlineRounded';

import { ROUTES } from '@/constants/routes';
import { useMyTasks } from '@/hooks/useTaskHooks';
import { useTodayAttendance } from '@/hooks/useAttendanceStatus';
import TaskStatusChip from '@/components/tasks/TaskStatusChip';
import TaskPriorityChip from '@/components/tasks/TaskPriorityChip';
import PageHeader from '@/components/common/PageHeader';
import { categoryLabel, TASK_CATEGORIES } from '@/components/tasks/TaskForm';

const TASK_STATUSES = [
  'DRAFT',
  'ASSIGNED',
  'IN_PROGRESS',
  'SUBMITTED',
  'COMPLETED',
  'CHANGES_REQUESTED',
  'REJECTED',
];
const TASK_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];

function StatCard({ icon, label, value, color }) {
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
      <Box>
        <Typography variant="h5" fontWeight={700}>
          {value ?? '—'}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {label}
        </Typography>
      </Box>
    </Card>
  );
}

/**
 * Employee self-service task list page.
 *
 * @returns {JSX.Element}
 */
export default function EmployeeTasksPage() {
  const navigate = useNavigate();
  const theme = useTheme();

  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [statusFilter, setStatusFilter] = useState('');
  const [priorityFilter, setPriorityFilter] = useState('');
  const [categoryFilter, setCategoryFilter] = useState('');

  const { isCheckedOut } = useTodayAttendance();

  const tasksQuery = useMyTasks({
    page,
    size: pageSize,
    sort: 'dueDate',
    direction: 'asc',
    ...(statusFilter && { status: statusFilter }),
    ...(priorityFilter && { priority: priorityFilter }),
    ...(categoryFilter && { category: categoryFilter }),
  });

  // Separate unfiltered query for stat cards
  const allTasksQuery = useMyTasks({ size: 200 });
  const allTasks = allTasksQuery.data?.content ?? [];
  const stats = {
    total: allTasks.length,
    inProgress: allTasks.filter((t) => t.status === 'IN_PROGRESS').length,
    completed: allTasks.filter((t) => t.status === 'COMPLETED').length,
    overdue: allTasks.filter((t) => t.overdue).length,
  };

  const tasks = tasksQuery.data?.content ?? [];
  const totalElements = tasksQuery.data?.totalElements ?? 0;

  return (
    <>
      <Helmet>
        <title>My Tasks | Employee Portal</title>
      </Helmet>

      <Box sx={{ p: { xs: 2, md: 3 } }}>
        <PageHeader title="My Tasks" subtitle="Tasks assigned to you by your manager" />

        {/* Checkout restriction banner */}
        {isCheckedOut && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            <Typography variant="body2" fontWeight={600}>
              You have checked out for today.
            </Typography>
            <Typography variant="body2">
              Task actions are unavailable until your next working session.
            </Typography>
          </Alert>
        )}

        {/* ── Stat cards ──────────────────────────────────────────────────── */}
        <Grid container spacing={2} sx={{ mb: 3 }}>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard
              icon={<AssignmentRoundedIcon />}
              label="Total Tasks"
              value={stats.total}
              color={theme.palette.primary.main}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard
              icon={<HourglassEmptyRoundedIcon />}
              label="In Progress"
              value={stats.inProgress}
              color={theme.palette.info.main}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard
              icon={<CheckCircleOutlineRoundedIcon />}
              label="Completed"
              value={stats.completed}
              color={theme.palette.success.main}
            />
          </Grid>
          <Grid size={{ xs: 6, sm: 3 }}>
            <StatCard
              icon={<ErrorOutlineRoundedIcon />}
              label="Overdue"
              value={stats.overdue}
              color={stats.overdue > 0 ? theme.palette.error.main : theme.palette.text.secondary}
            />
          </Grid>
        </Grid>

        {/* ── Filters ─────────────────────────────────────────────────────── */}
        <Card variant="outlined" sx={{ mb: 2 }}>
          <Box sx={{ p: 2 }}>
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={2}
              alignItems="flex-start"
              flexWrap="wrap"
            >
              <FormControl size="small" sx={{ minWidth: 160 }}>
                <InputLabel>Status</InputLabel>
                <Select
                  value={statusFilter}
                  onChange={(e) => {
                    setStatusFilter(e.target.value);
                    setPage(0);
                  }}
                  label="Status"
                >
                  <MenuItem value="">All</MenuItem>
                  {TASK_STATUSES.map((s) => (
                    <MenuItem key={s} value={s}>
                      {s.replace(/_/g, ' ')}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 140 }}>
                <InputLabel>Priority</InputLabel>
                <Select
                  value={priorityFilter}
                  onChange={(e) => {
                    setPriorityFilter(e.target.value);
                    setPage(0);
                  }}
                  label="Priority"
                >
                  <MenuItem value="">All</MenuItem>
                  {TASK_PRIORITIES.map((p) => (
                    <MenuItem key={p} value={p}>
                      {p}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 150 }}>
                <InputLabel>Category</InputLabel>
                <Select
                  value={categoryFilter}
                  onChange={(e) => {
                    setCategoryFilter(e.target.value);
                    setPage(0);
                  }}
                  label="Category"
                >
                  <MenuItem value="">All Categories</MenuItem>
                  {TASK_CATEGORIES.map((c) => (
                    <MenuItem key={c} value={c}>
                      {categoryLabel(c)}
                    </MenuItem>
                  ))}
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
                    <TableCell>Category</TableCell>
                    <TableCell>Priority</TableCell>
                    <TableCell>Status</TableCell>
                    <TableCell>Due Date</TableCell>
                    <TableCell>Manager</TableCell>
                    <TableCell align="center">Actions</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {tasks.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} align="center" sx={{ py: 4, color: 'text.secondary' }}>
                        No tasks assigned to you yet.
                      </TableCell>
                    </TableRow>
                  ) : (
                    tasks.map((task) => (
                      <TableRow key={task.id} hover>
                        <TableCell>
                          <Typography
                            variant="body2"
                            fontWeight={600}
                            noWrap
                            sx={{ maxWidth: 200 }}
                          >
                            {task.title}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          {task.category ? (
                            <Chip
                              label={categoryLabel(task.category)}
                              size="small"
                              variant="outlined"
                              sx={{ height: 20, fontSize: '0.7rem' }}
                            />
                          ) : (
                            <Typography variant="caption" color="text.secondary">
                              —
                            </Typography>
                          )}
                        </TableCell>
                        <TableCell>
                          <TaskPriorityChip priority={task.priority} />
                        </TableCell>
                        <TableCell>
                          <TaskStatusChip status={task.status} overdue={task.overdue} />
                        </TableCell>
                        <TableCell>
                          <Typography
                            variant="body2"
                            color={task.overdue ? 'error.main' : 'text.primary'}
                          >
                            {task.dueDate ? new Date(task.dueDate).toLocaleDateString() : '—'}
                          </Typography>
                        </TableCell>
                        <TableCell>
                          <Typography variant="body2">
                            {task.createdByEmployeeName ?? task.createdBy ?? '—'}
                          </Typography>
                        </TableCell>
                        <TableCell align="center">
                          <Tooltip title="View Details">
                            <IconButton
                              size="small"
                              onClick={() => navigate(ROUTES.EMPLOYEE_TASK_DETAIL(task.id))}
                            >
                              <VisibilityRoundedIcon fontSize="small" />
                            </IconButton>
                          </Tooltip>
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
              onRowsPerPageChange={(e) => {
                setPageSize(parseInt(e.target.value));
                setPage(0);
              }}
              rowsPerPageOptions={[10, 20, 50]}
            />
          </Paper>
        )}
      </Box>
    </>
  );
}
