/**
 * @fileoverview CompanyPolicyList — table of existing knowledge documents.
 *
 * Displays documents returned by GET /api/ai/rag/documents.
 * Admin users see a Delete action; HR users see read-only rows.
 *
 * Columns: Title, Source Type, Status, Created, Actions (Admin only)
 */

import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  IconButton,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Tooltip,
  Typography,
} from '@mui/material';
import DeleteRoundedIcon from '@mui/icons-material/DeleteRounded';
import RefreshRoundedIcon from '@mui/icons-material/RefreshRounded';
import { deleteDocument } from '@/services/knowledgeApi';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';

/**
 * @param {{ status: string }} props
 * @returns {JSX.Element}
 */
function StatusChip({ status }) {
  const map = {
    ACTIVE: { color: 'success', label: 'Active' },
    PROCESSING: { color: 'warning', label: 'Processing' },
    ERROR: { color: 'error', label: 'Error' },
  };
  const { color, label } = map[status] ?? { color: 'default', label: status };
  return <Chip label={label} color={color} size="small" aria-label={`Document status: ${label}`} />;
}

/**
 * Table of knowledge documents with optional delete (Admin only).
 *
 * @param {{
 *   documents: import('@/services/knowledgeApi').KnowledgeDocumentResponse[],
 *   loading:   boolean,
 *   error:     string,
 *   onRefresh: () => void,
 * }} props
 * @returns {JSX.Element}
 */
export default function CompanyPolicyList({ documents, loading, error, onRefresh }) {
  const { hasAnyRole } = useAuth();
  const isAdmin = hasAnyRole([ROLES.ADMIN]);

  const [deletingId, setDeletingId] = useState(null);
  const [deleteError, setDeleteError] = useState('');

  const handleDelete = async (id, title) => {
    if (!window.confirm(`Delete "${title}"? This cannot be undone.`)) return;
    setDeletingId(id);
    setDeleteError('');
    try {
      await deleteDocument(id);
      onRefresh();
    } catch (err) {
      setDeleteError(err?.message ?? 'Failed to delete document.');
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 1.5 }}>
        <Typography variant="h6" fontWeight={600}>
          Existing Knowledge Documents
        </Typography>
        <Tooltip title="Refresh list">
          <span>
            <IconButton
              size="small"
              onClick={onRefresh}
              disabled={loading}
              aria-label="Refresh document list"
            >
              {loading ? <CircularProgress size={18} /> : <RefreshRoundedIcon fontSize="small" />}
            </IconButton>
          </span>
        </Tooltip>
      </Box>

      {deleteError && (
        <Alert severity="error" onClose={() => setDeleteError('')} sx={{ mb: 1 }}>
          {deleteError}
        </Alert>
      )}

      {error && (
        <Alert
          severity="error"
          action={
            <Button size="small" color="inherit" onClick={onRefresh}>
              Retry
            </Button>
          }
          sx={{ mb: 1 }}
        >
          {error}
        </Alert>
      )}

      {!loading && !error && documents.length === 0 && (
        <Typography variant="body2" color="text.secondary" sx={{ py: 3, textAlign: 'center' }}>
          No documents yet. Add your first company policy above.
        </Typography>
      )}

      {documents.length > 0 && (
        <TableContainer component={Paper} variant="outlined">
          <Table size="small" aria-label="Company policy documents table">
            <TableHead>
              <TableRow>
                <TableCell>
                  <strong>Title</strong>
                </TableCell>
                <TableCell>
                  <strong>Source Type</strong>
                </TableCell>
                <TableCell>
                  <strong>Status</strong>
                </TableCell>
                <TableCell>
                  <strong>Created</strong>
                </TableCell>
                {isAdmin && (
                  <TableCell align="right">
                    <strong>Actions</strong>
                  </TableCell>
                )}
              </TableRow>
            </TableHead>
            <TableBody>
              {documents.map((doc) => (
                <TableRow key={doc.id} hover>
                  <TableCell sx={{ maxWidth: 280 }}>
                    <Typography
                      variant="body2"
                      sx={{
                        fontWeight: 500,
                        overflow: 'hidden',
                        textOverflow: 'ellipsis',
                        whiteSpace: 'nowrap',
                      }}
                      title={doc.title}
                    >
                      {doc.title}
                    </Typography>
                    {doc.description && (
                      <Typography variant="caption" color="text.secondary">
                        {doc.description}
                      </Typography>
                    )}
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" sx={{ textTransform: 'capitalize' }}>
                      {(doc.sourceType ?? '').toLowerCase().replace(/_/g, ' ')}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <StatusChip status={doc.status} />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {doc.createdAt ? new Date(doc.createdAt).toLocaleDateString() : '—'}
                    </Typography>
                  </TableCell>
                  {isAdmin && (
                    <TableCell align="right">
                      <Tooltip title="Delete document">
                        <span>
                          <IconButton
                            size="small"
                            color="error"
                            disabled={deletingId === doc.id}
                            onClick={() => handleDelete(doc.id, doc.title)}
                            aria-label={`Delete ${doc.title}`}
                          >
                            {deletingId === doc.id ? (
                              <CircularProgress size={16} color="error" />
                            ) : (
                              <DeleteRoundedIcon fontSize="small" />
                            )}
                          </IconButton>
                        </span>
                      </Tooltip>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Box>
  );
}
