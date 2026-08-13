/**
 * @fileoverview LeaveActionsMenu — per-row context menu for leave requests.
 *
 * Visibility of approve/reject/cancel depends on the leave status and
 * the caller-provided role flags.
 */

import React from 'react';
import { Divider, ListItemIcon, ListItemText, Menu, MenuItem } from '@mui/material';
import VisibilityIcon from '@mui/icons-material/Visibility';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import CancelIcon from '@mui/icons-material/Cancel';
import DeleteIcon from '@mui/icons-material/Delete';
import EditIcon from '@mui/icons-material/Edit';

/**
 * @typedef {Object} LeaveActionsMenuProps
 * @property {HTMLElement | null} anchorEl
 * @property {boolean}            open
 * @property {() => void}         onClose
 * @property {() => void}         onView
 * @property {() => void}         [onApprove]
 * @property {() => void}         [onReject]
 * @property {() => void}         [onEdit]
 * @property {() => void}         [onCancel]
 * @property {boolean}            [canApprove]
 * @property {boolean}            [canEdit]
 * @property {boolean}            [canCancel]
 */

/**
 * Context menu for leave request rows.
 *
 * @param {LeaveActionsMenuProps} props
 * @returns {JSX.Element}
 */
export default function LeaveActionsMenu({
  anchorEl,
  open,
  onClose,
  onView,
  onApprove,
  onReject,
  onEdit,
  onCancel,
  canApprove = false,
  canEdit = false,
  canCancel = false,
}) {
  const handle = (fn) => () => {
    onClose();
    fn && fn();
  };

  return (
    <Menu
      anchorEl={anchorEl}
      open={open}
      onClose={onClose}
      anchorOrigin={{ vertical: 'bottom', horizontal: 'right' }}
      transformOrigin={{ vertical: 'top', horizontal: 'right' }}
      slotProps={{ paper: { sx: { minWidth: 180 } } }}
    >
      <MenuItem onClick={handle(onView)} aria-label="View leave details">
        <ListItemIcon>
          <VisibilityIcon fontSize="small" />
        </ListItemIcon>
        <ListItemText>View Details</ListItemText>
      </MenuItem>

      {canEdit && (
        <MenuItem onClick={handle(onEdit)} aria-label="Edit leave request">
          <ListItemIcon>
            <EditIcon fontSize="small" />
          </ListItemIcon>
          <ListItemText>Edit</ListItemText>
        </MenuItem>
      )}

      {canApprove && <Divider />}

      {canApprove && (
        <MenuItem
          onClick={handle(onApprove)}
          sx={{ color: 'success.main' }}
          aria-label="Approve leave"
        >
          <ListItemIcon>
            <CheckCircleIcon fontSize="small" color="success" />
          </ListItemIcon>
          <ListItemText>Approve</ListItemText>
        </MenuItem>
      )}

      {canApprove && (
        <MenuItem onClick={handle(onReject)} sx={{ color: 'error.main' }} aria-label="Reject leave">
          <ListItemIcon>
            <CancelIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Reject</ListItemText>
        </MenuItem>
      )}

      {canCancel && <Divider />}

      {canCancel && (
        <MenuItem onClick={handle(onCancel)} sx={{ color: 'error.main' }} aria-label="Cancel leave">
          <ListItemIcon>
            <DeleteIcon fontSize="small" color="error" />
          </ListItemIcon>
          <ListItemText>Cancel Request</ListItemText>
        </MenuItem>
      )}
    </Menu>
  );
}
