/**
 * @fileoverview QuickActions — grid of shortcut buttons for common tasks.
 *
 * Renders a role-filtered set of action buttons that navigate to the most
 * common workflows. Each button is only shown to roles that are permitted
 * to access that workflow.
 */

import React from 'react';
import { useNavigate } from 'react-router-dom';
import { Box, Button, Grid, Typography } from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import EventNoteIcon from '@mui/icons-material/EventNote';
import ApartmentIcon from '@mui/icons-material/Apartment';
import AccessTimeIcon from '@mui/icons-material/AccessTime';
import StarRateIcon from '@mui/icons-material/StarRate';
import PeopleIcon from '@mui/icons-material/People';
import { useAuth } from '@/hooks/useAuth';
import { ROLES } from '@/constants/roles';
import { ROUTES } from '@/constants/routes';

/**
 * @typedef {Object} QuickActionDef
 * @property {string}           id
 * @property {string}           label
 * @property {string}           path
 * @property {React.ElementType} Icon
 * @property {string}           color  - MUI button colour.
 * @property {string[]}         roles  - Roles that may see this action.
 */

/** @type {QuickActionDef[]} */
const QUICK_ACTIONS = [
  {
    id: 'add-employee',
    label: 'Add Employee',
    path: ROUTES.EMPLOYEES,
    Icon: PersonAddIcon,
    color: 'primary',
    roles: [ROLES.ADMIN, ROLES.HR],
  },
  {
    id: 'manage-leaves',
    label: 'Manage Leaves',
    path: ROUTES.LEAVES,
    Icon: EventNoteIcon,
    color: 'warning',
    roles: [ROLES.ADMIN, ROLES.HR, ROLES.MANAGER],
  },
  {
    id: 'departments',
    label: 'Departments',
    path: ROUTES.DEPARTMENTS,
    Icon: ApartmentIcon,
    color: 'secondary',
    roles: [ROLES.ADMIN, ROLES.HR],
  },
  {
    id: 'attendance',
    label: 'Attendance',
    path: ROUTES.ATTENDANCE,
    Icon: AccessTimeIcon,
    color: 'info',
    roles: [ROLES.ADMIN, ROLES.HR, ROLES.MANAGER, ROLES.EMPLOYEE],
  },
  {
    id: 'reviews',
    label: 'Reviews',
    path: ROUTES.REVIEWS,
    Icon: StarRateIcon,
    color: 'success',
    roles: [ROLES.ADMIN, ROLES.HR, ROLES.MANAGER],
  },
  {
    id: 'employees',
    label: 'View Employees',
    path: ROUTES.EMPLOYEES,
    Icon: PeopleIcon,
    color: 'primary',
    roles: [ROLES.ADMIN, ROLES.HR, ROLES.MANAGER, ROLES.EMPLOYEE],
  },
];

/**
 * Role-filtered quick action button grid.
 *
 * @returns {JSX.Element}
 */
export default function QuickActions() {
  const navigate = useNavigate();
  const { hasAnyRole } = useAuth();

  const visible = QUICK_ACTIONS.filter((a) => hasAnyRole(a.roles));

  if (!visible.length) return null;

  return (
    <Box sx={{ mb: 3 }}>
      <Typography variant="subtitle1" fontWeight={600} sx={{ mb: 2 }}>
        Quick Actions
      </Typography>
      <Grid container spacing={1.5}>
        {visible.map(({ id, label, path, Icon, color }) => (
          <Grid key={id} size={{ xs: 6, sm: 4, md: 'auto' }}>
            <Button
              variant="outlined"
              color={color}
              startIcon={<Icon />}
              onClick={() => navigate(path)}
              fullWidth
              size="small"
              sx={{
                justifyContent: 'flex-start',
                whiteSpace: 'nowrap',
                borderRadius: 2,
                py: 1,
              }}
              aria-label={label}
            >
              {label}
            </Button>
          </Grid>
        ))}
      </Grid>
    </Box>
  );
}
