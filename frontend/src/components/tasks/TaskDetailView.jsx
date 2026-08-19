/**
 * @fileoverview TaskDetailView — reusable task detail display component.
 *
 * Designed to be extended in later phases with submissions, comments,
 * AI review, attachments, approval, and audit history sections.
 */

import React from 'react';
import {
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  Stack,
  Tooltip,
  Typography,
} from '@mui/material';
import CalendarTodayRoundedIcon from '@mui/icons-material/CalendarTodayRounded';
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import TaskStatusChip from './TaskStatusChip';
import TaskPriorityChip from './TaskPriorityChip';
import EmployeeAvatar from '@/components/employees/EmployeeAvatar';

/**
 * @param {{
 *   task: import('../../services/taskApi').TaskResponse,
 *   onStartTask?: () => void,
 *   isUpdatingStatus?: boolean,
 *   showStartButton?: boolean,
 *   startDisabled?: boolean,
 *   startDisabledReason?: string,
 * }} props
 * @returns {JSX.Element}
 */
export default function TaskDetailView({
  task,
  onStartTask,
  isUpdatingStatus = false,
  showStartButton = false,
  startDisabled = false,
  startDisabledReason = '',
}) {
  const formattedDueDate = task.dueDate
    ? new Date(task.dueDate).toLocaleDateString(undefined, {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      })
    : '—';

  const formattedCreated = task.createdAt ? new Date(task.createdAt).toLocaleString() : '—';

  return (
    <Box>
      {/* Header */}
      <Box sx={{ mb: 3 }}>
        <Stack direction="row" spacing={1} alignItems="center" flexWrap="wrap" sx={{ mb: 1 }}>
          <TaskStatusChip status={task.status} overdue={task.overdue} size="medium" />
          <TaskPriorityChip priority={task.priority} size="medium" />
          {task.category && (
            <Chip label={task.category} size="medium" variant="outlined" color="default" />
          )}
        </Stack>
        <Typography variant="h5" fontWeight={700} sx={{ mt: 1 }}>
          {task.title}
        </Typography>
      </Box>

      <Grid container spacing={3}>
        {/* Left column — main content */}
        <Grid size={{ xs: 12, md: 8 }}>
          {task.description && (
            <Card variant="outlined" sx={{ mb: 2 }}>
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Description
                </Typography>
                <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                  {task.description}
                </Typography>
              </CardContent>
            </Card>
          )}

          {task.guidelines && (
            <Card variant="outlined" sx={{ mb: 2 }}>
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Guidelines
                </Typography>
                <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                  {task.guidelines}
                </Typography>
              </CardContent>
            </Card>
          )}

          {task.acceptanceCriteria && (
            <Card variant="outlined" sx={{ mb: 2 }}>
              <CardContent>
                <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                  Acceptance Criteria
                </Typography>
                <Typography variant="body1" sx={{ whiteSpace: 'pre-wrap' }}>
                  {task.acceptanceCriteria}
                </Typography>
              </CardContent>
            </Card>
          )}

          {/* Future phases: submissions, comments, attachments, AI review, audit */}
        </Grid>

        {/* Right column — metadata */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card variant="outlined">
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                Task Details
              </Typography>
              <Divider sx={{ my: 1 }} />

              <Stack spacing={2}>
                {/* Due date */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <CalendarTodayRoundedIcon fontSize="small" color="action" />
                  <Box>
                    <Typography variant="caption" color="text.secondary" display="block">
                      Due Date
                    </Typography>
                    <Typography
                      variant="body2"
                      fontWeight={600}
                      color={task.overdue ? 'error.main' : 'text.primary'}
                    >
                      {formattedDueDate}
                      {task.overdue && ' (Overdue)'}
                    </Typography>
                  </Box>
                </Box>

                {/* Assigned to */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  {task.assignedEmployeeId ? (
                    <EmployeeAvatar
                      firstName={task.assignedEmployeeName?.split(' ')[0]}
                      lastName={task.assignedEmployeeName?.split(' ').slice(1).join(' ')}
                      profilePhotoUrl={`/api/employees/${task.assignedEmployeeId}/profile-photo`}
                      size={28}
                    />
                  ) : (
                    <PersonRoundedIcon fontSize="small" color="action" />
                  )}
                  <Box>
                    <Typography variant="caption" color="text.secondary" display="block">
                      Assigned To
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {task.assignedEmployeeName ?? 'Unassigned'}
                    </Typography>
                    {task.assignedEmployeeCode && (
                      <Typography variant="caption" color="text.secondary">
                        {task.assignedEmployeeCode}
                      </Typography>
                    )}
                  </Box>
                </Box>

                {/* Created by */}
                <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                  <PersonRoundedIcon fontSize="small" color="action" />
                  <Box>
                    <Typography variant="caption" color="text.secondary" display="block">
                      Created By
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {task.createdByEmployeeName ?? task.createdBy ?? '—'}
                    </Typography>
                  </Box>
                </Box>

                {/* Estimated hours */}
                {task.estimatedHours != null && (
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <AccessTimeRoundedIcon fontSize="small" color="action" />
                    <Box>
                      <Typography variant="caption" color="text.secondary" display="block">
                        Estimated Hours
                      </Typography>
                      <Typography variant="body2" fontWeight={600}>
                        {task.estimatedHours}h
                      </Typography>
                    </Box>
                  </Box>
                )}

                <Divider />

                <Box>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Created
                  </Typography>
                  <Typography variant="body2">{formattedCreated}</Typography>
                </Box>
              </Stack>
            </CardContent>
          </Card>

          {/* Employee action: start task */}
          {showStartButton && task.status === 'ASSIGNED' && (
            <Tooltip
              title={startDisabled ? startDisabledReason : ''}
              disableHoverListener={!startDisabled}
            >
              <span>
                <Button
                  variant="contained"
                  fullWidth
                  sx={{ mt: 2 }}
                  onClick={startDisabled ? undefined : onStartTask}
                  disabled={isUpdatingStatus || startDisabled}
                >
                  Start Task
                </Button>
              </span>
            </Tooltip>
          )}
        </Grid>
      </Grid>
    </Box>
  );
}
