/**
 * @fileoverview NotificationBell — notification bell icon with unread badge,
 * dropdown panel, and Web Audio tone for new notifications.
 *
 * Features:
 *  - Unread count badge on the bell icon
 *  - Dropdown panel listing recent notifications
 *  - Mark individual / all as read
 *  - Navigates to the relevant task on click (role-aware)
 *  - Sound alert for genuinely new notifications (not on initial load)
 *  - Mute toggle persisted in localStorage
 */

import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Badge,
  Box,
  Button,
  Chip,
  CircularProgress,
  ClickAwayListener,
  Divider,
  IconButton,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Paper,
  Popper,
  Tooltip,
  Typography,
} from '@mui/material';
import NotificationsRoundedIcon from '@mui/icons-material/NotificationsRounded';
import VolumeUpRoundedIcon from '@mui/icons-material/VolumeUpRounded';
import VolumeOffRoundedIcon from '@mui/icons-material/VolumeOffRounded';
import DoneAllRoundedIcon from '@mui/icons-material/DoneAllRounded';

import { ROUTES } from '@/constants/routes';
import { useAuth } from '@/contexts/AuthContext';
import { ROLES } from '@/constants/roles';
import {
  useUnreadCount,
  useNotifications,
  useMarkNotificationRead,
  useMarkAllNotificationsRead,
} from '@/hooks/useNotificationHooks';
import { useNotificationSound } from '@/hooks/useNotificationSound';

/**
 * Relative time formatter.
 *
 * @param {string} isoDate
 * @returns {string}
 */
function relativeTime(isoDate) {
  if (!isoDate) return '';
  const diff = Date.now() - new Date(isoDate).getTime();
  const minutes = Math.floor(diff / 60_000);
  if (minutes < 1) return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  return `${days}d ago`;
}

/**
 * @param {string} type NotificationType enum string
 * @returns {string} colour for the left border
 */
function notifColor(type) {
  if (type === 'TASK_ASSIGNED') return '#4F46E5';
  if (type === 'TASK_STARTED') return '#10B981';
  if (type === 'TASK_UPDATED') return '#F59E0B';
  if (type === 'TASK_REASSIGNED') return '#8B5CF6';
  if (type === 'TASK_DUE_SOON') return '#EF4444';
  if (type === 'TASK_OVERDUE') return '#DC2626';
  if (type === 'TASK_SUBMITTED') return '#0EA5E9';
  if (type === 'TASK_APPROVED') return '#10B981';
  if (type === 'TASK_CHANGES_REQUESTED') return '#F97316';
  if (type === 'TASK_COMMENT') return '#6366F1';
  if (type === 'LEAVE_APPROVED') return '#059669';
  if (type === 'LEAVE_REJECTED') return '#DC2626';
  if (type === 'ROLE_UPDATED') return '#7C3AED';
  if (type === 'AI_REVIEW_COMPLETED') return '#0EA5E9';
  if (type === 'AI_REVIEW_FAILED') return '#EF4444';
  return '#6B7280';
}

/**
 * NotificationBell component.
 *
 * @param {{ iconColor?: string }} props
 * @returns {JSX.Element}
 */
export default function NotificationBell({ iconColor = '#94A3B8' }) {
  const navigate = useNavigate();
  const { user, hasAnyRole } = useAuth();

  const [anchorEl, setAnchorEl] = useState(null);
  const open = Boolean(anchorEl);

  // Fetch unread count (polls every 15s)
  const { data: unreadData } = useUnreadCount({ enabled: Boolean(user) });
  const unreadCount = unreadData?.unreadCount ?? 0;

  // Only fetch notification list when panel is open
  const { data: notifData, isLoading: notifLoading } = useNotifications(
    { page: 0, size: 20 },
    {
      enabled: open && Boolean(user),
      refetchInterval: open ? 30_000 : false,
      refetchIntervalInBackground: false,
    },
  );
  const notifications = notifData?.content ?? [];

  const markRead = useMarkNotificationRead();
  const markAll = useMarkAllNotificationsRead();

  // Sound management
  const { muted, toggleMute, playSoundForType } = useNotificationSound();

  // Track the previous unread count to detect genuinely new notifications.
  // We skip the very first load (prevCount === null) to avoid playing on page load.
  // When there are new notifications, play the appropriate sound based on the newest type.
  const prevCountRef = useRef(null);
  useEffect(() => {
    if (prevCountRef.current === null) {
      prevCountRef.current = unreadCount;
      return;
    }
    if (unreadCount > prevCountRef.current) {
      // Find the newest notification type to play the right sound
      const latestType = notifications[0]?.type ?? null;
      playSoundForType(latestType);
    }
    prevCountRef.current = unreadCount;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [unreadCount, playSoundForType]);

  const handleOpen = useCallback((event) => {
    setAnchorEl((prev) => (prev ? null : event.currentTarget));
  }, []);

  const handleClose = useCallback(() => {
    setAnchorEl(null);
  }, []);

  /**
   * Determine navigation target based on role.
   *
   * @param {string} taskId
   * @returns {string}
   */
  const taskRoute = useCallback(
    (taskId) => {
      if (!taskId) return null;
      const isEmployee =
        hasAnyRole([ROLES.EMPLOYEE]) && !hasAnyRole([ROLES.HR, ROLES.MANAGER, ROLES.ADMIN]);
      return isEmployee ? ROUTES.EMPLOYEE_TASK_DETAIL(taskId) : ROUTES.MANAGER_TASK_DETAIL(taskId);
    },
    [hasAnyRole],
  );

  /**
   * Determine navigation target for a notification.
   * Task notifications → task detail.
   * Leave/role notifications → leaves page.
   *
   * @param {Object} notif
   * @returns {string|null}
   */
  const notifRoute = useCallback(
    (notif) => {
      if (notif.relatedTaskId) return taskRoute(notif.relatedTaskId);
      if (notif.type === 'LEAVE_APPROVED' || notif.type === 'LEAVE_REJECTED') {
        const isEmployee =
          hasAnyRole([ROLES.EMPLOYEE]) && !hasAnyRole([ROLES.HR, ROLES.MANAGER, ROLES.ADMIN]);
        return isEmployee ? ROUTES.MY_LEAVES : ROUTES.LEAVES;
      }
      if (notif.type === 'ROLE_UPDATED') {
        return ROUTES.PROFILE;
      }
      return null;
    },
    [taskRoute, hasAnyRole],
  );

  const handleNotificationClick = useCallback(
    async (notif) => {
      // Mark as read (fire-and-forget; errors are swallowed)
      if (!notif.read) {
        markRead.mutate(notif.id);
      }
      // Navigate if there is a relevant destination
      const route = notifRoute(notif);
      if (route) {
        handleClose();
        navigate(route);
      }
    },
    [markRead, notifRoute, handleClose, navigate],
  );

  const handleMarkAll = useCallback(() => {
    markAll.mutate();
  }, [markAll]);

  return (
    <>
      <Tooltip title={muted ? 'Notification sound: OFF' : 'Notification sound: ON'}>
        <IconButton
          size="small"
          onClick={toggleMute}
          sx={{ color: iconColor, opacity: 0.7, '&:hover': { opacity: 1 } }}
        >
          {muted ? (
            <VolumeOffRoundedIcon fontSize="small" />
          ) : (
            <VolumeUpRoundedIcon fontSize="small" />
          )}
        </IconButton>
      </Tooltip>

      <Tooltip title="Notifications">
        <IconButton
          onClick={handleOpen}
          size="small"
          sx={{ color: iconColor, '&:hover': { color: '#E2E8F0' } }}
        >
          <Badge
            badgeContent={unreadCount > 0 ? unreadCount : null}
            color="error"
            max={99}
            sx={{
              '& .MuiBadge-badge': {
                fontSize: '0.65rem',
                height: 16,
                minWidth: 16,
              },
            }}
          >
            <NotificationsRoundedIcon />
          </Badge>
        </IconButton>
      </Tooltip>

      <Popper
        open={open}
        anchorEl={anchorEl}
        placement="bottom-end"
        modifiers={[{ name: 'offset', options: { offset: [0, 8] } }]}
        style={{ zIndex: 1300 }}
      >
        <ClickAwayListener onClickAway={handleClose}>
          <Paper
            elevation={8}
            sx={{
              width: 360,
              maxHeight: 480,
              display: 'flex',
              flexDirection: 'column',
              borderRadius: 2,
              border: '1px solid',
              borderColor: 'divider',
              overflow: 'hidden',
            }}
          >
            {/* Header */}
            <Box
              sx={{
                px: 2,
                py: 1.5,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                borderBottom: '1px solid',
                borderColor: 'divider',
              }}
            >
              <Typography variant="subtitle1" fontWeight={600}>
                Notifications
                {unreadCount > 0 && (
                  <Chip
                    label={unreadCount}
                    size="small"
                    color="error"
                    sx={{ ml: 1, height: 18, fontSize: '0.68rem' }}
                  />
                )}
              </Typography>
              {unreadCount > 0 && (
                <Tooltip title="Mark all as read">
                  <IconButton size="small" onClick={handleMarkAll} disabled={markAll.isPending}>
                    <DoneAllRoundedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              )}
            </Box>

            {/* Notification list */}
            <Box sx={{ overflowY: 'auto', flex: 1 }}>
              {notifLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 3 }}>
                  <CircularProgress size={24} />
                </Box>
              ) : notifications.length === 0 ? (
                <Box sx={{ py: 4, textAlign: 'center' }}>
                  <Typography variant="body2" color="text.secondary">
                    No notifications yet.
                  </Typography>
                </Box>
              ) : (
                <List dense disablePadding>
                  {notifications.map((notif, idx) => (
                    <React.Fragment key={notif.id}>
                      {idx > 0 && <Divider component="li" />}
                      <ListItem disablePadding>
                        <ListItemButton
                          onClick={() => handleNotificationClick(notif)}
                          sx={{
                            borderLeft: `3px solid ${notifColor(notif.type)}`,
                            bgcolor: notif.read ? 'transparent' : 'action.hover',
                            '&:hover': { bgcolor: 'action.selected' },
                            alignItems: 'flex-start',
                            gap: 1,
                            py: 1,
                          }}
                        >
                          <ListItemText
                            primary={
                              <Typography
                                variant="body2"
                                fontWeight={notif.read ? 400 : 600}
                                noWrap
                              >
                                {notif.title}
                              </Typography>
                            }
                            secondary={
                              <>
                                <Typography
                                  component="span"
                                  variant="caption"
                                  display="block"
                                  sx={{
                                    whiteSpace: 'pre-line',
                                    color: 'text.secondary',
                                    lineHeight: 1.4,
                                  }}
                                >
                                  {notif.message}
                                </Typography>
                                <Typography
                                  component="span"
                                  variant="caption"
                                  color="text.disabled"
                                >
                                  {relativeTime(notif.createdAt)}
                                </Typography>
                              </>
                            }
                          />
                          {!notif.read && (
                            <Box
                              sx={{
                                width: 8,
                                height: 8,
                                borderRadius: '50%',
                                bgcolor: 'error.main',
                                mt: 0.5,
                                flexShrink: 0,
                              }}
                            />
                          )}
                        </ListItemButton>
                      </ListItem>
                    </React.Fragment>
                  ))}
                </List>
              )}
            </Box>

            {/* Footer */}
            {unreadCount > 0 && (
              <Box
                sx={{
                  px: 2,
                  py: 1,
                  borderTop: '1px solid',
                  borderColor: 'divider',
                  textAlign: 'center',
                }}
              >
                <Button
                  size="small"
                  onClick={handleMarkAll}
                  disabled={markAll.isPending}
                  startIcon={<DoneAllRoundedIcon />}
                >
                  Mark all as read
                </Button>
              </Box>
            )}
          </Paper>
        </ClickAwayListener>
      </Popper>
    </>
  );
}
