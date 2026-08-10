/**
 * SettingsPage.jsx
 * User account settings — change password.
 */
import React, { useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CircularProgress,
  Divider,
  IconButton,
  InputAdornment,
  Snackbar,
  TextField,
  Typography,
} from '@mui/material';
import {
  Lock as LockIcon,
  Visibility,
  VisibilityOff,
} from '@mui/icons-material';
import { changePassword } from '../../services/settingsApi';

const INITIAL = { currentPassword: '', newPassword: '', confirmPassword: '' };

export default function SettingsPage() {
  const [form, setForm]       = useState(INITIAL);
  const [errors, setErrors]   = useState({});
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);
  const [serverError, setServerError] = useState('');
  const [showPw, setShowPw]   = useState({
    current: false, new: false, confirm: false,
  });

  const toggle = (field) =>
    setShowPw((prev) => ({ ...prev, [field]: !prev[field] }));

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: '' }));
    setServerError('');
  };

  const validate = () => {
    const next = {};
    if (!form.currentPassword) next.currentPassword = 'Current password is required';
    if (!form.newPassword) next.newPassword = 'New password is required';
    else if (form.newPassword.length < 8)
      next.newPassword = 'New password must be at least 8 characters';
    if (!form.confirmPassword) next.confirmPassword = 'Please confirm your new password';
    else if (form.newPassword !== form.confirmPassword)
      next.confirmPassword = 'Passwords do not match';
    setErrors(next);
    return Object.keys(next).length === 0;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!validate()) return;
    setLoading(true);
    setServerError('');
    try {
      await changePassword(form);
      setSuccess(true);
      setForm(INITIAL);
    } catch (err) {
      const data = err.response?.data;
      if (data?.violations) {
        // Bean-validation field errors from backend
        const mapped = {};
        Object.entries(data.violations).forEach(([field, msg]) => {
          mapped[field] = msg;
        });
        setErrors(mapped);
      } else {
        // General error (wrong password, etc.)
        setServerError(data?.detail || data?.message || 'An error occurred. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={{ maxWidth: 560, mx: 'auto', mt: 4 }}>
      <Typography variant="h4" fontWeight={700} gutterBottom>
        Settings
      </Typography>

      {/* ── Change Password ─────────────────────────────────── */}
      <Card elevation={2} sx={{ mt: 2 }}>
        <CardContent>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 2 }}>
            <LockIcon color="primary" />
            <Typography variant="h6" fontWeight={600}>
              Change Password
            </Typography>
          </Box>
          <Divider sx={{ mb: 3 }} />

          {serverError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {serverError}
            </Alert>
          )}

          <Box component="form" onSubmit={handleSubmit} noValidate>
            <TextField
              fullWidth
              label="Current Password"
              name="currentPassword"
              type={showPw.current ? 'text' : 'password'}
              value={form.currentPassword}
              onChange={handleChange}
              error={!!errors.currentPassword}
              helperText={errors.currentPassword}
              margin="normal"
              autoComplete="current-password"
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => toggle('current')} edge="end">
                      {showPw.current ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              fullWidth
              label="New Password"
              name="newPassword"
              type={showPw.new ? 'text' : 'password'}
              value={form.newPassword}
              onChange={handleChange}
              error={!!errors.newPassword}
              helperText={errors.newPassword || 'Minimum 8 characters'}
              margin="normal"
              autoComplete="new-password"
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => toggle('new')} edge="end">
                      {showPw.new ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <TextField
              fullWidth
              label="Confirm New Password"
              name="confirmPassword"
              type={showPw.confirm ? 'text' : 'password'}
              value={form.confirmPassword}
              onChange={handleChange}
              error={!!errors.confirmPassword}
              helperText={errors.confirmPassword}
              margin="normal"
              autoComplete="new-password"
              InputProps={{
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton onClick={() => toggle('confirm')} edge="end">
                      {showPw.confirm ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              }}
            />

            <Box sx={{ mt: 3 }}>
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={loading}
                startIcon={loading ? <CircularProgress size={18} /> : <LockIcon />}
              >
                {loading ? 'Changing…' : 'Change Password'}
              </Button>
            </Box>
          </Box>
        </CardContent>
      </Card>

      {/* ── Notification Preferences ──────────────────────────
          NOTE: No notification_preferences column exists in the current schema.
          Email and push notification settings require additional DB migration
          and are intentionally excluded from this release.
      ─────────────────────────────────────────────────────── */}

      <Snackbar
        open={success}
        autoHideDuration={4000}
        onClose={() => setSuccess(false)}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        <Alert severity="success" onClose={() => setSuccess(false)}>
          Password changed successfully!
        </Alert>
      </Snackbar>
    </Box>
  );
}
