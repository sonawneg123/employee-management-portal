/**
 * @fileoverview ProfilePage — authenticated user's employee profile.
 *
 * Phase 6F additions:
 *  - Profile photo upload (JPG/JPEG/PNG/WEBP, max 5 MB)
 *  - Replace existing photo
 *  - Delete photo (with confirmation)
 *  - Avatar uses uploaded photo when available
 *
 * Phase 6G additions:
 *  - Photo crop / position / zoom dialog before upload
 *  - Topbar avatar sync — updates immediately via AuthContext.updateUser
 *  - Cancel crop does NOT modify the existing photo
 *
 * Fetches via GET /profile and displays:
 * - Name, role badge, status, employee meta
 * - Editable personal info (phone, address) via PUT /profile/personal
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
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
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Divider,
  Grid,
  LinearProgress,
  Skeleton,
  Slider,
  Snackbar,
  TextField,
  Tooltip,
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
import CameraAltRoundedIcon from '@mui/icons-material/CameraAltRounded';
import DeleteOutlineRoundedIcon from '@mui/icons-material/DeleteOutlineRounded';
import ZoomInRoundedIcon from '@mui/icons-material/ZoomInRounded';
import ZoomOutRoundedIcon from '@mui/icons-material/ZoomOutRounded';
import CheckRoundedIcon from '@mui/icons-material/CheckRounded';

import {
  getProfile,
  updatePersonalInfo,
  uploadProfilePhoto,
  deleteProfilePhoto,
} from '@/services/profileApi';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';

// ── Helpers ───────────────────────────────────────────────────────────────────

const ALLOWED_PHOTO_TYPES = ['image/jpeg', 'image/png', 'image/webp'];
const MAX_PHOTO_BYTES = 5 * 1024 * 1024; // 5 MB
const CROP_CANVAS_SIZE = 300; // px — output square

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

/**
 * Renders the cropped image from the given canvas + parameters as a circular preview.
 *
 * @param {HTMLImageElement} img
 * @param {number} zoom - Scale factor (1 = fit)
 * @param {{ x: number, y: number }} offset - Offset in pixels (relative to centre)
 * @param {HTMLCanvasElement} canvas
 */
function drawCrop(img, zoom, offset, canvas) {
  const ctx = canvas.getContext('2d');
  const size = CROP_CANVAS_SIZE;
  canvas.width = size;
  canvas.height = size;
  ctx.clearRect(0, 0, size, size);

  // Draw circular clip
  ctx.save();
  ctx.beginPath();
  ctx.arc(size / 2, size / 2, size / 2, 0, Math.PI * 2);
  ctx.clip();

  // Scale and draw image centred at offset
  const scale = zoom * (size / Math.max(img.naturalWidth, img.naturalHeight));
  const scaledW = img.naturalWidth * scale;
  const scaledH = img.naturalHeight * scale;
  const drawX = (size - scaledW) / 2 + offset.x;
  const drawY = (size - scaledH) / 2 + offset.y;
  ctx.drawImage(img, drawX, drawY, scaledW, scaledH);
  ctx.restore();
}

/**
 * Converts a canvas to a Blob.
 *
 * @param {HTMLCanvasElement} canvas
 * @param {string} [mimeType='image/jpeg']
 * @returns {Promise<Blob>}
 */
function canvasToBlob(canvas, mimeType = 'image/jpeg') {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => (blob ? resolve(blob) : reject(new Error('Canvas toBlob failed'))),
      mimeType,
      0.92,
    );
  });
}

// ── Crop Dialog ───────────────────────────────────────────────────────────────

/**
 * Photo crop/position/zoom dialog.
 *
 * @param {{
 *   open: boolean,
 *   imageSrc: string,
 *   mimeType: string,
 *   onConfirm: (blob: Blob) => void,
 *   onCancel: () => void,
 * }} props
 */
function CropDialog({ open, imageSrc, mimeType, onConfirm, onCancel }) {
  const canvasRef = useRef(null);
  const imgRef = useRef(null);
  const isDragging = useRef(false);
  const lastPos = useRef({ x: 0, y: 0 });

  const [zoom, setZoom] = useState(1);
  const [offset, setOffset] = useState({ x: 0, y: 0 });
  const [imgLoaded, setImgLoaded] = useState(false);

  // Reset on open
  useEffect(() => {
    if (open) {
      setZoom(1);
      setOffset({ x: 0, y: 0 });
      setImgLoaded(false);
    }
  }, [open, imageSrc]);

  // Redraw on param change
  useEffect(() => {
    if (!imgLoaded || !canvasRef.current || !imgRef.current) return;
    drawCrop(imgRef.current, zoom, offset, canvasRef.current);
  }, [zoom, offset, imgLoaded]);

  const handleImgLoad = useCallback(() => {
    setImgLoaded(true);
    if (canvasRef.current && imgRef.current) {
      drawCrop(imgRef.current, 1, { x: 0, y: 0 }, canvasRef.current);
    }
  }, []);

  // Drag to position
  const handleMouseDown = useCallback((e) => {
    isDragging.current = true;
    lastPos.current = { x: e.clientX, y: e.clientY };
  }, []);

  const handleMouseMove = useCallback((e) => {
    if (!isDragging.current) return;
    const dx = e.clientX - lastPos.current.x;
    const dy = e.clientY - lastPos.current.y;
    lastPos.current = { x: e.clientX, y: e.clientY };
    setOffset((prev) => ({ x: prev.x + dx, y: prev.y + dy }));
  }, []);

  const handleMouseUp = useCallback(() => {
    isDragging.current = false;
  }, []);

  // Touch drag support
  const handleTouchStart = useCallback((e) => {
    const t = e.touches[0];
    isDragging.current = true;
    lastPos.current = { x: t.clientX, y: t.clientY };
  }, []);

  const handleTouchMove = useCallback((e) => {
    if (!isDragging.current) return;
    const t = e.touches[0];
    const dx = t.clientX - lastPos.current.x;
    const dy = t.clientY - lastPos.current.y;
    lastPos.current = { x: t.clientX, y: t.clientY };
    setOffset((prev) => ({ x: prev.x + dx, y: prev.y + dy }));
  }, []);

  const handleTouchEnd = useCallback(() => {
    isDragging.current = false;
  }, []);

  const handleConfirm = useCallback(async () => {
    if (!canvasRef.current) return;
    const blob = await canvasToBlob(
      canvasRef.current,
      mimeType === 'image/png' ? 'image/png' : 'image/jpeg',
    );
    onConfirm(blob);
  }, [mimeType, onConfirm]);

  return (
    <Dialog open={open} onClose={onCancel} maxWidth="sm" fullWidth>
      <DialogTitle>Crop &amp; Position Photo</DialogTitle>
      <DialogContent>
        <Typography variant="caption" color="text.secondary" display="block" sx={{ mb: 2 }}>
          Drag to reposition • Use the slider to zoom in/out • Preview below
        </Typography>
        {/* Hidden source image */}
        {imageSrc && (
          <img
            ref={imgRef}
            src={imageSrc}
            alt="Crop source"
            style={{ display: 'none' }}
            onLoad={handleImgLoad}
          />
        )}
        {/* Circular preview canvas */}
        <Box sx={{ display: 'flex', justifyContent: 'center', mb: 2 }}>
          <Box
            sx={{
              width: CROP_CANVAS_SIZE,
              height: CROP_CANVAS_SIZE,
              borderRadius: '50%',
              overflow: 'hidden',
              border: '3px solid',
              borderColor: 'primary.main',
              cursor: 'grab',
              userSelect: 'none',
              '&:active': { cursor: 'grabbing' },
            }}
            onMouseDown={handleMouseDown}
            onMouseMove={handleMouseMove}
            onMouseUp={handleMouseUp}
            onMouseLeave={handleMouseUp}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
          >
            <canvas
              ref={canvasRef}
              width={CROP_CANVAS_SIZE}
              height={CROP_CANVAS_SIZE}
              style={{ display: 'block' }}
            />
          </Box>
        </Box>
        {/* Zoom slider */}
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, px: 2 }}>
          <ZoomOutRoundedIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
          <Slider
            value={zoom}
            min={0.5}
            max={3}
            step={0.01}
            onChange={(_, v) => setZoom(v)}
            aria-label="Zoom"
            sx={{ flex: 1 }}
          />
          <ZoomInRoundedIcon sx={{ color: 'text.secondary', fontSize: 20 }} />
        </Box>
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ display: 'block', textAlign: 'center', mt: 0.5 }}
        >
          Zoom: {Math.round(zoom * 100)}%
        </Typography>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onCancel} variant="outlined">
          Cancel
        </Button>
        <Button
          onClick={handleConfirm}
          variant="contained"
          startIcon={<CheckRoundedIcon />}
          disabled={!imgLoaded}
        >
          Use This Photo
        </Button>
      </DialogActions>
    </Dialog>
  );
}

// ── Page ──────────────────────────────────────────────────────────────────────

/**
 * Profile page — view and edit own employee details, including profile photo.
 *
 * @returns {JSX.Element}
 */
export default function ProfilePage() {
  const { user, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const [editMode, setEditMode] = useState(false);
  const [snackbar, setSnackbar] = useState({ open: false, severity: 'success', message: '' });
  const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(null); // null | 0-100
  const [photoError, setPhotoError] = useState('');
  // Cache-bust counter so the <img> re-fetches after upload/delete
  const [photoCacheBust, setPhotoCacheBust] = useState(Date.now());
  const fileInputRef = useRef(null);

  // Crop dialog state
  const [cropOpen, setCropOpen] = useState(false);
  const [cropImageSrc, setCropImageSrc] = useState(null);
  const [cropMimeType, setCropMimeType] = useState('image/jpeg');

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

  // ── Save personal info mutation ────────────────────────────────────────────
  const saveMutation = useMutation({
    mutationFn: updatePersonalInfo,
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile', user?.userId], updatedProfile);
      updateUser({
        firstName: updatedProfile.firstName,
        lastName: updatedProfile.lastName,
      });
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setEditMode(false);
      showSnack('success', 'Profile updated successfully');
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

  // ── Profile photo upload ───────────────────────────────────────────────────
  const uploadMutation = useMutation({
    mutationFn: ({ file, onProgress }) => uploadProfilePhoto(file, onProgress),
    onSuccess: (updatedProfile) => {
      queryClient.setQueryData(['profile', user?.userId], updatedProfile);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setUploadProgress(null);
      const newCacheBust = Date.now();
      setPhotoCacheBust(newCacheBust);
      // Sync profile photo URL to AuthContext so Topbar updates immediately
      updateUser({ profilePhotoUrl: updatedProfile.profilePhotoUrl ?? '/api/profile/photo' });
      showSnack('success', 'Profile photo updated.');
    },
    onError: (err) => {
      setUploadProgress(null);
      const msg = err?.response?.data?.detail ?? err?.message ?? 'Failed to upload photo.';
      setPhotoError(msg);
      showSnack('error', msg);
    },
  });

  // ── Profile photo delete ───────────────────────────────────────────────────
  const deleteMutation = useMutation({
    mutationFn: deleteProfilePhoto,
    onSuccess: () => {
      const cleared = { ...(profile ?? {}), profilePhotoUrl: null };
      queryClient.setQueryData(['profile', user?.userId], cleared);
      queryClient.invalidateQueries({ queryKey: ['profile'] });
      setPhotoCacheBust(Date.now());
      setDeleteConfirmOpen(false);
      // Clear profile photo URL in AuthContext so Topbar updates immediately
      updateUser({ profilePhotoUrl: null });
      showSnack('success', 'Profile photo removed.');
    },
    onError: (err) => {
      setDeleteConfirmOpen(false);
      showSnack('error', err?.response?.data?.detail ?? 'Failed to remove photo.');
    },
  });

  // ── File selection — opens crop dialog ────────────────────────────────────
  const handleFileChange = (e) => {
    const file = e.target.files?.[0];
    if (!fileInputRef.current) return;
    // Reset input so same file can be re-selected after an error
    fileInputRef.current.value = '';
    if (!file) return;

    setPhotoError('');

    // Client-side validation (mirrors server)
    if (!ALLOWED_PHOTO_TYPES.includes(file.type)) {
      const msg = 'Unsupported file type. Please upload a JPG, PNG, or WEBP image.';
      setPhotoError(msg);
      showSnack('error', msg);
      return;
    }
    if (file.size > MAX_PHOTO_BYTES) {
      const msg = 'Image is too large. Maximum allowed size is 5 MB.';
      setPhotoError(msg);
      showSnack('error', msg);
      return;
    }

    // Open crop dialog with the selected file as a data URL
    const reader = new FileReader();
    reader.onload = (ev) => {
      setCropImageSrc(ev.target.result);
      setCropMimeType(file.type);
      setCropOpen(true);
    };
    reader.readAsDataURL(file);
  };

  // ── Crop dialog handlers ────────────────────────────────────────────────────
  const handleCropConfirm = useCallback(
    (blob) => {
      setCropOpen(false);
      setCropImageSrc(null);
      setUploadProgress(0);
      // Create a File from the blob for upload
      const filename = `profile_photo.${blob.type === 'image/png' ? 'png' : 'jpg'}`;
      const croppedFile = new File([blob], filename, { type: blob.type });
      uploadMutation.mutate({ file: croppedFile, onProgress: setUploadProgress });
    },
    [uploadMutation],
  );

  const handleCropCancel = useCallback(() => {
    setCropOpen(false);
    setCropImageSrc(null);
    // Do NOT modify the existing photo — cancel does nothing
  }, []);

  const initials = profile
    ? `${profile.firstName?.[0] ?? ''}${profile.lastName?.[0] ?? ''}`.toUpperCase()
    : user
      ? `${user.firstName?.[0] ?? ''}${user.lastName?.[0] ?? ''}`.toUpperCase()
      : '?';

  const roleLabel = getRoleLabel(user?.roles);
  const hasPhoto = Boolean(profile?.profilePhotoUrl);
  // Build authenticated photo URL (the JWT is sent by axiosInstance; for <img> tags
  // we use the absolute path — authentication handled by the cookie/session-free JWT
  // in the Authorization header is not applicable for plain <img> tags.
  // Instead we use the relative /api/profile/photo path — the browser will forward
  // the Authorization header only if set via fetch/XHR, not <img>. For simplicity
  // we display the photo using a blob-URL approach via a hidden fetch, OR
  // fall back to the avatar initials approach — which is the safest pattern for JWT-only apps.
  // We render the photo via an authenticated fetch → object URL.
  const [photoObjectUrl, setPhotoObjectUrl] = useState(null);

  useEffect(() => {
    // When the profile has a photo, fetch it with the current auth token and
    // create a blob object URL for the <img> tag.
    if (!hasPhoto) {
      if (photoObjectUrl) {
        URL.revokeObjectURL(photoObjectUrl);
        setPhotoObjectUrl(null);
      }
      return;
    }

    let cancelled = false;

    // Use axiosInstance which already has the Authorization header
    import('@/api/axiosInstance').then(({ default: axiosInstance }) => {
      axiosInstance
        .get('/profile/photo', { responseType: 'blob', params: { v: photoCacheBust } })
        .then((res) => {
          if (!cancelled) {
            const url = URL.createObjectURL(res.data);
            setPhotoObjectUrl((prev) => {
              if (prev) URL.revokeObjectURL(prev);
              return url;
            });
          }
        })
        .catch(() => {
          if (!cancelled) setPhotoObjectUrl(null);
        });
    });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasPhoto, photoCacheBust]);

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
              {/* Avatar + photo controls */}
              <Box sx={{ textAlign: 'center', mb: 3 }}>
                {isLoading ? (
                  <Skeleton variant="circular" width={88} height={88} sx={{ mx: 'auto', mb: 2 }} />
                ) : (
                  <Box sx={{ position: 'relative', display: 'inline-block', mb: 2 }}>
                    <Avatar
                      src={photoObjectUrl ?? undefined}
                      sx={{
                        width: 88,
                        height: 88,
                        background: photoObjectUrl
                          ? 'transparent'
                          : 'linear-gradient(135deg, #4F46E5, #7C3AED)',
                        fontSize: '2rem',
                        fontWeight: 800,
                      }}
                      aria-label="Profile photo"
                    >
                      {!photoObjectUrl && initials}
                    </Avatar>
                    {/* Upload overlay button */}
                    {!uploadMutation.isPending && (
                      <Tooltip title={hasPhoto ? 'Replace photo' : 'Upload photo'}>
                        <Box
                          component="label"
                          htmlFor="profile-photo-input"
                          sx={{
                            position: 'absolute',
                            bottom: 0,
                            right: 0,
                            width: 26,
                            height: 26,
                            borderRadius: '50%',
                            bgcolor: 'primary.main',
                            display: 'flex',
                            alignItems: 'center',
                            justifyContent: 'center',
                            cursor: 'pointer',
                            border: '2px solid',
                            borderColor: 'background.paper',
                            '&:hover': { bgcolor: 'primary.dark' },
                          }}
                          aria-label={hasPhoto ? 'Replace profile photo' : 'Upload profile photo'}
                        >
                          <CameraAltRoundedIcon sx={{ fontSize: 14, color: '#fff' }} />
                        </Box>
                      </Tooltip>
                    )}
                    {/* Hidden file input */}
                    <input
                      ref={fileInputRef}
                      id="profile-photo-input"
                      type="file"
                      accept="image/jpeg,image/png,image/webp"
                      style={{ display: 'none' }}
                      onChange={handleFileChange}
                      aria-label="Select profile photo file"
                    />
                  </Box>
                )}

                {/* Upload progress bar */}
                {uploadProgress !== null && (
                  <Box sx={{ width: '100%', mb: 1 }}>
                    <LinearProgress
                      variant="determinate"
                      value={uploadProgress}
                      sx={{ borderRadius: 2, height: 6 }}
                    />
                    <Typography variant="caption" color="text.secondary">
                      Uploading… {uploadProgress}%
                    </Typography>
                  </Box>
                )}

                {/* Photo error */}
                {photoError && (
                  <Alert severity="error" sx={{ mb: 1, textAlign: 'left', fontSize: '0.75rem' }}>
                    {photoError}
                  </Alert>
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

                {/* Photo action buttons */}
                {!isLoading && (
                  <Box sx={{ display: 'flex', gap: 1, justifyContent: 'center', mt: 1.5 }}>
                    <Button
                      size="small"
                      variant="outlined"
                      component="label"
                      htmlFor="profile-photo-input"
                      startIcon={<CameraAltRoundedIcon fontSize="small" />}
                      disabled={uploadMutation.isPending}
                      sx={{ fontSize: '0.72rem', py: 0.4, px: 1.2 }}
                    >
                      {hasPhoto ? 'Change Photo' : 'Upload Photo'}
                    </Button>
                    {hasPhoto && (
                      <Button
                        size="small"
                        variant="outlined"
                        color="error"
                        startIcon={<DeleteOutlineRoundedIcon fontSize="small" />}
                        onClick={() => setDeleteConfirmOpen(true)}
                        disabled={deleteMutation.isPending}
                        sx={{ fontSize: '0.72rem', py: 0.4, px: 1.2 }}
                      >
                        Remove
                      </Button>
                    )}
                  </Box>
                )}
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

      {/* ── Crop dialog ─────────────────────────────────────────────────────── */}
      <CropDialog
        open={cropOpen}
        imageSrc={cropImageSrc}
        mimeType={cropMimeType}
        onConfirm={handleCropConfirm}
        onCancel={handleCropCancel}
      />

      {/* ── Delete photo confirmation dialog ─────────────────────────────── */}
      <Dialog
        open={deleteConfirmOpen}
        onClose={() => setDeleteConfirmOpen(false)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle>Remove Profile Photo?</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Your profile photo will be permanently removed. The default initials avatar will be
            shown instead.
          </DialogContentText>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 2 }}>
          <Button onClick={() => setDeleteConfirmOpen(false)} variant="outlined">
            Cancel
          </Button>
          <Button
            onClick={() => deleteMutation.mutate()}
            variant="contained"
            color="error"
            disabled={deleteMutation.isPending}
            startIcon={
              deleteMutation.isPending ? <CircularProgress size={14} color="inherit" /> : null
            }
          >
            Remove
          </Button>
        </DialogActions>
      </Dialog>

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
