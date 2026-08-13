/**
 * @fileoverview EmployeeDetails — full read-only detail view of a single employee.
 *
 * Displayed in both the EmployeeDetailsPage (full-page) and potentially in
 * a side-panel or preview modal. Renders all employee fields in grouped sections.
 */

import React from 'react';
import { Box, Card, CardContent, Chip, Divider, Grid, Skeleton, Typography } from '@mui/material';
import EmailIcon from '@mui/icons-material/Email';
import PhoneIcon from '@mui/icons-material/Phone';
import LocationOnIcon from '@mui/icons-material/LocationOn';
import WorkIcon from '@mui/icons-material/Work';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import AttachMoneyIcon from '@mui/icons-material/AttachMoney';
import BadgeIcon from '@mui/icons-material/Badge';
import EmployeeAvatar from './EmployeeAvatar';
import EmployeeStatusChip from './EmployeeStatusChip';
import EmployeeDepartmentChip from './EmployeeDepartmentChip';
import {
  formatFullName,
  formatSalary,
  formatJoinDate,
  formatPhone,
  formatYearsOfService,
} from '@/utils/employeeFormatters';

/**
 * @typedef {Object} DetailRowProps
 * @property {React.ReactNode} icon
 * @property {string}          label
 * @property {React.ReactNode} value
 */

/**
 * A single labelled detail row with an icon.
 *
 * @param {DetailRowProps} props
 * @returns {JSX.Element}
 */
function DetailRow({ icon, label, value }) {
  return (
    <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', py: 1 }}>
      <Box sx={{ color: 'text.secondary', mt: 0.3, flexShrink: 0 }}>{icon}</Box>
      <Box>
        <Typography variant="caption" color="text.secondary" display="block">
          {label}
        </Typography>
        <Typography variant="body2" fontWeight={500}>
          {value}
        </Typography>
      </Box>
    </Box>
  );
}

/**
 * @typedef {Object} EmployeeDetailsProps
 * @property {import('@/services/employeeApi').EmployeeResponse | undefined} employee
 * @property {boolean} isLoading
 */

/**
 * Full read-only employee detail view.
 *
 * @param {EmployeeDetailsProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeDetails({ employee, isLoading }) {
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
            <Skeleton variant="circular" width={80} height={80} />
            <Box sx={{ flex: 1 }}>
              <Skeleton variant="text" width="50%" height={32} />
              <Skeleton variant="text" width="35%" />
              <Skeleton
                variant="rectangular"
                width={80}
                height={24}
                sx={{ mt: 1, borderRadius: 4 }}
              />
            </Box>
          </Box>
          {[0, 1, 2, 3, 4].map((i) => (
            <Box key={i} sx={{ display: 'flex', gap: 2, mb: 2 }}>
              <Skeleton variant="circular" width={24} height={24} />
              <Box sx={{ flex: 1 }}>
                <Skeleton variant="text" width="30%" />
                <Skeleton variant="text" width="60%" />
              </Box>
            </Box>
          ))}
        </CardContent>
      </Card>
    );
  }

  if (!employee) return null;

  const fullName = formatFullName(employee.firstName, employee.lastName);

  return (
    <Card>
      <CardContent>
        {/* Header — avatar + name + chips */}
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', mb: 2.5 }}>
          <EmployeeAvatar
            firstName={employee.firstName}
            lastName={employee.lastName}
            profilePhotoUrl={employee.profilePhotoUrl}
            size={80}
          />
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="h5" fontWeight={700} gutterBottom noWrap>
              {fullName}
            </Typography>
            <Typography variant="body1" color="text.secondary" gutterBottom>
              {employee.jobTitle ?? '—'}
            </Typography>
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 0.5 }}>
              <EmployeeStatusChip status={employee.status} size="small" />
              <EmployeeDepartmentChip departmentName={employee.departmentName} size="small" />
              <Chip
                icon={<BadgeIcon />}
                label={employee.employeeCode}
                size="small"
                variant="outlined"
                aria-label={`Employee code: ${employee.employeeCode}`}
              />
            </Box>
          </Box>
        </Box>

        <Divider sx={{ mb: 2 }} />

        {/* Detail grid */}
        <Grid container spacing={0}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailRow
              icon={<EmailIcon fontSize="small" />}
              label="Email"
              value={employee.email ?? '—'}
            />
            <DetailRow
              icon={<PhoneIcon fontSize="small" />}
              label="Phone"
              value={formatPhone(employee.phone)}
            />
            <DetailRow
              icon={<LocationOnIcon fontSize="small" />}
              label="Address"
              value={employee.address ?? '—'}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailRow
              icon={<WorkIcon fontSize="small" />}
              label="Department"
              value={employee.departmentName ?? '—'}
            />
            <DetailRow
              icon={<CalendarTodayIcon fontSize="small" />}
              label="Hire Date"
              value={`${formatJoinDate(employee.dateOfJoining)} · ${formatYearsOfService(employee.dateOfJoining)}`}
            />
            <DetailRow
              icon={<AttachMoneyIcon fontSize="small" />}
              label="Salary"
              value={formatSalary(employee.salary)}
            />
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}
