/**
 * @fileoverview LeaveApprovalDialog — confirmation dialog for approving a leave request.
 */

import React from 'react';
import {
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Typography,
} from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import { formatLeaveDateRange, formatLeaveType } from '@/utils/leaveFormatters';

/**
 * @typedef {Object} LeaveApprovalDialogProps
 * @property {boolean}          open
 * @property {import('@/services/leaveApi').LeaveRequestResponse | null} leave
 * @property {boolean}          isApproving
 * @property {() => void}       onConfirm
 * @property {() => void}       onCancel
 */

/**
 * Approval confirmation dialog.
 *
 * @param {LeaveApprovalDialogProps} props
 * @returns {JSX.Element}
 */
export default function LeaveApprovalDialog({ open, leave, isApproving, onConfirm, onCancel }) {
  const employeeName = leave?.employeeName ?? 'this employee';

  return (
    <Dialog
      open={open}
      onClose={isApproving ? undefined : onCancel}
      maxWidth="xs"
      fullWidth
      aria-labelledby="approve-leave-title"
    >
      <DialogTitle id="approve-leave-title" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <CheckCircleIcon color="success" />
        Approve Leave Request
      </DialogTitle>

      <DialogContent>
        <DialogContentText>
          Approve the{' '}
          <Typography component="span" fontWeight={700} color="text.primary">
            {formatLeaveType(leave?.leaveType)}
          </Typography>{' '}
          request for{' '}
          <Typography component="span" fontWeight={700} color="text.primary">
            {employeeName}
          </Typography>{' '}
          ({formatLeaveDateRange(leave?.startDate, leave?.endDate)})?
        </DialogContentText>
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onCancel} disabled={isApproving} aria-label="Cancel">
          Cancel
        </Button>
        <Button
          variant="contained"
          color="success"
          onClick={onConfirm}
          disabled={isApproving}
          startIcon={isApproving ? <CircularProgress size={16} color="inherit" /> : null}
          aria-label={`Approve leave for ${employeeName}`}
        >
          {isApproving ? 'Approving…' : 'Approve'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
