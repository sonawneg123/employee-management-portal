/**
 * @fileoverview EmployeeDashboardPage — self-service dashboard for ROLE_EMPLOYEE users.
 *
 * Shows the employee's own data only:
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
import PersonIcon       from '@mui/icons-material/Person';
import EventNoteIcon    from '@mui/icons-material/EventNote';
import AccessTimeIcon   from '@mui/icons-material/AccessTime';
import ApartmentIcon    from '@mui/icons-material/Apartment';
import WorkIcon         from '@mui/icons-material/Work';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';

import { getProfile } from '@/services/profileApi';
import { ROUTES } from '@/constants/routes';
import { useAuth } from '@/contexts/AuthContext';

import WelcomeCard              from '@/components/dashboard/WelcomeCard';
import UpcomingLeavesWidget     from '@/components/dashboard/UpcomingLeavesWidget';
import AttendanceSummaryWidget  from '@/components/dashboard/AttendanceSummaryWidget';

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
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2, mb: 2.5 }}>
          {isLoading ? (
            <Skeleton variant="circular" width={56} height={56} />
          ) : (
            <Avatar sx={{ width: 56, height: 56, bgcolor: 'primary.main', fontSize: '1.25rem' }}>
              {initials}
            </Avatar>
          )}
          <Box sx={{ minWidth: 0 }}>
            {isLoading ? (
              <>
                <Skeleton variant="text" width={140} height={28} />
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
                  sx={{ fontWeight: 600, mt: 0.25 }}
                />
              </>
            )}
          </Box>
        </Box>

        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.25 }}>
          {[
            { Icon: WorkIcon,         label: 'Job Title',    value: profile?.jobTitle },
            { Icon: ApartmentIcon,    label: 'Department',   value: profile?.departmentName },
            { Icon: PersonIcon,       label: 'Employee ID',  value: profile?.employeeCode },
            { Icon: CalendarTodayIcon, label: 'Joined',      value: profile?.joiningDate },
          ].map(({ Icon, label, value }) => (
            <Box key={label} sx={{ display: 'flex', alignItems: 'center', gap: 1.25 }}>
              <Icon sx={{ fontSize: 18, color: 'text.secondary', flexShrink: 0 }} />
              <Box>
                <Typography variant="caption" color="text.secondary" display="block">
                  {label}
                </Typography>
                {isLoading ? (
                  <Skeleton variant="text" width={120} />
                ) : (
                  <Typography variant="body2" fontWeight={500}>
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
 * @param {{ onApplyLeave: () => void, onViewAttendance: () => void }} props
 * @returns {JSX.Element}
 */
function EmployeeQuickActions({ onApplyLeave, onViewAttendance }) {
  return (
    <Card>
      <CardContent sx={{ p: 3 }}>
        <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
          Quick Actions
        </Typography>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
          <Button
            variant="contained"
            startIcon={<EventNoteIcon />}
            onClick={onApplyLeave}
            fullWidth
            size="small"
          >
            Apply for Leave
          </Button>
          <Button
            variant="outlined"
            startIcon={<AccessTimeIcon />}
            onClick={onViewAttendance}
            fullWidth
            size="small"
          >
            View My Attendance
          </Button>
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
    data:      profile,
    isLoading: profileLoading,
    isError:   profileError,
  } = useQuery({
    queryKey: ['profile', user?.userId],
    queryFn:  getProfile,
    enabled:  Boolean(user?.userId),
    staleTime: 5 * 60_000,
  });

  return (
    <>
      <Helmet><title>My Dashboard — Employee Portal</title></Helmet>

      <Box sx={{ pb: 4 }}>
        {/* Welcome banner */}
        <WelcomeCard />

        {profileError && (
          <Alert severity="warning" sx={{ mb: 2 }}>
            Could not load your profile information.
          </Alert>
        )}

        <Grid container spacing={3}>
          {/* Left column — profile + quick actions */}
          <Grid size={{ xs: 12, md: 4 }}>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <ProfileSummaryCard profile={profile} isLoading={profileLoading} />
              <EmployeeQuickActions
                onApplyLeave={() => navigate(ROUTES.EMPLOYEE_LEAVES)}
                onViewAttendance={() => navigate(ROUTES.EMPLOYEE_ATTENDANCE)}
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
