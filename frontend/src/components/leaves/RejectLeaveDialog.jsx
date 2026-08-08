/**
 * @fileoverview RejectLeaveDialog — confirmation + reason dialog for rejecting a leave request.
 */

import React, { useState } from 'react';
import {
  Button, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogContentText, DialogTitle, TextField, Typography,
} from '@mui/material';
import CancelIcon from '@mui/icons-material/Cancel';
import { formatLeaveType } from '@/utils/leaveFormatters';
import { formatLeaveDateRange } from '@/utils/leaveFormatters';

/**
 * @typedef {Object} RejectLeaveDialogProps
 * @property {boolean}          open
 * @property {import('@/services/leaveApi').LeaveRequestResponse | null} leave
 * @property {boolean}          isRejecting
 * @property {(reason: string) => void} onConfirm
 * @property {() => void}       onCancel
 */

/**
 * Rejection dialog with optional reason input.
 *
 * @param {RejectLeaveDialogProps} props
 * @returns {JSX.Element}
 */
export default function RejectLeaveDialog({ open, leave, isRejecting, onConfirm, onCancel }) {
  const [reason, setReason] = useState('');
  const employeeName = leave?.employeeName ?? 'this employee';

  const handleConfirm = () => {
    onConfirm(reason.trim());
    setReason('');
  };

  const handleCancel = () => {
    setReason('');
    onCancel();
  };

  return (
    <Dialog
      open={open}
      onClose={isRejecting ? undefined : handleCancel}
      maxWidth="xs"
      fullWidth
      aria-labelledby="reject-leave-title"
    >
      <DialogTitle id="reject-leave-title" sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
        <CancelIcon color="error" />
        Reject Leave Request
      </DialogTitle>

      <DialogContent>
        <DialogContentText sx={{ mb: 2 }}>
          Reject the{' '}
          <Typography component="span" fontWeight={700} color="text.primary">
            {formatLeaveType(leave?.leaveType)}
          </Typography>{' '}
          request for{' '}
          <Typography component="span" fontWeight={700} color="text.primary">
            {employeeName}
          </Typography>{' '}
          ({formatLeaveDateRange(leave?.startDate, leave?.endDate)})?
        </DialogContentText>

        <TextField
          label="Rejection Reason (optional)"
          value={reason}
          onChange={(e) => setReason(e.target.value)}
          fullWidth
          multiline
          rows={2}
          disabled={isRejecting}
          placeholder="Provide a brief reason for the employee…"
          inputProps={{ 'aria-label': 'Rejection reason' }}
        />
      </DialogContent>

      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={handleCancel} disabled={isRejecting} aria-label="Cancel">Cancel</Button>
        <Button
          variant="contained"
          color="error"
          onClick={handleConfirm}
          disabled={isRejecting}
          startIcon={isRejecting ? <CircularProgress size={16} color="inherit" /> : null}
          aria-label={`Confirm rejection for ${employeeName}`}
        >
          {isRejecting ? 'Rejecting…' : 'Reject'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
