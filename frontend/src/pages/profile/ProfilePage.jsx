/**
 * @fileoverview ProfilePage — authenticated user's employee profile.
 *
 * Fetches the current user's profile via GET /profile and displays:
 * - Name, email, department, job title, employee code, joining date, status
 * - Editable personal info (phone, address) via PUT /profile/personal
 *
 * Uses react-query for data fetching and react-hook-form for the edit form.
 */

import React, { useEffect, useState } from 'react';
import { Helmet } from 'react-helmet-async';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useForm } from 'react-hook-form';
import {
  Alert,
  Avatar,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Divider,
  Grid,
  Skeleton,
  Snackbar,
  TextField,
  Typography,
} from '@mui/material';
import EditIcon       from '@mui/icons-material/Edit';
import SaveIcon       from '@mui/icons-material/Save';
import CancelIcon     from '@mui/icons-material/Cancel';
import PersonIcon     from '@mui/icons-material/Person';
import ApartmentIcon  from '@mui/icons-material/Apartment';
import WorkIcon       from '@mui/icons-material/Work';
import CalendarTodayIcon from '@mui/icons-material/CalendarToday';
import PhoneIcon      from '@mui/icons-material/Phone';
import HomeIcon       from '@mui/icons-material/Home';
import EmailIcon      from '@mui/icons-material/Email';

import { getProfile, updatePersonalInfo } from '@/services/profileApi';
import { useAuth } from '@/contexts/AuthContext';

// ── Info row helper ──────────────────────────────────────────────────────────

/**
 * Renders a labelled info row with an icon.
 *
 * @param {{ Icon: React.ElementType, label: string, value: string|null, loading: boolean }} props
 * @returns {JSX.Element}
 */
function InfoRow({ Icon, label, value, loading }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, py: 1 }}>
      <Icon sx={{ fontSize: 20, color: 'text.secondary', mt: 0.25, flexShrink: 0 }} />
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="caption" color="text.secondary" display="block">
          {label}
        </Typography>
        {loading ? (
          <Skeleton variant="text" width={160} />
        ) : (
          <Typography variant="body2" fontWeight={500}>
            {value ?? '—'}
          </Typography>
        )}
      </Box>
    </Box>
  );
}

// ── Page component ────────────────────────────────────────────────────────────

/**
 * Profile page — view and edit own employee details.
 *
 * @returns {JSX.Element}
 */
export default function ProfilePage() {
  const { user } = useAuth();
  const queryClient = useQueryClient();
  const [editMode, setEditMode] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  const showSnack = (severity, message) => setSnackbar({ open: true, severity, message });

  // ── Fetch profile ────────────────────────────────────────────────────────
  const {
    data:      profile,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ['profile', user?.userId],
    queryFn:  getProfile,
    enabled:  Boolean(user?.userId),
    staleTime: 5 * 60_000,
  });

  // ── Edit form ────────────────────────────────────────────────────────────
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: { phone: '', address: '' },
  });

  // Populate form when profile loads or edit mode opens
  useEffect(() => {
    if (profile) {
      reset({ phone: profile.phone ?? '', address: profile.address ?? '' });
    }
  }, [profile, reset]);

  // ── Save mutation ────────────────────────────────────────────────────────
  const saveMutation = useMutation({
    mutationFn: updatePersonalInfo,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile', user?.userId] });
      setEditMode(false);
      showSnack('success', 'Profile updated successfully.');
    },
    onError: (err) => {
      showSnack('error', err?.message ?? 'Failed to update profile.');
    },
  });

  const handleSave = handleSubmit((values) => {
    saveMutation.mutate(values);
  });

  const handleCancelEdit = () => {
    reset({ phone: profile?.phone ?? '', address: profile?.address ?? '' });
    setEditMode(false);
  };

  // ── Render helpers ───────────────────────────────────────────────────────
  const initials = profile
    ? `${profile.firstName?.[0] ?? ''}${profile.lastName?.[0] ?? ''}`.toUpperCase()
    : user
      ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
      : '?';

  return (
    <>
      <Helmet><title>My Profile — Employee Portal</title></Helmet>

      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" fontWeight={700}>My Profile</Typography>
        <Typography variant="body2" color="text.secondary">
          View and manage your employee information
        </Typography>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error?.message ?? 'Failed to load profile. Please try again.'}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* ── Left column: avatar + identity ─────────────────────────── */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent sx={{ p: 3, textAlign: 'center' }}>
              {isLoading ? (
                <Skeleton variant="circular" width={96} height={96} sx={{ mx: 'auto', mb: 2 }} />
              ) : (
                <Avatar
                  sx={{
                    width: 96,
                    height: 96,
                    bgcolor: 'primary.main',
                    fontSize: '2rem',
                    mx: 'auto',
                    mb: 2,
                  }}
                >
                  {initials}
                </Avatar>
              )}

              {isLoading ? (
                <>
                  <Skeleton variant="text" width="60%" sx={{ mx: 'auto' }} height={32} />
                  <Skeleton variant="text" width="80%" sx={{ mx: 'auto' }} />
                </>
              ) : (
                <>
                  <Typography variant="h6" fontWeight={700} gutterBottom>
                    {profile?.firstName} {profile?.lastName}
                  </Typography>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    {profile?.email ?? user?.email}
                  </Typography>
                  <Chip
                    label={profile?.status ?? 'ACTIVE'}
                    color={profile?.status === 'ACTIVE' ? 'success' : 'default'}
                    size="small"
                    sx={{ fontWeight: 600, mt: 0.5 }}
                  />
                </>
              )}

              <Divider sx={{ my: 2 }} />

              <Box sx={{ textAlign: 'left' }}>
                <InfoRow Icon={PersonIcon}       label="Employee ID"  value={profile?.employeeCode} loading={isLoading} />
                <InfoRow Icon={WorkIcon}         label="Job Title"    value={profile?.jobTitle}      loading={isLoading} />
                <InfoRow Icon={ApartmentIcon}    label="Department"   value={profile?.departmentName} loading={isLoading} />
                <InfoRow Icon={CalendarTodayIcon} label="Joined"      value={profile?.joiningDate}   loading={isLoading} />
                <InfoRow Icon={EmailIcon}        label="Email"        value={profile?.email ?? user?.email} loading={isLoading} />
              </Box>
            </CardContent>
          </Card>
        </Grid>

        {/* ── Right column: editable personal info ───────────────────── */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
                <Typography variant="h6" fontWeight={600}>
                  Personal Information
                </Typography>
                {!editMode && !isLoading && (
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<EditIcon />}
                    onClick={() => setEditMode(true)}
                  >
                    Edit
                  </Button>
                )}
              </Box>

              {editMode ? (
                <Box component="form" onSubmit={handleSave}>
                  <Grid container spacing={2}>
                    <Grid size={{ xs: 12, sm: 6 }}>
                      <TextField
                        label="Phone"
                        fullWidth
                        size="small"
                        {...register('phone', {
                          maxLength: { value: 20, message: 'Phone must be at most 20 characters' },
                        })}
                        error={Boolean(errors.phone)}
                        helperText={errors.phone?.message}
                        inputProps={{ 'aria-label': 'Phone number' }}
                      />
                    </Grid>
                    <Grid size={{ xs: 12 }}>
                      <TextField
                        label="Address"
                        fullWidth
                        multiline
                        rows={3}
                        size="small"
                        {...register('address', {
                          maxLength: { value: 255, message: 'Address must be at most 255 characters' },
                        })}
                        error={Boolean(errors.address)}
                        helperText={errors.address?.message}
                        inputProps={{ 'aria-label': 'Address' }}
                      />
                    </Grid>
                    <Grid size={{ xs: 12 }}>
                      <Box sx={{ display: 'flex', gap: 1 }}>
                        <Button
                          type="submit"
                          variant="contained"
                          startIcon={<SaveIcon />}
                          size="small"
                          disabled={saveMutation.isPending}
                        >
                          {saveMutation.isPending ? 'Saving…' : 'Save Changes'}
                        </Button>
                        <Button
                          variant="outlined"
                          startIcon={<CancelIcon />}
                          size="small"
                          onClick={handleCancelEdit}
                          disabled={saveMutation.isPending}
                        >
                          Cancel
                        </Button>
                      </Box>
                    </Grid>
                  </Grid>
                </Box>
              ) : (
                <Box>
                  <InfoRow Icon={PhoneIcon} label="Phone"   value={profile?.phone}   loading={isLoading} />
                  <InfoRow Icon={HomeIcon}  label="Address" value={profile?.address} loading={isLoading} />
                </Box>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Snackbar
        open={snackbar.open}
        autoHideDuration={4000}
        onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert
          onClose={() => setSnackbar((s) => ({ ...s, open: false }))}
          severity={snackbar.severity}
          variant="filled"
          sx={{ width: '100%' }}
        >
          {snackbar.message}
        </Alert>
      </Snackbar>
    </>
  );
}
