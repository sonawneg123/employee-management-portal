/**
 * @fileoverview DepartmentActionsMenu — context menu for per-row department actions.
 */

import React from 'react';
import {
  Divider,
  ListItemIcon,
  ListItemText,
  Menu,
  MenuItem,
} from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import EditIcon       from '@mui/icons-material/Edit';
import DeleteIcon     from '@mui/icons-material/Delete';

/**
 * @typedef {Object} DepartmentActionsMenuProps
 * @property {HTMLElement | null} anchorEl
 * @property {boolean}            open
 * @property {() => void}         onClose
 * @property {() => void}         onView
 * @property {() => void}         [onEdit]
 * @property {() => void}         [onDelete]
 * @property {boolean}            [canEdit]
 * @property {boolean}            [canDelete]
 */

/**
 * Per-row context menu for the department table.
 *
 * @param {DepartmentActionsMenuProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentActionsMenu({
  anchorEl,
  open,
  onClose,
  onView,
  onEdit,
  onDelete,
  canEdit   = false,
  canDelete = false,
}) {
  const handle = (action) => () => { onClose(); action(); };

  return (
    <Menu
      anchorEl={anchorEl}
      open={open}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top',    horizontal: 'right' }}
      slotProps={{ paper: { sx: { minWidth: 160 } } }}
    >
      <MenuItem onClick={handle(onView)} aria-label="View department">
        <ListItemIcon><VisibilityIcon fontSize="small" /></ListItemIcon>
        <ListItemText>View Details</ListItemText>
      </MenuItem>

      {canEdit && (
        <MenuItem onClick={handle(onEdit)} aria-label="Edit department">
          <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Edit</ListItemText>
        </MenuItem>
      )}

      {canEdit && canDelete && <Divider />}

      {canDelete && (
        <MenuItem
          onClick={handle(onDelete)}
          sx={{ color: 'error.main' }}
          aria-label="Delete department"
        >
          <ListItemIcon>
            <DeleteIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Delete</ListItemText>
        </MenuItem>
      )}
    </Menu>
  );
}
