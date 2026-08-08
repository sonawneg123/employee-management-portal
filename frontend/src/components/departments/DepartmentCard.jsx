/**
 * @fileoverview DepartmentCard — compact card view of a single department.
 *
 * Used in the mobile-responsive list layout. Displays avatar, name, code,
 * employee count and an optional actions menu trigger.
 */

import React from 'react';
import {
  Box,
  Card,
  CardActionArea,
  CardContent,
  Chip,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import MoreVertIcon  from '@mui/icons-material/MoreVert';
import PeopleIcon    from '@mui/icons-material/People';
import DepartmentAvatar from './DepartmentAvatar';
import { formatDeptCode, formatHeadName } from '@/utils/departmentFormatters';

/**
 * @typedef {Object} DepartmentCardProps
 * @property {import('@/services/departmentApi').DepartmentResponse} department
 * @property {() => void} [onClick]
 * @property {(event: React.MouseEvent, department: Object) => void} [onMenuOpen]
 * @property {boolean} [selected]
 */

/**
 * Mobile-friendly department card.
 *
 * @param {DepartmentCardProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentCard({ department, onClick, onMenuOpen, selected = false }) {
  return (
    <Card
      variant="outlined"
      sx={{
        mb: 1.5,
        borderColor: selected ? 'primary.main' : 'divider',
        borderWidth: selected ? 2 : 1,
      }}
      aria-label={`Department card for ${department.name}`}
    >
      <Box sx={{ display: 'flex', alignItems: 'stretch' }}>
        <CardActionArea onClick={onClick} sx={{ flex: 1 }} aria-label={`View ${department.name}`}>
          <CardContent sx={{ display: 'flex', gap: 2, alignItems: 'flex-start' }}>
            <DepartmentAvatar name={department.name} size={48} />
            <Box sx={{ flex: 1, minWidth: 0 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.25 }}>
                <Typography variant="subtitle2" fontWeight={700} noWrap>
                  {department.name}
                </Typography>
                <Chip
                  label={formatDeptCode(department.code)}
                  size="small"
                  variant="outlined"
                  sx={{ fontFamily: 'monospace', fontWeight: 700, fontSize: 11 }}
                />
              </Box>
              {department.description && (
                <Typography variant="caption" color="text.secondary" noWrap>
                  {department.description}
                </Typography>
              )}
              <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mt: 0.75 }}>
                <PeopleIcon sx={{ fontSize: 14, color: 'text.secondary' }} />
                <Typography variant="caption" color="text.secondary">
                  {department.employeeCount ?? 0} employees
                </Typography>
                {department.headName && (
                  <>
                    <Typography variant="caption" color="text.disabled" sx={{ mx: 0.5 }}>·</Typography>
                    <Typography variant="caption" color="text.secondary" noWrap>
                      {formatHeadName(department.headName)}
                    </Typography>
                  </>
                )}
              </Box>
            </Box>
          </CardContent>
        </CardActionArea>

        {onMenuOpen && (
          <Box sx={{ display: 'flex', alignItems: 'center', pr: 1 }}>
            <Tooltip title="Actions">
              <IconButton
                size="small"
                onClick={(e) => onMenuOpen(e, department)}
                aria-label={`Actions for ${department.name}`}
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
