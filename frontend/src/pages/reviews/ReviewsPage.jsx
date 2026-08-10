/**
 * ReviewsPage.jsx
 * Full performance review management.
 * Roles: ADMIN/HR/MANAGER can create, edit, delete.
 *         EMPLOYEE can only view their own reviews.
 */
import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  FormControl,
  FormHelperText,
  IconButton,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Skeleton,
  Snackbar,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TablePagination,
  TableRow,
  TableSortLabel,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Star as StarIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  getReviews,
  createReview,
  updateReview,
  deleteReview,
} from '../../services/reviewApi';
import { useAuth } from '../../hooks/useAuth';

// ── Rating helpers ─────────────────────────────────────────────────────────

const RATING_LABELS = ['', 'Unsatisfactory', 'Needs Improvement',
  'Meets Expectations', 'Good', 'Outstanding'];

const RATING_COLORS = ['', 'error', 'warning', 'info', 'success', 'success'];

function RatingChip({ rating }) {
  return (
    <Chip
      icon={<StarIcon />}
      label={`${rating} — ${RATING_LABELS[rating] ?? 'Unknown'}`}
      color={RATING_COLORS[rating] ?? 'default'}
      size="small"
      variant="outlined"
    />
  );
}

// ── Empty form state ───────────────────────────────────────────────────────

const emptyForm = () => ({
  employeeId: '',
  reviewPeriod: '',
  rating: '',
  reviewDate: '',
  comments: '',
  goals: '',
});

// ── Main component ─────────────────────────────────────────────────────────

export default function ReviewsPage() {
  const { user, hasAnyRole } = useAuth();
  const queryClient = useQueryClient();

  const canManage = hasAnyRole(['ROLE_ADMIN', 'ROLE_HR', 'ROLE_MANAGER']);
  const canDelete = hasAnyRole(['ROLE_ADMIN']);

  // ── Pagination / sort state ────────────────────────────────────────────
  const [page, setPage]       = useState(0);
  const [rowsPerPage, setRows] = useState(20);
  const [sortBy, setSortBy]   = useState('reviewDate');
  const [sortDir, setSortDir] = useState('desc');

  // ── Dialog state ───────────────────────────────────────────────────────
  const [formOpen, setFormOpen]       = useState(false);
  const [editTarget, setEditTarget]   = useState(null); // ReviewResponse | null
  const [formData, setFormData]       = useState(emptyForm());
  const [formErrors, setFormErrors]   = useState({});
  const [viewTarget, setViewTarget]   = useState(null); // ReviewResponse | null
  const [deleteTarget, setDeleteTarget] = useState(null);

  // ── Snackbar ───────────────────────────────────────────────────────────
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'success' });
  const showSnack = (message, severity = 'success') =>
    setSnack({ open: true, message, severity });

  // ── Query ──────────────────────────────────────────────────────────────
  const { data, isLoading, isError } = useQuery({
    queryKey: ['reviews', page, rowsPerPage, sortBy, sortDir],
    queryFn: () =>
      getReviews({ page, size: rowsPerPage, sortBy, sortDir })
        .then((r) => r.data),
    keepPreviousData: true,
  });

  // ── Mutations ──────────────────────────────────────────────────────────
  const onCreate = useMutation({
    mutationFn: (payload) => createReview(payload).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries(['reviews']);
      setFormOpen(false);
      showSnack('Review created successfully.');
    },
    onError: (err) => handleMutationError(err),
  });

  const onUpdate = useMutation({
    mutationFn: ({ id, payload }) => updateReview(id, payload).then((r) => r.data),
    onSuccess: () => {
      queryClient.invalidateQueries(['reviews']);
      setFormOpen(false);
      showSnack('Review updated successfully.');
    },
    onError: (err) => handleMutationError(err),
  });

  const onDelete = useMutation({
    mutationFn: (id) => deleteReview(id),
    onSuccess: () => {
      queryClient.invalidateQueries(['reviews']);
      setDeleteTarget(null);
      showSnack('Review deleted.', 'info');
    },
    onError: () => showSnack('Failed to delete review.', 'error'),
  });

  const handleMutationError = (err) => {
    const data = err.response?.data;
    if (data?.violations) {
      setFormErrors(data.violations);
    } else {
      showSnack(data?.detail || 'An error occurred.', 'error');
    }
  };

  // ── Form handlers ──────────────────────────────────────────────────────
  const openCreate = () => {
    setEditTarget(null);
    setFormData(emptyForm());
    setFormErrors({});
    setFormOpen(true);
  };

  const openEdit = (review) => {
    setEditTarget(review);
    setFormData({
      employeeId: review.employeeId,
      reviewPeriod: review.reviewPeriod,
      rating: review.rating,
      reviewDate: review.reviewDate,
      comments: review.comments || '',
      goals: review.goals || '',
    });
    setFormErrors({});
    setFormOpen(true);
  };

  const handleFormChange = (e) => {
    const { name, value } = e.target;
    setFormData((p) => ({ ...p, [name]: value }));
    setFormErrors((p) => ({ ...p, [name]: '' }));
  };

  const validateForm = () => {
    const errs = {};
    if (!editTarget && !formData.employeeId) errs.employeeId = 'Employee ID is required';
    if (!formData.reviewPeriod) errs.reviewPeriod = 'Review period is required';
    if (!formData.rating) errs.rating = 'Rating is required';
    if (!formData.reviewDate) errs.reviewDate = 'Review date is required';
    setFormErrors(errs);
    return Object.keys(errs).length === 0;
  };

  const handleFormSubmit = () => {
    if (!validateForm()) return;
    if (editTarget) {
      const { employeeId, ...payload } = formData; // employeeId not updatable
      onUpdate.mutate({ id: editTarget.id, payload: { ...payload, rating: Number(payload.rating) } });
    } else {
      onCreate.mutate({ ...formData, rating: Number(formData.rating) });
    }
  };

  // ── Sort ───────────────────────────────────────────────────────────────
  const handleSort = (field) => {
    if (sortBy === field) {
      setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'));
    } else {
      setSortBy(field);
      setSortDir('desc');
    }
    setPage(0);
  };

  // ── Render ─────────────────────────────────────────────────────────────
  const rows = data?.content ?? [];
  const total = data?.totalElements ?? 0;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>
          Performance Reviews
        </Typography>
        {canManage && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={openCreate}
          >
            New Review
          </Button>
        )}
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          Failed to load reviews. Please try again.
        </Alert>
      )}

      <TableContainer component={Paper} elevation={2}>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Employee</TableCell>
              <TableCell>Department</TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortBy === 'reviewPeriod'}
                  direction={sortBy === 'reviewPeriod' ? sortDir : 'desc'}
                  onClick={() => handleSort('reviewPeriod')}
                >
                  Period
                </TableSortLabel>
              </TableCell>
              <TableCell>Rating</TableCell>
              <TableCell>
                <TableSortLabel
                  active={sortBy === 'reviewDate'}
                  direction={sortBy === 'reviewDate' ? sortDir : 'desc'}
                  onClick={() => handleSort('reviewDate')}
                >
                  Date
                </TableSortLabel>
              </TableCell>
              <TableCell>Reviewer</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading
              ? Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i}>
                    {Array.from({ length: 7 }).map((__, j) => (
                      <TableCell key={j}><Skeleton /></TableCell>
                    ))}
                  </TableRow>
                ))
              : rows.length === 0
                ? (
                  <TableRow>
                    <TableCell colSpan={7} align="center" sx={{ py: 4 }}>
                      <Typography color="text.secondary">
                        No performance reviews found.
                      </Typography>
                    </TableCell>
                  </TableRow>
                )
                : rows.map((review) => (
                  <TableRow key={review.id} hover>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>
                        {review.employeeName || review.employeeCode}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {review.employeeCode}
                      </Typography>
                    </TableCell>
                    <TableCell>{review.departmentName ?? '—'}</TableCell>
                    <TableCell>{review.reviewPeriod}</TableCell>
                    <TableCell><RatingChip rating={review.rating} /></TableCell>
                    <TableCell>{review.reviewDate}</TableCell>
                    <TableCell>
                      {review.reviewerName ?? <Typography variant="caption" color="text.secondary">—</Typography>}
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="View details">
                        <IconButton size="small" onClick={() => setViewTarget(review)}>
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {canManage && (
                        <Tooltip title="Edit">
                          <IconButton size="small" onClick={() => openEdit(review)}>
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      {canDelete && (
                        <Tooltip title="Delete">
                          <IconButton size="small" color="error" onClick={() => setDeleteTarget(review)}>
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                ))}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={total}
          page={page}
          onPageChange={(_, p) => setPage(p)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => { setRows(parseInt(e.target.value, 10)); setPage(0); }}
          rowsPerPageOptions={[10, 20, 50]}
        />
      </TableContainer>

      {/* ── Create / Edit Dialog ──────────────────────────────────────── */}
      <Dialog open={formOpen} onClose={() => setFormOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editTarget ? 'Edit Review' : 'New Performance Review'}</DialogTitle>
        <DialogContent>
          {!editTarget && (
            <TextField
              fullWidth
              label="Employee ID (UUID)"
              name="employeeId"
              value={formData.employeeId}
              onChange={handleFormChange}
              error={!!formErrors.employeeId}
              helperText={formErrors.employeeId}
              margin="dense"
              placeholder="e.g. 3fa85f64-5717-4562-b3fc-2c963f66afa6"
            />
          )}
          <TextField
            fullWidth
            label="Review Period"
            name="reviewPeriod"
            value={formData.reviewPeriod}
            onChange={handleFormChange}
            error={!!formErrors.reviewPeriod}
            helperText={formErrors.reviewPeriod}
            margin="dense"
            placeholder="e.g. Q1 2025"
          />
          <FormControl fullWidth margin="dense" error={!!formErrors.rating}>
            <InputLabel>Rating</InputLabel>
            <Select
              name="rating"
              value={formData.rating}
              label="Rating"
              onChange={handleFormChange}
            >
              {[1, 2, 3, 4, 5].map((n) => (
                <MenuItem key={n} value={n}>
                  {n} — {RATING_LABELS[n]}
                </MenuItem>
              ))}
            </Select>
            {formErrors.rating && <FormHelperText>{formErrors.rating}</FormHelperText>}
          </FormControl>
          <TextField
            fullWidth
            label="Review Date"
            name="reviewDate"
            type="date"
            value={formData.reviewDate}
            onChange={handleFormChange}
            error={!!formErrors.reviewDate}
            helperText={formErrors.reviewDate}
            margin="dense"
            InputLabelProps={{ shrink: true }}
          />
          <TextField
            fullWidth
            label="Comments (optional)"
            name="comments"
            value={formData.comments}
            onChange={handleFormChange}
            multiline
            rows={3}
            margin="dense"
          />
          <TextField
            fullWidth
            label="Goals for next period (optional)"
            name="goals"
            value={formData.goals}
            onChange={handleFormChange}
            multiline
            rows={3}
            margin="dense"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFormOpen(false)}>Cancel</Button>
          <Button
            variant="contained"
            onClick={handleFormSubmit}
            disabled={onCreate.isPending || onUpdate.isPending}
          >
            {onCreate.isPending || onUpdate.isPending
              ? <CircularProgress size={20} />
              : editTarget ? 'Save Changes' : 'Create Review'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── View Dialog ────────────────────────────────────────────────── */}
      <Dialog open={!!viewTarget} onClose={() => setViewTarget(null)} maxWidth="sm" fullWidth>
        <DialogTitle>Review Detail</DialogTitle>
        {viewTarget && (
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              <Box>
                <Typography variant="caption" color="text.secondary">Employee</Typography>
                <Typography>{viewTarget.employeeName || viewTarget.employeeCode} ({viewTarget.departmentName})</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Period</Typography>
                <Typography>{viewTarget.reviewPeriod}</Typography>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Rating</Typography>
                <Box sx={{ mt: 0.5 }}><RatingChip rating={viewTarget.rating} /></Box>
              </Box>
              <Box>
                <Typography variant="caption" color="text.secondary">Review Date</Typography>
                <Typography>{viewTarget.reviewDate}</Typography>
              </Box>
              {viewTarget.reviewerName && (
                <Box>
                  <Typography variant="caption" color="text.secondary">Reviewer</Typography>
                  <Typography>{viewTarget.reviewerName}</Typography>
                </Box>
              )}
              {viewTarget.comments && (
                <>
                  <Divider />
                  <Box>
                    <Typography variant="caption" color="text.secondary">Comments</Typography>
                    <Typography sx={{ whiteSpace: 'pre-wrap' }}>{viewTarget.comments}</Typography>
                  </Box>
                </>
              )}
              {viewTarget.goals && (
                <Box>
                  <Typography variant="caption" color="text.secondary">Goals for Next Period</Typography>
                  <Typography sx={{ whiteSpace: 'pre-wrap' }}>{viewTarget.goals}</Typography>
                </Box>
              )}
            </Box>
          </DialogContent>
        )}
        <DialogActions>
          <Button onClick={() => setViewTarget(null)}>Close</Button>
        </DialogActions>
      </Dialog>

      {/* ── Confirm Delete Dialog ──────────────────────────────────────── */}
      <Dialog open={!!deleteTarget} onClose={() => setDeleteTarget(null)}>
        <DialogTitle>Delete Review?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            This will permanently delete the review for{' '}
            <strong>{deleteTarget?.employeeName || deleteTarget?.employeeCode}</strong>{' '}
            ({deleteTarget?.reviewPeriod}). This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDeleteTarget(null)}>Cancel</Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => onDelete.mutate(deleteTarget.id)}
            disabled={onDelete.isPending}
          >
            {onDelete.isPending ? <CircularProgress size={20} /> : 'Delete'}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── Snackbar ──────────────────────────────────────────────────── */}
      <Snackbar
        open={snack.open}
        autoHideDuration={4000}
        onClose={() => setSnack((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity={snack.severity} onClose={() => setSnack((s) => ({ ...s, open: false }))}>
          {snack.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
