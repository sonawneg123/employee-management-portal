/**
 * @fileoverview EmployeeAvatar — circular avatar for an employee.
 *
 * Shows the employee's profile photo when available, otherwise falls back
 * to a deterministically coloured avatar with the employee's initials.
 */

import React from 'react';
import { Avatar, Tooltip } from '@mui/material';
import { formatInitials, avatarColorFromName } from '@/utils/employeeFormatters';

/**
 * @typedef {Object} EmployeeAvatarProps
 * @property {string | null | undefined} firstName
 * @property {string | null | undefined} lastName
 * @property {string | null | undefined} [profilePhotoUrl]
 * @property {number}                    [size=40]     - Avatar diameter in pixels.
 * @property {boolean}                   [tooltip]     - Wrap in a tooltip showing the full name.
 * @property {string}                    [tooltipText] - Override tooltip text.
 */

/**
 * Circular employee avatar with initials fallback.
 *
 * @param {EmployeeAvatarProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeAvatar({
  firstName,
  lastName,
  profilePhotoUrl,
  size = 40,
  tooltip = false,
  tooltipText,
}) {
  const initials = formatInitials(firstName, lastName);
  const bgColor = avatarColorFromName(firstName);
  const fullName = [firstName, lastName].filter(Boolean).join(' ') || 'Employee';

  const avatar = (
    <Avatar
      src={profilePhotoUrl ?? undefined}
      alt={fullName}
      sx={{
        width: size,
        height: size,
        bgcolor: bgColor,
        fontSize: size * 0.38,
        fontWeight: 700,
        flexShrink: 0,
      }}
      aria-label={`Avatar for ${fullName}`}
    >
      {!profilePhotoUrl && initials}
    </Avatar>
  );

  if (!tooltip) return avatar;

  return (
    <Tooltip title={tooltipText ?? fullName} arrow>
      {avatar}
    </Tooltip>
  );
}
