/**
 * @fileoverview ProfilePage — authenticated user's employee profile (redesigned).
 *
 * Fetches via GET /profile and displays:
 * - Name, role badge, status, employee meta
 * - Editable personal info (phone, address) via PUT /profile/personal
 *
 * Layout: premium profile header card + information sections.
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
import PersonRoundedIcon from '@mui/icons-material/PersonRounded';
import EditRoundedIcon from '@mui/icons-material/EditRounded';
import SaveRoundedIcon from '@mui/icons-material/SaveRounded';
import CancelRoundedIcon from '@mui/icons-material/CancelRounded';
import BadgeRoundedIcon from '@mui/icons-material/BadgeRounded';
import ApartmentRoundedIcon from '@mui/icons-material/ApartmentRounded';
import WorkRoundedIcon from '@mui/icons-material/WorkRounded';
import CalendarTodayRoundedIcon from '@mui/icons-material/CalendarTodayRounded';
import PhoneRoundedIcon from '@mui/icons-material/PhoneRounded';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import EmailRoundedIcon from '@mui/icons-material/EmailRounded';

import { getProfile, updatePersonalInfo } from '@/services/profileApi';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * @param {string[]} roles
 * @returns {string}
 */
function getRoleLabel(roles) {
  if (!roles?.length) return 'Employee';
  if (roles.includes(ROLES.ADMIN)) return 'Administrator';
  if (roles.includes(ROLES.HR)) return 'HR Manager';
  if (roles.includes(ROLES.MANAGER)) return 'Manager';
  return 'Employee';
}

/**
 * Single labelled info row with icon.
 *
 * @param {{ Icon: React.ElementType, label: string, value: string|null, loading: boolean }} props
 */
function InfoRow({ Icon, label, value, loading }) {
  return (
    <Box sx={{ display: 'flex', alignItems: 'flex-start', gap: 1.5, py: 1.25 }}>
      <Box
        sx={{
          width: 32,
          height: 32,
          borderRadius: '8px',
          bgcolor: 'rgba(79,70,229,0.08)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          flexShrink: 0,
          mt: 0.1,
        }}
      >
        <Icon sx={{ fontSize: 16, color: 'primary.main' }} />
      </Box>
      <Box sx={{ minWidth: 0 }}>
        <Typography variant="caption" color="text.secondary" display="block" fontWeight={500}>
          {label}
        </Typography>
        {loading ? (
          <Skeleton variant="text" width={160} height={20} />
        ) : (
          <Typography variant="body2" fontWeight={600} sx={{ wordBreak: 'break-word' }}>
            {value || '—'}
          </Typography>
        )}
      </Box>
    </Box>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

/**
 * Profile page — view and edit own employee details.
 *
 * @returns {JSX.Element}
 */
export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const [editMode, setEditMode] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });

  const showSnack = (severity, message) => setSnackbar({ open: true, severity, message });

  // ── Fetch profile ──────────────────────────────────────────────────────────
  const {
    data: profile,
    isLoading,
    isError,
    error,
  } = useQuery({
    queryKey: ['profile', user?.userId],
    queryFn: getProfile,
    enabled: Boolean(user?.userId),
    staleTime: 5 * 60_000,
  });

  // ── Edit form ──────────────────────────────────────────────────────────────
  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm({
    defaultValues: { firstName: '', lastName: '', phone: '', address: '' },
  });

  useEffect(() => {
    if (profile) {
      reset({
        firstName: profile.firstName ?? '',
        lastName: profile.lastName ?? '',
        phone: profile.phone ?? '',
        address: profile.address ?? '',
      });
    }
  }, [profile, reset]);

  // ── Save mutation ──────────────────────────────────────────────────────────
  const saveMutation = useMutation({
    mutationFn: updatePersonalInfo,
    onSuccess: (updatedProfile) => {
      // 1. Immediately write the fresh data into the React Query cache so the
      //    profile card re-renders without waiting for a background refetch.
      queryClient.setQueryData(['profile', user?.userId], updatedProfile);

      // 2. Sync firstName/lastName back into AuthContext so topbar/sidebar
      //    reflect the new name immediately without a re-login.
      updateUser({
        firstName: updatedProfile.firstName,
        lastName: updatedProfile.lastName,
      });

      // 3. Schedule a background refetch so any other observers (e.g. LeavesPage
      //    profile query) also see the fresh data.
      queryClient.invalidateQueries({ queryKey: ['profile'] });

      setEditMode(false);
      showSnack('success', 'Profile updated successfully 🎉');
    },
    onError: (err) => showSnack('error', err?.message ?? 'Failed to update profile.'),
  });

  const handleSave = handleSubmit((values) => saveMutation.mutate(values));

  const handleCancelEdit = () => {
    reset({
      firstName: profile?.firstName ?? '',
      lastName: profile?.lastName ?? '',
      phone: profile?.phone ?? '',
      address: profile?.address ?? '',
    });
    setEditMode(false);
  };

  const initials = profile
    ? `${profile.firstName?.[0] ?? ''}${profile.lastName?.[0] ?? ''}`.toUpperCase()
    : user
      ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
      : '?';

  const roleLabel = getRoleLabel(user?.roles);

  return (
    <>
      <Helmet>
        <title>My Profile — PeopleCore HR</title>
      </Helmet>

      <Box sx={{ mb: 3, display: 'flex', alignItems: 'center', gap: 2 }}>
        <Box
          sx={{
            width: 44,
            height: 44,
            borderRadius: '12px',
            bgcolor: 'rgba(79,70,229,0.1)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flexShrink: 0,
          }}
        >
          <PersonRoundedIcon sx={{ fontSize: 22, color: 'primary.main' }} />
        </Box>
        <Box>
          <Typography
            variant="h2"
            fontWeight={800}
            sx={{ letterSpacing: '-0.02em', mb: 0.25, lineHeight: 1.2 }}
          >
            My Profile
          </Typography>
          <Typography variant="body2" color="text.secondary">
            View and manage your employee information
          </Typography>
        </Box>
      </Box>

      {isError && (
        <Alert severity="error" sx={{ mb: 3 }}>
          {error?.message ?? 'Failed to load profile. Please try again.'}
        </Alert>
      )}

      <Grid container spacing={3}>
        {/* ── Left: avatar + identity ─────────────────────────────────── */}
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              {/* Avatar + name block */}
              <Box sx={{ textAlign: 'center', mb: 3 }}>
                {isLoading ? (
                  <Skeleton variant="circular" width={88} height={88} sx={{ mx: 'auto', mb: 2 }} />
                ) : (
                  <Avatar
                    sx={{
                      width: 88,
                      height: 88,
                      background: 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                      fontSize: '2rem',
                      fontWeight: 800,
                      mx: 'auto',
                      mb: 2,
                    }}
                  >
                    {initials}
                  </Avatar>
                )}

                {isLoading ? (
                  <>
                    <Skeleton variant="text" width="70%" height={28} sx={{ mx: 'auto' }} />
                    <Skeleton variant="text" width="90%" height={20} sx={{ mx: 'auto' }} />
                  </>
                ) : (
                  <>
                    <Typography variant="h5" fontWeight={800} gutterBottom>
                      {profile?.firstName} {profile?.lastName}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" gutterBottom>
                      {profile?.email ?? user?.email}
                    </Typography>
                  </>
                )}

                <Box
                  sx={{
                    display: 'flex',
                    gap: 1,
                    justifyContent: 'center',
                    flexWrap: 'wrap',
                    mt: 1,
                  }}
                >
                  {!isLoading && (
                    <>
                      <Chip
                        label={roleLabel}
                        size="small"
                        sx={{
                          fontWeight: 700,
                          bgcolor: 'rgba(79,70,229,0.1)',
                          color: 'primary.main',
                          border: '1px solid rgba(79,70,229,0.2)',
                        }}
                      />
                      <Chip
                        label={profile?.status ?? 'ACTIVE'}
                        size="small"
                        sx={{
                          fontWeight: 700,
                          bgcolor:
                            profile?.status === 'ACTIVE'
                              ? 'rgba(16,185,129,0.1)'
                              : 'rgba(245,158,11,0.1)',
                          color: profile?.status === 'ACTIVE' ? 'success.main' : 'warning.main',
                          border:
                            profile?.status === 'ACTIVE'
                              ? '1px solid rgba(16,185,129,0.2)'
                              : '1px solid rgba(245,158,11,0.2)',
                        }}
                      />
                    </>
                  )}
                </Box>
              </Box>

              <Divider sx={{ mb: 2 }} />

              {/* Identity details */}
              <InfoRow
                Icon={BadgeRoundedIcon}
                label="Employee ID"
                value={profile?.employeeCode}
                loading={isLoading}
              />
              <InfoRow
                Icon={WorkRoundedIcon}
                label="Job Title"
                value={profile?.jobTitle}
                loading={isLoading}
              />
              <InfoRow
                Icon={ApartmentRoundedIcon}
                label="Department"
                value={profile?.departmentName}
                loading={isLoading}
              />
              <InfoRow
                Icon={CalendarTodayRoundedIcon}
                label="Joined"
                value={profile?.dateOfJoining}
                loading={isLoading}
              />
              <InfoRow
                Icon={EmailRoundedIcon}
                label="Email"
                value={profile?.email ?? user?.email}
                loading={isLoading}
              />
            </CardContent>
          </Card>
        </Grid>

        {/* ── Right: editable personal info ───────────────────────────── */}
        <Grid size={{ xs: 12, md: 8 }}>
          <Card>
            <CardContent sx={{ p: 3 }}>
              <Box
                sx={{
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  mb: 3,
                }}
              >
                <Box>
                  <Typography variant="h5" fontWeight={700}>
                    Personal Information
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    {editMode
                      ? 'Edit your contact details below.'
                      : 'Your phone and address details.'}
                  </Typography>
                </Box>
                {!editMode && !isLoading && (
                  <Button
                    variant="outlined"
                    size="small"
                    startIcon={<EditRoundedIcon />}
                    onClick={() => setEditMode(true)}
                    sx={{ fontWeight: 600 }}
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
                        label="First Name"
                        fullWidth
                        size="small"
                        {...register('firstName', {
                          maxLength: { value: 100, message: 'Max 100 chars' },
                        })}
                        error={Boolean(errors.firstName)}
                        helperText={errors.firstName?.message}
                        inputProps={{ 'aria-label': 'First name' }}
                      />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                      <TextField
                        label="Last Name"
                        fullWidth
                        size="small"
                        {...register('lastName', {
                          maxLength: { value: 100, message: 'Max 100 chars' },
                        })}
                        error={Boolean(errors.lastName)}
                        helperText={errors.lastName?.message}
                        inputProps={{ 'aria-label': 'Last name' }}
                      />
                    </Grid>
                    <Grid size={{ xs: 12, sm: 6 }}>
                      <TextField
                        label="Phone"
                        fullWidth
                        size="small"
                        {...register('phone', {
                          maxLength: { value: 20, message: 'Max 20 chars' },
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
                          maxLength: { value: 255, message: 'Max 255 chars' },
                        })}
                        error={Boolean(errors.address)}
                        helperText={errors.address?.message}
                        inputProps={{ 'aria-label': 'Address' }}
                      />
                    </Grid>
                    <Grid size={{ xs: 12 }}>
                      <Box sx={{ display: 'flex', gap: 1.5 }}>
                        <Button
                          type="submit"
                          variant="contained"
                          startIcon={<SaveRoundedIcon />}
                          size="small"
                          disabled={saveMutation.isPending}
                          sx={{ fontWeight: 600 }}
                        >
                          {saveMutation.isPending ? 'Saving…' : 'Save Changes'}
                        </Button>
                        <Button
                          variant="outlined"
                          startIcon={<CancelRoundedIcon />}
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
                  <InfoRow
                    Icon={PhoneRoundedIcon}
                    label="Phone"
                    value={profile?.phone}
                    loading={isLoading}
                  />
                  <InfoRow
                    Icon={HomeRoundedIcon}
                    label="Address"
                    value={profile?.address}
                    loading={isLoading}
                  />
                  {!isLoading && !profile?.phone && !profile?.address && (
                    <Box
                      sx={{
                        py: 4,
                        textAlign: 'center',
                        color: 'text.disabled',
                        border: '1.5px dashed',
                        borderColor: 'divider',
                        borderRadius: '12px',
                        mt: 1,
                      }}
                    >
                      <Typography variant="body2">No contact details yet.</Typography>
                      <Button
                        size="small"
                        startIcon={<EditRoundedIcon />}
                        onClick={() => setEditMode(true)}
                        sx={{ mt: 1 }}
                      >
                        Add contact info
                      </Button>
                    </Box>
                  )}
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
