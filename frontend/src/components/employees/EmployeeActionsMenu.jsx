/**
 * @fileoverview EmployeeActionsMenu — context menu for per-row employee actions.
 *
 * Renders view, edit, and delete actions. Visibility of edit/delete depends on
 * the `canEdit` and `canDelete` props so the parent can apply role guards.
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
 * @typedef {Object} EmployeeActionsMenuProps
 * @property {HTMLElement | null}  anchorEl   - Anchor element for the menu position.
 * @property {boolean}             open       - Whether the menu is visible.
 * @property {() => void}          onClose    - Called when the menu should close.
 * @property {() => void}          onView     - Navigate to employee details.
 * @property {() => void}          [onEdit]   - Open the edit dialog.
 * @property {() => void}          [onDelete] - Open the delete confirmation.
 * @property {boolean}             [canEdit]  - Shows the Edit item when true.
 * @property {boolean}             [canDelete]- Shows the Delete item when true.
 */

/**
 * Per-row actions menu for the employee table and cards.
 *
 * @param {EmployeeActionsMenuProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeActionsMenu({
  anchorEl,
  open,
  onClose,
  onView,
  onEdit,
  onDelete,
  canEdit   = false,
  canDelete = false,
}) {
  /**
   * Wraps an action callback so the menu closes after the action fires.
   *
   * @param {() => void} action
   * @returns {() => void}
   */
  const handle = (action) => () => {
    onClose();
    action();
  };

  return (
    <Menu
      anchorEl={anchorEl}
      open={open}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top',    horizontal: 'right' }}
      slotProps={{ paper: { sx: { minWidth: 160 } } }}
    >
      <MenuItem onClick={handle(onView)} aria-label="View employee details">
        <ListItemIcon><VisibilityIcon fontSize="small" /></ListItemIcon>
        <ListItemText>View Details</ListItemText>
      </MenuItem>

      {canEdit && (
        <MenuItem onClick={handle(onEdit)} aria-label="Edit employee">
          <ListItemIcon><EditIcon fontSize="small" /></ListItemIcon>
          <ListItemText>Edit</ListItemText>
        </MenuItem>
      )}

      {(canEdit && canDelete) && <Divider />}

      {canDelete && (
        <MenuItem
          onClick={handle(onDelete)}
          sx={{ color: 'error.main' }}
          aria-label="Delete employee"
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
