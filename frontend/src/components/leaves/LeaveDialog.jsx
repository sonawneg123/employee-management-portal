/**
 * @fileoverview LeaveDialog — modal wrapping LeaveForm for create/edit.
 */

import React from 'react';
import {
  Button, CircularProgress, Dialog, DialogActions, DialogContent,
  DialogTitle, IconButton, Tooltip,
} from '@mui/material';
import CloseIcon from '@mui/icons-material/Close';
import LeaveForm from './LeaveForm';

const FORM_ID = 'leave-dialog-form';

/**
 * @typedef {Object} LeaveDialogProps
 * @property {boolean}          open
 * @property {'create'|'edit'}  mode
 * @property {Object}           [defaultValues]
 * @property {boolean}          isSubmitting
 * @property {Record<string,string>} [serverErrors]
 * @property {(data: Object) => void} onSubmit
 * @property {() => void}       onClose
 */

/**
 * Leave request create/edit modal dialog.
 *
 * @param {LeaveDialogProps} props
 * @returns {JSX.Element}
 */
export default function LeaveDialog({ open, mode, defaultValues, isSubmitting, serverErrors, onSubmit, onClose }) {
  const isEdit = mode === 'edit';
  const title  = isEdit ? 'Edit Leave Request' : 'Request Leave';

  return (
    <Dialog
      open={open}
      onClose={isSubmitting ? undefined : onClose}
      maxWidth="sm"
      fullWidth
      scroll="paper"
      aria-labelledby="leave-dialog-title"
    >
      <DialogTitle
        id="leave-dialog-title"
        sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}
      >
        {title}
        <Tooltip title="Close">
          <IconButton onClick={onClose} disabled={isSubmitting} size="small" aria-label="Close">
            <CloseIcon fontSize="small" />
          </IconButton>
        </Tooltip>
      </DialogTitle>

      <DialogContent dividers>
        <LeaveForm
          formId={FORM_ID}
          defaultValues={defaultValues}
          onSubmit={onSubmit}
          isSubmitting={isSubmitting}
          serverErrors={serverErrors}
        />
      </DialogContent>

      <DialogActions sx={{ px: 3, py: 2 }}>
        <Button onClick={onClose} disabled={isSubmitting} aria-label="Cancel">Cancel</Button>
        <Button
          type="submit"
          form={FORM_ID}
          variant="contained"
          disabled={isSubmitting}
          startIcon={isSubmitting ? <CircularProgress size={16} color="inherit" /> : null}
          aria-label={isEdit ? 'Save changes' : 'Submit request'}
        >
          {isSubmitting ? 'Saving…' : isEdit ? 'Save Changes' : 'Submit Request'}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
