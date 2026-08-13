/**
 * @fileoverview EmployeeCard — compact card view of a single employee.
 *
 * Used in mobile-responsive list views where the full table isn't practical.
 * Displays avatar, name, job title, department chip, status chip, and
 * an optional actions menu trigger.
 */

import React from 'react';
import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import EmployeeAvatar from './EmployeeAvatar';
import EmployeeStatusChip from './EmployeeStatusChip';
import EmployeeDepartmentChip from './EmployeeDepartmentChip';
import { formatFullName, formatJoinDate } from '@/utils/employeeFormatters';

/**
 * @typedef {Object} EmployeeCardProps
 * @property {import('@/services/employeeApi').EmployeeResponse} employee
 * @property {() => void}    [onClick]     - Called when the card body is clicked.
 * @property {(event: React.MouseEvent, employee: Object) => void} [onMenuOpen]
 *   - Called when the ⋮ menu button is clicked; receives the trigger event.
 * @property {boolean}       [selected]    - Highlights the card when true.
 */

/**
 * Mobile-friendly employee card.
 *
 * @param {EmployeeCardProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeCard({ employee, onClick, onMenuOpen, selected = false }) {
  const fullName = formatFullName(employee.firstName, employee.lastName);

  return (
    <Card
      variant="outlined"
      sx={{
        mb: 1.5,
        borderColor: selected ? 'primary.main' : 'divider',
        borderWidth: selected ? 2 : 1,
      }}
      aria-label={`Employee card for ${fullName}`}
    >
      <Box sx={{ display: 'flex', alignItems: 'stretch' }}>
        <CardActionArea
          onClick={onClick}
          sx={{ flex: 1 }}
          aria-label={`View details for ${fullName}`}
        >
          <CardContent sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
            <EmployeeAvatar
              firstName={employee.firstName}
              lastName={employee.lastName}
              profilePhotoUrl={employee.profilePhotoUrl}
              size={48}
            />
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Typography variant="subtitle2" fontWeight={700} noWrap>
                {fullName}
              </Typography>
              <Typography variant="caption" color="text.secondary" noWrap>
                {employee.jobTitle ?? '—'}
              </Typography>
              <Box sx={{ display: 'flex', gap: 0.75, mt: 1, flexWrap: 'wrap' }}>
                <EmployeeDepartmentChip departmentName={employee.departmentName} size="small" />
                <EmployeeStatusChip status={employee.status} size="small" />
              </Box>
              <Typography
                variant="caption"
                color="text.disabled"
                sx={{ mt: 0.5, display: 'block' }}
              >
                Joined {formatJoinDate(employee.dateOfJoining)}
              </Typography>
            </Box>
          </CardContent>
        </CardActionArea>

        {onMenuOpen && (
          <Box sx={{ display: 'flex', alignItems: 'center', pr: 1 }}>
            <Tooltip title="Actions">
              <IconButton
                size="small"
                onClick={(e) => onMenuOpen(e, employee)}
                aria-label={`Actions for ${fullName}`}
              >
                <MoreVertIcon fontSize="small" />
              </IconButton>
            </Tooltip>
          </Box>
        )}
      </Box>
    </Card>
  );
}
