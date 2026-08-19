/**
 * ReviewsPage.jsx
 * Full performance review management.
 * Roles: ADMIN/HR/MANAGER can create, edit, delete.
 *         EMPLOYEE can only view their own reviews.
 */
import React, { useState } from 'react';
import {
  Alert,
  Autocomplete,
  Box,
  Button,
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
  alpha,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Star as StarIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import StarRoundedIcon from '@mui/icons-material/StarRounded';
import AssessmentRoundedIcon from '@mui/icons-material/AssessmentRounded';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { getReviews, createReview, updateReview, deleteReview } from '@/services/reviewApi';
import { getEmployees } from '@/services/employeeApi';
import { useAuth } from '@/contexts/AuthContext';

// ── Rating helpers ─────────────────────────────────────────────────────────

const RATING_LABELS = [
  '',
  'Unsatisfactory',
  'Needs Improvement',
  'Meets Expectations',
  'Good',
  'Outstanding',
];

/** Soft badge palette per rating */
const RATING_SOFT = [
  null,
  { bg: '#FEE2E2', color: '#991B1B' }, // 1 - Unsatisfactory
  { bg: '#FEF3C7', color: '#92400E' }, // 2 - Needs Improvement
  { bg: '#DBEAFE', color: '#1E40AF' }, // 3 - Meets Expectations
  { bg: '#D1FAE5', color: '#065F46' }, // 4 - Good
  { bg: '#EDE9FE', color: '#5B21B6' }, // 5 - Outstanding
];

function RatingChip({ rating }) {
  const palette = RATING_SOFT[rating] ?? { bg: '#F1F5F9', color: '#475569' };
  const label = RATING_LABELS[rating] ?? 'Unknown';

  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: '4px',
        px: '10px',
        py: '4px',
        borderRadius: '20px',
        bgcolor: palette.bg,
        color: palette.color,
        fontSize: '0.75rem',
        fontWeight: 600,
        lineHeight: 1.4,
        whiteSpace: 'nowrap',
      }}
    >
      <StarRoundedIcon sx={{ fontSize: '0.85rem' }} />
      {rating} — {label}
    </Box>
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
  const { hasAnyRole } = useAuth();
  const queryClient = useQueryClient();

  const canManage = hasAnyRole(['ROLE_ADMIN', 'ROLE_HR', 'ROLE_MANAGER']);
  const canDelete = hasAnyRole(['ROLE_ADMIN']);

  // ── Pagination / sort state ────────────────────────────────────────────
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRows] = useState(20);
  const [sortBy, setSortBy] = useState('reviewDate');
  const [sortDir, setSortDir] = useState('desc');

  // ── Dialog state ───────────────────────────────────────────────────────
  const [formOpen, setFormOpen] = useState(false);
  const [editTarget, setEditTarget] = useState(null);
  const [formData, setFormData] = useState(emptyForm());
  const [formErrors, setFormErrors] = useState({});
  const [viewTarget, setViewTarget] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);

  // ── Snackbar ───────────────────────────────────────────────────────────
  const [snack, setSnack] = useState({ open: false, message: '', severity: 'success' });
  const showSnack = (message, severity = 'success') => setSnack({ open: true, message, severity });

  // ── Query ──────────────────────────────────────────────────────────────
  const { data, isLoading, isError } = useQuery({
    queryKey: ['reviews', page, rowsPerPage, sortBy, sortDir],
    queryFn: () => getReviews({ page, size: rowsPerPage, sortBy, sortDir }),
    placeholderData: (prev) => prev,
  });

  const { data: employeeData } = useQuery({
    queryKey: ['employees', 'all-for-review'],
    queryFn: () => getEmployees({ size: 200, sortBy: 'employeeCode', sortDir: 'asc' }),
    enabled: canManage,
    staleTime: 5 * 60_000,
  });
  const employeeOptions = employeeData?.content ?? [];

  // ── Mutations ──────────────────────────────────────────────────────────
  const onCreate = useMutation({
    mutationFn: (payload) => createReview(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      setFormOpen(false);
      showSnack('Review created successfully.');
    },
    onError: (err) => handleMutationError(err),
  });

  const onUpdate = useMutation({
    mutationFn: ({ id, payload }) => updateReview(id, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      setFormOpen(false);
      showSnack('Review updated successfully.');
    },
    onError: (err) => handleMutationError(err),
  });

  const onDelete = useMutation({
    mutationFn: (id) => deleteReview(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['reviews'] });
      setDeleteTarget(null);
      showSnack('Review deleted.', 'info');
    },
    onError: () => showSnack('Failed to delete review.', 'error'),
  });

  const handleMutationError = (err) => {
    if (err?.violations) {
      setFormErrors(err.violations);
    } else {
      showSnack(err?.message || 'An error occurred.', 'error');
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
      const { employeeId: _eid, ...payload } = formData;
      onUpdate.mutate({
        id: editTarget.id,
        payload: { ...payload, rating: Number(payload.rating) },
      });
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
      {/* ── Page header ── */}
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'flex-start',
          mb: 4,
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
          <Box
            sx={{
              width: 48,
              height: 48,
              borderRadius: '12px',
              bgcolor: (t) => alpha(t.palette.primary.main, 0.1),
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              color: 'primary.main',
            }}
          >
            <AssessmentRoundedIcon />
          </Box>
          <Box>
            <Typography variant="h2" fontWeight={800} sx={{ letterSpacing: '-0.02em' }}>
              Performance Reviews ⭐
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Track and manage employee performance evaluations
            </Typography>
          </Box>
        </Box>
        {canManage && (
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={openCreate}
            sx={{ height: 44, borderRadius: '10px', px: 3, fontWeight: 600 }}
          >
            New Review
          </Button>
        )}
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 3, borderRadius: '10px' }}>
          Failed to load reviews. Please try again.
        </Alert>
      )}

      <Paper
        variant="outlined"
        sx={{ borderRadius: '12px', borderColor: 'divider', overflow: 'hidden' }}
      >
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow sx={{ bgcolor: 'background.default' }}>
                {['Employee', 'Department', null, 'Rating', null, 'Reviewer', null].map(
                  (label, idx) => {
                    if (idx === 2) {
                      return (
                        <TableCell
                          key="period"
                          sx={{
                            fontWeight: 700,
                            fontSize: '0.75rem',
                            letterSpacing: '0.05em',
                            color: 'text.secondary',
                            textTransform: 'uppercase',
                          }}
                        >
                          <TableSortLabel
                            active={sortBy === 'reviewPeriod'}
                            direction={sortBy === 'reviewPeriod' ? sortDir : 'desc'}
                            onClick={() => handleSort('reviewPeriod')}
                          >
                            Period
                          </TableSortLabel>
                        </TableCell>
                      );
                    }
                    if (idx === 4) {
                      return (
                        <TableCell
                          key="date"
                          sx={{
                            fontWeight: 700,
                            fontSize: '0.75rem',
                            letterSpacing: '0.05em',
                            color: 'text.secondary',
                            textTransform: 'uppercase',
                          }}
                        >
                          <TableSortLabel
                            active={sortBy === 'reviewDate'}
                            direction={sortBy === 'reviewDate' ? sortDir : 'desc'}
                            onClick={() => handleSort('reviewDate')}
                          >
                            Date
                          </TableSortLabel>
                        </TableCell>
                      );
                    }
                    if (idx === 6) {
                      return (
                        <TableCell
                          key="actions"
                          align="right"
                          sx={{
                            fontWeight: 700,
                            fontSize: '0.75rem',
                            letterSpacing: '0.05em',
                            color: 'text.secondary',
                            textTransform: 'uppercase',
                          }}
                        >
                          Actions
                        </TableCell>
                      );
                    }
                    return (
                      <TableCell
                        key={label}
                        sx={{
                          fontWeight: 700,
                          fontSize: '0.75rem',
                          letterSpacing: '0.05em',
                          color: 'text.secondary',
                          textTransform: 'uppercase',
                        }}
                      >
                        {label}
                      </TableCell>
                    );
                  },
                )}
              </TableRow>
            </TableHead>
            <TableBody>
              {isLoading ? (
                Array.from({ length: 5 }).map((_, i) => (
                  <TableRow key={i}>
                    {Array.from({ length: 7 }).map((__, j) => (
                      <TableCell key={j}>
                        <Skeleton sx={{ borderRadius: '6px' }} />
                      </TableCell>
                    ))}
                  </TableRow>
                ))
              ) : rows.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} align="center" sx={{ py: 8 }}>
                    <Box
                      sx={{
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        gap: 1.5,
                      }}
                    >
                      <Typography fontSize="2.5rem" role="img" aria-label="star">
                        ⭐
                      </Typography>
                      <Typography variant="h6" fontWeight={700} color="text.secondary">
                        No performance reviews yet
                      </Typography>
                      <Typography variant="body2" color="text.disabled">
                        {canManage
                          ? 'Create the first performance review to get started.'
                          : 'No performance reviews have been recorded yet.'}
                      </Typography>
                      {canManage && (
                        <Button
                          variant="contained"
                          startIcon={<AddIcon />}
                          onClick={openCreate}
                          sx={{ mt: 1, borderRadius: '8px', fontWeight: 600 }}
                        >
                          New Review
                        </Button>
                      )}
                    </Box>
                  </TableCell>
                </TableRow>
              ) : (
                rows.map((review) => (
                  <TableRow
                    key={review.id}
                    hover
                    sx={{
                      '&:hover': { bgcolor: (t) => alpha(t.palette.primary.main, 0.03) },
                      transition: 'background-color 150ms ease',
                    }}
                  >
                    <TableCell>
                      <Typography variant="body2" fontWeight={600}>
                        {review.employeeName || review.employeeCode}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {review.employeeCode}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {review.departmentName ?? '—'}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" fontWeight={500}>
                        {review.reviewPeriod}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <RatingChip rating={review.rating} />
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {review.reviewDate}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" color="text.secondary">
                        {review.reviewerName ?? '—'}
                      </Typography>
                    </TableCell>
                    <TableCell align="right">
                      <Tooltip title="View details">
                        <IconButton
                          size="small"
                          onClick={() => setViewTarget(review)}
                          sx={{ color: 'text.secondary' }}
                        >
                          <ViewIcon fontSize="small" />
                        </IconButton>
                      </Tooltip>
                      {canManage && (
                        <Tooltip title="Edit">
                          <IconButton
                            size="small"
                            onClick={() => openEdit(review)}
                            sx={{ color: 'primary.main' }}
                          >
                            <EditIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                      {canDelete && (
                        <Tooltip title="Delete">
                          <IconButton
                            size="small"
                            color="error"
                            onClick={() => setDeleteTarget(review)}
                          >
                            <DeleteIcon fontSize="small" />
                          </IconButton>
                        </Tooltip>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <TablePagination
          component="div"
          count={total}
          page={page}
          onPageChange={(_, p) => setPage(p)}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={(e) => {
            setRows(parseInt(e.target.value, 10));
            setPage(0);
          }}
          rowsPerPageOptions={[10, 20, 50]}
          sx={{ borderTop: '1px solid', borderColor: 'divider' }}
        />
      </Paper>

      {/* ── Create / Edit Dialog ──────────────────────────────────────── */}
      <Dialog
        open={formOpen}
        onClose={() => setFormOpen(false)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: '16px' } }}
      >
        <DialogTitle sx={{ fontWeight: 700, pb: 1, display: 'flex', alignItems: 'center', gap: 1 }}>
          {editTarget ? (
            <>
              <EditIcon sx={{ fontSize: 20, color: 'primary.main' }} /> Edit Review
            </>
          ) : (
            <>
              <StarRoundedIcon sx={{ fontSize: 20, color: '#F5C518' }} /> New Performance Review
            </>
          )}
        </DialogTitle>
        <DialogContent
          sx={{ pt: '16px !important', display: 'flex', flexDirection: 'column', gap: 1 }}
        >
          {!editTarget && (
            <Autocomplete
              options={employeeOptions}
              getOptionLabel={(opt) => {
                const name = [opt.firstName, opt.lastName].filter(Boolean).join(' ');
                return name
                  ? `${name} (${opt.employeeCode})`
                  : (opt.employeeCode ?? String(opt.id));
              }}
              isOptionEqualToValue={(opt, val) => opt.id === val.id}
              value={employeeOptions.find((e) => e.id === formData.employeeId) ?? null}
              onChange={(_e, emp) => {
                setFormData((p) => ({ ...p, employeeId: emp?.id ?? '' }));
                setFormErrors((p) => ({ ...p, employeeId: '' }));
              }}
              renderInput={(params) => (
                <TextField
                  {...params}
                  label="Employee"
                  required
                  margin="dense"
                  error={!!formErrors.employeeId}
                  helperText={
                    formErrors.employeeId ??
                    (employeeOptions.length === 0
                      ? 'No employee records yet — create employees first via HR.'
                      : '')
                  }
                  placeholder="Search by name or code…"
                  sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
                />
              )}
              noOptionsText="No employees found"
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
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
          />
          <FormControl fullWidth margin="dense" error={!!formErrors.rating}>
            <InputLabel>Rating</InputLabel>
            <Select
              name="rating"
              value={formData.rating}
              label="Rating"
              onChange={handleFormChange}
              sx={{ borderRadius: '10px' }}
            >
              {[1, 2, 3, 4, 5].map((n) => (
                <MenuItem key={n} value={n}>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <StarIcon sx={{ fontSize: '1rem', color: '#F59E0B' }} />
                    {n} — {RATING_LABELS[n]}
                  </Box>
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
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
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
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
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
            sx={{ '& .MuiOutlinedInput-root': { borderRadius: '10px' } }}
          />
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5, gap: 1 }}>
          <Button
            onClick={() => setFormOpen(false)}
            variant="outlined"
            sx={{ borderRadius: '8px', fontWeight: 600 }}
          >
            Cancel
          </Button>
          <Button
            variant="contained"
            onClick={handleFormSubmit}
            disabled={onCreate.isPending || onUpdate.isPending}
            sx={{ borderRadius: '8px', fontWeight: 600 }}
          >
            {onCreate.isPending || onUpdate.isPending ? (
              <CircularProgress size={20} />
            ) : editTarget ? (
              'Save Changes'
            ) : (
              'Create Review'
            )}
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── View Dialog ────────────────────────────────────────────────── */}
      <Dialog
        open={!!viewTarget}
        onClose={() => setViewTarget(null)}
        maxWidth="sm"
        fullWidth
        PaperProps={{ sx: { borderRadius: '16px' } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Review Details</DialogTitle>
        {viewTarget && (
          <DialogContent>
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
              <Box
                sx={{
                  p: 2,
                  borderRadius: '10px',
                  bgcolor: 'background.default',
                  border: '1px solid',
                  borderColor: 'divider',
                }}
              >
                <Typography
                  variant="caption"
                  color="text.secondary"
                  fontWeight={600}
                  sx={{ textTransform: 'uppercase', letterSpacing: '0.06em' }}
                >
                  Employee
                </Typography>
                <Typography fontWeight={600} sx={{ mt: 0.5 }}>
                  {viewTarget.employeeName || viewTarget.employeeCode}
                  {viewTarget.departmentName && (
                    <Typography
                      component="span"
                      color="text.secondary"
                      variant="body2"
                      sx={{ ml: 1 }}
                    >
                      · {viewTarget.departmentName}
                    </Typography>
                  )}
                </Typography>
              </Box>

              <Box sx={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 2 }}>
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={600}
                    sx={{ textTransform: 'uppercase', letterSpacing: '0.06em' }}
                  >
                    Period
                  </Typography>
                  <Typography fontWeight={500} sx={{ mt: 0.5 }}>
                    {viewTarget.reviewPeriod}
                  </Typography>
                </Box>
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={600}
                    sx={{ textTransform: 'uppercase', letterSpacing: '0.06em' }}
                  >
                    Review Date
                  </Typography>
                  <Typography fontWeight={500} sx={{ mt: 0.5 }}>
                    {viewTarget.reviewDate}
                  </Typography>
                </Box>
              </Box>

              <Box>
                <Typography
                  variant="caption"
                  color="text.secondary"
                  fontWeight={600}
                  sx={{ textTransform: 'uppercase', letterSpacing: '0.06em' }}
                >
                  Rating
                </Typography>
                <Box sx={{ mt: 0.5 }}>
                  <RatingChip rating={viewTarget.rating} />
                </Box>
              </Box>

              {viewTarget.reviewerName && (
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={600}
                    sx={{ textTransform: 'uppercase', letterSpacing: '0.06em' }}
                  >
                    Reviewer
                  </Typography>
                  <Typography sx={{ mt: 0.5 }}>{viewTarget.reviewerName}</Typography>
                </Box>
              )}

              {viewTarget.comments && (
                <>
                  <Divider />
                  <Box>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      fontWeight={600}
                      sx={{ textTransform: 'uppercase', letterSpacing: '0.06em' }}
                    >
                      Comments
                    </Typography>
                    <Typography sx={{ whiteSpace: 'pre-wrap', mt: 0.5, fontSize: '0.9rem' }}>
                      {viewTarget.comments}
                    </Typography>
                  </Box>
                </>
              )}

              {viewTarget.goals && (
                <Box>
                  <Typography
                    variant="caption"
                    color="text.secondary"
                    fontWeight={600}
                    sx={{ textTransform: 'uppercase', letterSpacing: '0.06em' }}
                  >
                    Goals for Next Period
                  </Typography>
                  <Typography sx={{ whiteSpace: 'pre-wrap', mt: 0.5, fontSize: '0.9rem' }}>
                    {viewTarget.goals}
                  </Typography>
                </Box>
              )}
            </Box>
          </DialogContent>
        )}
        <DialogActions sx={{ px: 3, pb: 2.5 }}>
          <Button
            onClick={() => setViewTarget(null)}
            variant="outlined"
            sx={{ borderRadius: '8px', fontWeight: 600 }}
          >
            Close
          </Button>
        </DialogActions>
      </Dialog>

      {/* ── Confirm Delete Dialog ──────────────────────────────────────── */}
      <Dialog
        open={!!deleteTarget}
        onClose={() => setDeleteTarget(null)}
        PaperProps={{ sx: { borderRadius: '16px' } }}
      >
        <DialogTitle sx={{ fontWeight: 700 }}>Delete Review?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            This will permanently delete the review for{' '}
            <strong>{deleteTarget?.employeeName || deleteTarget?.employeeCode}</strong> (
            {deleteTarget?.reviewPeriod}). This action cannot be undone.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2.5, gap: 1 }}>
          <Button
            onClick={() => setDeleteTarget(null)}
            variant="outlined"
            sx={{ borderRadius: '8px', fontWeight: 600 }}
          >
            Cancel
          </Button>
          <Button
            color="error"
            variant="contained"
            onClick={() => onDelete.mutate(deleteTarget.id)}
            disabled={onDelete.isPending}
            sx={{ borderRadius: '8px', fontWeight: 600 }}
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
        <Alert
          severity={snack.severity}
          onClose={() => setSnack((s) => ({ ...s, open: false }))}
          sx={{ borderRadius: '10px' }}
        >
          {snack.message}
        </Alert>
      </Snackbar>
    </Box>
  );
}
