/**
 * @fileoverview RoleDashboard — role-based dashboard layout switcher.
 *
 * Renders a different combination of dashboard widgets depending on the
 * authenticated user's highest-priority role:
 *
 * | Role          | Layout                                                    |
 * |---------------|-----------------------------------------------------------|
 * | ADMIN         | Full view — all KPIs, charts, activity, attendance        |
 * | HR            | KPIs, department chart, leaves widget, activity feed      |
 * | MANAGER       | KPIs, status chart, attendance, activity (team focus)     |
 * | EMPLOYEE      | Welcome, quick actions, upcoming leaves, attendance rate  |
 *
 * Falls back to the EMPLOYEE layout for any unrecognised role.
 */

import React from 'react';
import { Grid } from '@mui/material';
import { ROLES } from '@/constants/roles';
import { useAuth } from '@/contexts/AuthContext';

import WelcomeCard                  from './WelcomeCard';
import StatisticsCards              from './StatisticsCards';
import QuickActions                 from './QuickActions';
import RecentActivity               from './RecentActivity';
import UpcomingLeavesWidget         from './UpcomingLeavesWidget';
import AttendanceSummaryWidget      from './AttendanceSummaryWidget';
import DepartmentDistributionChart  from './DepartmentDistributionChart';
import EmployeeStatusChart          from './EmployeeStatusChart';

// ── Role-priority helper ──────────────────────────────────────────────────────

/**
 * Returns the highest-priority role for the given user.
 * Priority order: ADMIN > HR > MANAGER > EMPLOYEE.
 *
 * @param {string[]} roles - Array of role strings from the JWT.
 * @returns {string} The effective role string.
 */
function resolveRole(roles = []) {
  if (roles.includes(ROLES.ADMIN))    return ROLES.ADMIN;
  if (roles.includes(ROLES.HR))       return ROLES.HR;
  if (roles.includes(ROLES.MANAGER))  return ROLES.MANAGER;
  return ROLES.EMPLOYEE;
}

// ── Layout variants ───────────────────────────────────────────────────────────

/**
 * Admin layout — maximum visibility: all KPIs, both charts, activity, widgets.
 *
 * @returns {JSX.Element}
 */
function AdminDashboard() {
  return (
    <>
      {/* Row 1 — welcome banner */}
      <Grid container spacing={3} sx={{ mb: 1 }}>
        <Grid size={{ xs: 12 }}>
          <WelcomeCard />
        </Grid>
      </Grid>

      {/* Row 2 — KPI stat cards */}
      <StatisticsCards />

      {/* Row 3 — charts */}
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <DepartmentDistributionChart />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <EmployeeStatusChart />
        </Grid>
      </Grid>

      {/* Row 4 — activity + sidebar widgets */}
      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <RecentActivity />
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <Grid container spacing={3} direction="column">
            <Grid size={{ xs: 12 }}>
              <UpcomingLeavesWidget />
            </Grid>
            <Grid size={{ xs: 12 }}>
              <AttendanceSummaryWidget />
            </Grid>
          </Grid>
        </Grid>
      </Grid>
    </>
  );
}

/**
 * HR layout — people-focused: KPIs, department chart, leaves, activity.
 *
 * @returns {JSX.Element}
 */
function HrDashboard() {
  return (
    <>
      <Grid container spacing={3} sx={{ mb: 1 }}>
        <Grid size={{ xs: 12 }}>
          <WelcomeCard />
        </Grid>
      </Grid>

      <StatisticsCards />

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 7 }}>
          <DepartmentDistributionChart />
        </Grid>
        <Grid size={{ xs: 12, md: 5 }}>
          <UpcomingLeavesWidget />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <RecentActivity />
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <AttendanceSummaryWidget />
        </Grid>
      </Grid>
    </>
  );
}

/**
 * Manager layout — team-focused: KPIs, status chart, attendance, activity.
 *
 * @returns {JSX.Element}
 */
function ManagerDashboard() {
  return (
    <>
      <Grid container spacing={3} sx={{ mb: 1 }}>
        <Grid size={{ xs: 12 }}>
          <WelcomeCard />
        </Grid>
      </Grid>

      <StatisticsCards />

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 7 }}>
          <EmployeeStatusChart />
        </Grid>
        <Grid size={{ xs: 12, md: 5 }}>
          <AttendanceSummaryWidget />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 8 }}>
          <RecentActivity />
        </Grid>
        <Grid size={{ xs: 12, lg: 4 }}>
          <UpcomingLeavesWidget />
        </Grid>
      </Grid>
    </>
  );
}

/**
 * Employee layout — self-service: greeting, quick actions, own leave/attendance.
 *
 * @returns {JSX.Element}
 */
function EmployeeDashboard() {
  return (
    <>
      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12 }}>
          <WelcomeCard />
        </Grid>
      </Grid>

      <Grid container spacing={3} sx={{ mb: 3 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <QuickActions />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <AttendanceSummaryWidget />
        </Grid>
      </Grid>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <UpcomingLeavesWidget />
        </Grid>
      </Grid>
    </>
  );
}

// ── Role map ──────────────────────────────────────────────────────────────────

/** @type {Record<string, React.ComponentType>} */
const ROLE_LAYOUT_MAP = {
  [ROLES.ADMIN]:    AdminDashboard,
  [ROLES.HR]:       HrDashboard,
  [ROLES.MANAGER]:  ManagerDashboard,
  [ROLES.EMPLOYEE]: EmployeeDashboard,
};

// ── Exported component ────────────────────────────────────────────────────────

/**
 * Selects and renders the appropriate dashboard layout for the current user.
 * Falls back to {@link EmployeeDashboard} for any unrecognised role.
 *
 * @returns {JSX.Element}
 */
export default function RoleDashboard() {
  const { user } = useAuth();
  const role     = resolveRole(user?.roles);
  const Layout   = ROLE_LAYOUT_MAP[role] ?? EmployeeDashboard;

  return <Layout />;
}
