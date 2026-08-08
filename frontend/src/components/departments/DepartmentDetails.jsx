/**
 * @fileoverview DepartmentDetails — full read-only detail card for a department.
 *
 * Renders name, code chip, description, head name, employee count, and timestamps.
 * Used in {@link DepartmentDetailsPage}.
 */

import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  Skeleton,
  Typography,
} from '@mui/material';
import BadgeIcon         from '@mui/icons-material/Badge';
import DescriptionIcon   from '@mui/icons-material/Description';
import PersonIcon        from '@mui/icons-material/Person';
import PeopleIcon        from '@mui/icons-material/People';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import UpdateIcon        from '@mui/icons-material/Update';
import DepartmentAvatar from './DepartmentAvatar';
import {
  formatDeptCode,
  formatHeadName,
  formatEmployeeCount,
  formatDeptCreatedAt,
} from '@/utils/departmentFormatters';

/**
 * @typedef {Object} DetailRowProps
 * @property {React.ReactNode} icon
 * @property {string}          label
 * @property {React.ReactNode} value
 */

/**
 * @param {DetailRowProps} props
 * @returns {JSX.Element}
 */
function DetailRow({ icon, label, value }) {
  return (
    <Box sx={{ display: 'flex', gap: 1.5, alignItems: 'flex-start', py: 1 }}>
      <Box sx={{ color: 'text.secondary', mt: 0.3, flexShrink: 0 }}>{icon}</Box>
      <Box>
        <Typography variant="caption" color="text.secondary" display="block">{label}</Typography>
        <Typography variant="body2" fontWeight={500}>{value}</Typography>
      </Box>
    </Box>
  );
}

/**
 * @typedef {Object} DepartmentDetailsProps
 * @property {import('@/services/departmentApi').DepartmentResponse | undefined} department
 * @property {boolean} isLoading
 */

/**
 * Full read-only department detail card.
 *
 * @param {DepartmentDetailsProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentDetails({ department, isLoading }) {
  if (isLoading) {
    return (
      <Card>
        <CardContent>
          <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
            <Skeleton variant="circular" width={80} height={80} />
            <Box sx={{ flex: 1 }}>
              <Skeleton variant="text" width="50%" height={32} />
              <Skeleton variant="rectangular" width={60} height={24} sx={{ mt: 0.5, borderRadius: 4 }} />
            </Box>
          </Box>
          {[0, 1, 2, 3].map((i) => (
            <Box key={i} sx={{ display: 'flex', gap: 2, mb: 2 }}>
              <Skeleton variant="circular" width={24} height={24} />
              <Box sx={{ flex: 1 }}>
                <Skeleton variant="text" width="30%" />
                <Skeleton variant="text" width="55%" />
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
      <CardContent>
        {/* Header */}
        <Box sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', mb: 2.5 }}>
          <DepartmentAvatar name={department.name} size={80} />
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography variant="h5" fontWeight={700} gutterBottom noWrap>
              {department.name}
            </Typography>
            <Chip
              icon={<BadgeIcon />}
              label={formatDeptCode(department.code)}
              size="small"
              variant="outlined"
              sx={{ fontFamily: 'monospace', fontWeight: 700 }}
              aria-label={`Code: ${department.code}`}
            />
          </Box>
        </Box>

        <Divider sx={{ mb: 2 }} />

        <Grid container spacing={0}>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailRow
              icon={<DescriptionIcon fontSize="small" />}
              label="Description"
              value={department.description || '—'}
            />
            <DetailRow
              icon={<PersonIcon fontSize="small" />}
              label="Department Head"
              value={formatHeadName(department.headName)}
            />
          </Grid>
          <Grid size={{ xs: 12, sm: 6 }}>
            <DetailRow
              icon={<PeopleIcon fontSize="small" />}
              label="Employees"
              value={formatEmployeeCount(department.employeeCount)}
            />
            <DetailRow
              icon={<CalendarTodayIcon fontSize="small" />}
              label="Created"
              value={formatDeptCreatedAt(department.createdAt)}
            />
            <DetailRow
              icon={<UpdateIcon fontSize="small" />}
              label="Last Updated"
              value={formatDeptCreatedAt(department.updatedAt)}
            />
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
}
