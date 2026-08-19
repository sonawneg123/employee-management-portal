/**
 * @fileoverview EmployeeDashboardPage — self-service dashboard for ROLE_EMPLOYEE users.
 *
 * Shows the employee's own data only:
 * - Greeting welcome card
 * - Profile summary: name, department, job title, joining date
 * - Quick actions: Apply for Leave, View My Attendance
 * - Upcoming / pending leave requests
 * - Attendance summary widget
 *
 * Profile data is fetched from GET /profile which automatically scopes
 * to the authenticated user's linked employee record.
 */

import React from 'react';
import { Helmet } from 'react-helmet-async';
import { useNavigate } from 'react-router-dom';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Grid,
  Skeleton,
  Typography,
} from '@mui/material';
import EventNoteIcon from '@mui/icons-material/EventNote';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import ApartmentRoundedIcon from '@mui/icons-material/ApartmentRounded';
import WorkRoundedIcon from '@mui/icons-material/WorkRounded';
import CalendarTodayRoundedIcon from '@mui/icons-material/CalendarTodayRounded';
import BadgeRoundedIcon from '@mui/icons-material/BadgeRounded';
import TaskRoundedIcon from '@mui/icons-material/TaskRounded';
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded';

import { getProfile } from '@/services/profileApi';
import { ROUTES } from '@/constants/routes';
import { useAuth } from '@/contexts/AuthContext';

import WelcomeCard from '@/components/dashboard/WelcomeCard';
import UpcomingLeavesWidget from '@/components/dashboard/UpcomingLeavesWidget';
import AttendanceSummaryWidget from '@/components/dashboard/AttendanceSummaryWidget';

// ── Profile Summary Card ──────────────────────────────────────────────────────

/**
 * Displays the employee's key profile details in a compact card.
 *
 * @param {{ profile: import('@/services/profileApi').ProfileResponse, isLoading: boolean }} props
 * @returns {JSX.Element}
 */
function ProfileSummaryCard({ profile, isLoading }) {
  const initials = profile
    ? `${profile.firstName?.[0] ?? ''}${profile.lastName?.[0] ?? ''}`.toUpperCase()
    : '?';

  const statusColor = profile?.status === 'ACTIVE' ? 'success' : 'default';

  return (
    <Card sx={{ height: '100%' }}>
      <CardContent sx={{ p: 3 }}>
        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 3 }}>
          {isLoading ? (
            <Skeleton variant="circular" width={60} height={60} />
          ) : (
            <Avatar
              src={profile?.profilePhotoUrl}
              sx={{
                width: 60,
                height: 60,
                background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                fontSize: '1.25rem',
                fontWeight: 700,
                flexShrink: 0,
              }}
            >
              {!profile?.profilePhotoUrl && initials}
            </Avatar>
          )}
          <Box sx={{ minWidth: 0 }}>
            {isLoading ? (
              <>
                <Skeleton variant="text" width={140} height={26} />
                <Skeleton variant="text" width={100} />
              </>
            ) : (
              <>
                <Typography variant="h6" fontWeight={700} noWrap>
                  {profile?.firstName} {profile?.lastName}
                </Typography>
                <Chip
                  label={profile?.status ?? 'ACTIVE'}
                  color={statusColor}
                  size="small"
                  sx={{ fontWeight: 600, mt: 0.25, height: 22 }}
                />
              </>
            )}
          </Box>
        </Box>

        {/* Profile fields */}
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          {[
            {
              Icon: WorkRoundedIcon,
              label: 'Job Title',
              value: profile?.jobTitle,
              color: '#4F46E5',
            },
            {
              Icon: ApartmentRoundedIcon,
              label: 'Department',
              value: profile?.departmentName,
              color: '#7C3AED',
            },
            {
              Icon: BadgeRoundedIcon,
              label: 'Employee ID',
              value: profile?.employeeCode,
              color: '#10B981',
            },
            {
              Icon: CalendarTodayRoundedIcon,
              label: 'Joined',
              value: profile?.dateOfJoining,
              color: '#F59E0B',
            },
          ].map(({ Icon, label, value, color }) => (
            <Box key={label} sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <Box
                sx={{
                  width: 34,
                  height: 34,
                  borderRadius: '10px',
                  bgcolor: `${color}14`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  flexShrink: 0,
                }}
                aria-hidden="true"
              >
                <Icon sx={{ fontSize: 17, color }} />
              </Box>
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="caption" color="text.secondary" display="block">
                  {label}
                </Typography>
                {isLoading ? (
                  <Skeleton variant="text" width={120} />
                ) : (
                  <Typography variant="body2" fontWeight={600} noWrap>
                    {value ?? '—'}
                  </Typography>
                )}
              </Box>
            </Box>
          ))}
        </Box>
      </CardContent>
    </Card>
  );
}

// ── Quick Actions Card ────────────────────────────────────────────────────────

/**
 * Compact set of employee quick-action buttons.
 *
 * @param {{ onApplyLeave: () => void, onViewAttendance: () => void, onViewTasks: () => void, onViewReviews: () => void }} props
 * @returns {JSX.Element}
 */
function EmployeeQuickActions({ onApplyLeave, onViewAttendance, onViewTasks, onViewReviews }) {
  const actions = [
    {
      label: 'Apply for Leave',
      icon: <EventNoteIcon sx={{ fontSize: 18 }} />,
      onClick: onApplyLeave,
      variant: 'contained',
      sx: {
        background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
        color: '#fff',
        '&:hover': { background: 'linear-gradient(135deg, #4338CA, #6D28D9)' },
      },
    },
    {
      label: 'My Attendance',
      icon: <AccessTimeIcon sx={{ fontSize: 18 }} />,
      onClick: onViewAttendance,
      variant: 'outlined',
      sx: {},
    },
    {
      label: 'My Tasks',
      icon: <TaskRoundedIcon sx={{ fontSize: 18 }} />,
      onClick: onViewTasks,
      variant: 'outlined',
      sx: {},
    },
    {
      label: 'My Reviews',
      icon: <AssessmentRoundedIcon sx={{ fontSize: 18 }} />,
      onClick: onViewReviews,
      variant: 'outlined',
      sx: {},
    },
  ];

  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Typography variant="h6" fontWeight={700} sx={{ mb: 2, letterSpacing: '-0.01em' }}>
          Quick Actions
        </Typography>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
          {actions.map(({ label, icon, onClick, variant, sx }) => (
            <Button
              key={label}
              variant={variant}
              startIcon={icon}
              onClick={onClick}
              fullWidth
              size="small"
              sx={{
                justifyContent: 'flex-start',
                py: 1,
                borderRadius: '10px',
                ...sx,
              }}
            >
              {label}
            </Button>
          ))}
        </Box>
      </CardContent>
    </Card>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

/**
 * Employee self-service dashboard page.
 *
 * @returns {JSX.Element}
 */
export default function EmployeeDashboardPage() {
  const navigate = useNavigate();
  const { user } = useAuth();

  const {
    data: profile,
    isLoading: profileLoading,
    isError: profileError,
  } = useQuery({
    queryKey: ['profile', user?.userId],
    queryFn: getProfile,
    enabled: Boolean(user?.userId),
    staleTime: 5 * 60_000,
  });

  return (
    <>
      <Helmet>
        <title>Dashboard — PeopleCore HR</title>
      </Helmet>

      <Box sx={{ pb: 4 }}>
        {/* Welcome banner */}
        <WelcomeCard />

        {profileError && (
          <Alert severity="warning" sx={{ mb: 3, borderRadius: '12px' }}>
            Could not load your profile information.
          </Alert>
        )}

        <Grid container spacing={3}>
          {/* Left column — profile + quick actions */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              <ProfileSummaryCard profile={profile} isLoading={profileLoading} />
              <EmployeeQuickActions
                onApplyLeave={() => navigate(ROUTES.EMPLOYEE_LEAVES)}
                onViewAttendance={() => navigate(ROUTES.EMPLOYEE_ATTENDANCE)}
                onViewTasks={() => navigate(ROUTES.EMPLOYEE_TASKS)}
                onViewReviews={() => navigate(ROUTES.EMPLOYEE_REVIEWS)}
              />
            </Box>
          </Grid>

          {/* Right column — leaves + attendance */}
          <Grid size={{ xs: 12, md: 8 }}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
              <UpcomingLeavesWidget />
              <AttendanceSummaryWidget />
            </Box>
          </Grid>
        </Grid>
      </Box>
    </>
  );
}
