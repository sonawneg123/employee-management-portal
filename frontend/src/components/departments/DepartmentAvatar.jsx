/**
 * @fileoverview DepartmentAvatar — circular avatar for a department.
 *
 * Shows a deterministically coloured avatar with the department's initials.
 * No photo support — departments use initials only.
 */

import React from 'react';
import { Avatar, Tooltip } from '@mui/material';
import { deptInitials, deptAvatarColor } from '@/utils/departmentFormatters';

/**
 * @typedef {Object} DepartmentAvatarProps
 * @property {string | null | undefined} name      - Department name.
 * @property {number}                    [size=40]  - Diameter in pixels.
 * @property {boolean}                   [tooltip]  - Wrap in a name tooltip.
 */

/**
 * Circular department avatar showing initials on a deterministic background.
 *
 * @param {DepartmentAvatarProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentAvatar({ name, size = 40, tooltip = false }) {
  const initials = deptInitials(name);
  const bgColor = deptAvatarColor(name);

  const avatar = (
    <Avatar
      sx={{
        width: size,
        height: size,
        bgcolor: bgColor,
        fontSize: size * 0.36,
        fontWeight: 700,
        flexShrink: 0,
      }}
      aria-label={`Department avatar for ${name ?? 'unknown'}`}
    >
      {initials}
    </Avatar>
  );

  if (!tooltip || !name) return avatar;

  return (
    <Tooltip title={name} arrow>
      {avatar}
    </Tooltip>
  );
}
