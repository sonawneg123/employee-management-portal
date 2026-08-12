/**
 * @fileoverview DepartmentEmployeeList — list of employees in a department.
 *
 * Shown on the DepartmentDetailsPage. Fetches employees filtered by
 * departmentId and renders them as a compact avatar + name list.
 */

import React from 'react';
import {
  Avatar,
  Box,
  Card,
  CardContent,
  Divider,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Skeleton,
  Typography,
} from '@mui/material';
import { useEmployees } from '@/hooks/useEmployees';
import { formatFullName } from '@/utils/employeeFormatters';
import { avatarColorFromName } from '@/utils/employeeFormatters';
import EmployeeStatusChip from '@/components/employees/EmployeeStatusChip';

/**
 * @typedef {Object} DepartmentEmployeeListProps
 * @property {string} departmentId - UUID of the department to list employees for.
 */

/**
 * Card showing employees belonging to a specific department.
 *
 * @param {DepartmentEmployeeListProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentEmployeeList({ departmentId }) {
  const { data, isLoading } = useEmployees({
    departmentId,
    size: 50,
    sort: 'employeeCode',
    direction: 'asc',
  });

  const employees = data?.content ?? [];
  const total     = data?.totalElements ?? 0;

  return (
    <Card>
      <CardContent>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
          <Typography variant="subtitle2" fontWeight={700}>
            Employees
          </Typography>
          {!isLoading && (
            <Typography variant="caption" color="text.secondary">
              {total} total
            </Typography>
          )}
        </Box>
        <Divider sx={{ mb: 1 }} />

        {isLoading ? (
          <List disablePadding>
            {[0, 1, 2, 3].map((i) => (
              <ListItem key={i} sx={{ px: 0 }}>
                <ListItemAvatar>
                  <Skeleton variant="circular" width={36} height={36} />
                </ListItemAvatar>
                <ListItemText
                  primary={<Skeleton variant="text" width="60%" />}
                  secondary={<Skeleton variant="text" width="40%" />}
                />
              </ListItem>
            ))}
          </List>
        ) : employees.length === 0 ? (
          <Box sx={{ py: 4, textAlign: 'center', color: 'text.disabled' }}>
            <Typography variant="body2">No employees in this department.</Typography>
          </Box>
        ) : (
          <List disablePadding>
            {employees.map((emp) => {
              const fullName = formatFullName(emp.firstName, emp.lastName);
              return (
                <ListItem key={emp.id} sx={{ px: 0, py: 0.75 }}
                  secondaryAction={<EmployeeStatusChip status={emp.status} size="small" />}
                >
                  <ListItemAvatar sx={{ minWidth: 44 }}>
                    <Avatar
                      src={emp.profilePhotoUrl ?? undefined}
                      sx={{
                        width: 36,
                        height: 36,
                        bgcolor: avatarColorFromName(emp.firstName),
                        fontSize: 14,
                        fontWeight: 700,
                      }}
                      aria-label={fullName}
                    >
                      {!emp.profilePhotoUrl && (emp.firstName?.[0] ?? '?')}
                    </Avatar>
                  </ListItemAvatar>
                  <ListItemText
                    primary={
                      <Typography variant="body2" fontWeight={500} noWrap>
                        {fullName}
                      </Typography>
                    }
                    secondary={
                      <Typography variant="caption" color="text.secondary" noWrap>
                        {emp.jobTitle ?? '—'}
                      </Typography>
                    }
                  />
                </ListItem>
              );
            })}
          </List>
        )}
      </CardContent>
    </Card>
  );
}
