/**
 * @fileoverview CompanyPoliciesPage — knowledge-base management UI for Admin and HR.
 *
 * Allows Admin and HR users to:
 *  - View existing company policy documents
 *  - Add a new policy document (ingested via existing Phase 2A RAG pipeline)
 *
 * The submitted documents are processed by KnowledgeIngestionService on the backend
 * and automatically become available to the Phase 2B RAG-grounded AI Assistant.
 *
 * Route: /admin/policies  (ADMIN)
 *        /hr/policies     (HR)
 */

import React, { useState, useCallback } from 'react';
import { Helmet } from 'react-helmet-async';
import { Alert, Box, Divider, Typography } from '@mui/material';
import AutoStoriesRoundedIcon from '@mui/icons-material/AutoStoriesRounded';
import CompanyPolicyForm from '@/components/knowledge/CompanyPolicyForm';
import CompanyPolicyList from '@/components/knowledge/CompanyPolicyList';
import { listDocuments } from '@/services/knowledgeApi';

/**
 * Company Policies management page.
 *
 * @returns {JSX.Element}
 */
export default function CompanyPoliciesPage() {
  const [documents, setDocuments] = useState([]);
  const [listLoading, setListLoading] = useState(false);
  const [listError, setListError] = useState('');
  const [fetched, setFetched] = useState(false);

  // Fetch the document list on first render and on explicit refresh
  const fetchDocuments = useCallback(async () => {
    setListLoading(true);
    setListError('');
    try {
      const docs = await listDocuments();
      setDocuments(Array.isArray(docs) ? docs : []);
      setFetched(true);
    } catch (err) {
      setListError(err?.message ?? 'Failed to load documents. Please try again.');
    } finally {
      setListLoading(false);
    }
  }, []);

  // Trigger initial load once
  React.useEffect(() => {
    fetchDocuments();
  }, [fetchDocuments]);

  const handleSuccess = useCallback(() => {
    // Refresh the list after a successful ingestion
    fetchDocuments();
  }, [fetchDocuments]);

  return (
    <>
      <Helmet>
        <title>Company Policies — PeopleCore HR</title>
      </Helmet>

      <Box sx={{ maxWidth: 900, mx: 'auto', px: { xs: 1, sm: 2 }, py: 3 }}>
        {/* ── Page header ──────────────────────────────────────────────── */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5, mb: 2 }}>
          <AutoStoriesRoundedIcon sx={{ fontSize: 32, color: 'primary.main' }} />
          <Box>
            <Typography variant="h5" fontWeight={700}>
              Company Policies
            </Typography>
            <Typography variant="body2" color="text.secondary">
              Add and manage company policy documents for the AI Knowledge Base
            </Typography>
          </Box>
        </Box>

        {/* ── Info banner ───────────────────────────────────────────────── */}
        <Alert severity="info" sx={{ mb: 3 }}>
          Documents added here are processed by the RAG pipeline and become available to the{' '}
          <strong>AI Assistant</strong>. The assistant will use them to answer employee questions
          about company policies.
        </Alert>

        {/* ── Add form ──────────────────────────────────────────────────── */}
        <CompanyPolicyForm onSuccess={handleSuccess} />

        <Divider sx={{ my: 4 }} />

        {/* ── Document list ─────────────────────────────────────────────── */}
        {fetched && (
          <CompanyPolicyList
            documents={documents}
            loading={listLoading}
            error={listError}
            onRefresh={fetchDocuments}
          />
        )}
      </Box>
    </>
  );
}
