/**
 * @fileoverview DepartmentStatisticsCard — KPI summary tile for a department.
 *
 * Displays the employee count, department head, and creation date in a
 * compact Card. Used at the top of {@link DepartmentDetailsPage}.
 */

import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Divider,
  Skeleton,
  Typography,
} from '@mui/material';
import PeopleIcon        from '@mui/icons-material/People';
import PersonIcon        from '@mui/icons-material/Person';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import { formatEmployeeCount, formatHeadName, formatDeptCreatedAt } from '@/utils/departmentFormatters';

/**
 * @typedef {Object} StatRowProps
 * @property {React.ReactNode} icon
 * @property {string}          label
 * @property {string}          value
 */

/**
 * A single labelled row with an icon.
 *
 * @param {StatRowProps} props
 * @returns {JSX.Element}
 */
function StatRow({ icon, label, value }) {
  return (
    <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'center', py: 1 }}>
      <Box sx={{ color: 'primary.main', flexShrink: 0 }}>{icon}</Box>
      <Box sx={{ flex: 1 }}>
        <Typography variant="caption" color="text.secondary" display="block">
          {label}
        </Typography>
        <Typography variant="body2" fontWeight={600}>
          {value}
        </Typography>
      </Box>
    </Box>
  );
}

/**
 * @typedef {Object} DepartmentStatisticsCardProps
 * @property {import('@/services/departmentApi').DepartmentResponse | undefined} department
 * @property {boolean} isLoading
 */

/**
 * KPI statistics card for the department details page.
 *
 * @param {DepartmentStatisticsCardProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentStatisticsCard({ department, isLoading }) {
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          {[0, 1, 2].map((i) => (
            <Box key={i} sx={{ display: 'flex', gap: 1.5, py: 1 }}>
              <Skeleton variant="circular" width={24} height={24} />
              <Box sx={{ flex: 1 }}>
                <Skeleton variant="text" width="40%" />
                <Skeleton variant="text" width="60%" />
              </Box>
            </Box>
          ))}
        </CardContent>
      </Card>
    );
  }

  if (!department) return null;

  return (
    <Card>
      <CardContent sx={{ pb: '16px !important' }}>
        <Typography variant="subtitle2" fontWeight={700} gutterBottom>
          Statistics
        </Typography>
        <Divider sx={{ mb: 1 }} />
        <StatRow
          icon={<PeopleIcon fontSize="small" />}
          label="Employee Count"
          value={formatEmployeeCount(department.employeeCount)}
        />
        <StatRow
          icon={<PersonIcon fontSize="small" />}
          label="Department Head"
          value={formatHeadName(department.headName)}
        />
        <StatRow
          icon={<CalendarTodayIcon fontSize="small" />}
          label="Created"
          value={formatDeptCreatedAt(department.createdAt)}
        />
      </CardContent>
    </Card>
  );
}
