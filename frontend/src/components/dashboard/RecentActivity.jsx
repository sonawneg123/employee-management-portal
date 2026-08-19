/**
 * @fileoverview RecentActivity — premium time-ordered list of recent portal events.
 *
 * Fetches data from {@link useDashboardActivity} and renders each event as
 * a timeline-style list item with icon, description, and relative timestamp.
 * Premium SaaS design — navy + gold accent.
 */

import React from 'react';
import {
  Avatar,
  Box,
  List,
  ListItem,
  ListItemAvatar,
  ListItemText,
  Skeleton,
  Typography,
  useTheme,
} from '@mui/material';
import AccessTimeRoundedIcon from '@mui/icons-material/AccessTimeRounded';
import { formatRelative } from '@/utils/dateUtils';
import { ACTIVITY_TYPE_META } from '@/constants/dashboard';
import { useDashboardActivity } from '@/hooks/useDashboard';
import SectionCard from './SectionCard';

/**
 * Skeleton placeholder for a single activity list item.
 *
 * @returns {JSX.Element}
 */
function ActivityItemSkeleton() {
  return (
    <ListItem sx={{ px: 0, py: 0.75 }}>
      <ListItemAvatar>
        <Skeleton variant="circular" width={38} height={38} />
      </ListItemAvatar>
      <ListItemText
        primary={<Skeleton variant="text" width="70%" />}
        secondary={<Skeleton variant="text" width="40%" />}
      />
    </ListItem>
  );
}

/**
 * Recent portal activity feed with loading and empty states.
 *
 * @returns {JSX.Element}
 */
export default function RecentActivity() {
  const { data: activities, isLoading, isFetching, refresh } = useDashboardActivity({ limit: 8 });
  const theme = useTheme();
  const isDark = theme.palette.mode === 'dark';

  const isEmpty = !isLoading && (!activities || activities.length === 0);

  // Color mapping for activity types
  const colorHex = {
    success: '#10B981',
    warning: '#F59E0B',
    error: '#EF4444',
    info: '#3B82F6',
    primary: '#1A2342',
  };

  return (
    <SectionCard
      title="Recent Activity"
      subtitle="Latest events across the portal"
      showRefresh
      onRefresh={refresh}
      isFetching={isFetching}
      sx={{ height: '100%' }}
    >
      {isLoading ? (
        <List disablePadding>
          {[0, 1, 2, 3, 4].map((i) => (
            <ActivityItemSkeleton key={i} />
          ))}
        </List>
      ) : isEmpty ? (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: 200,
            gap: 1.5,
          }}
          role="status"
        >
          <Box
            sx={{
              width: 52,
              height: 52,
              borderRadius: '16px',
              bgcolor: isDark ? 'rgba(255,255,255,0.05)' : 'rgba(26,35,66,0.05)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
            }}
          >
            <AccessTimeRoundedIcon
              sx={{ fontSize: 24, color: isDark ? 'rgba(240,237,230,0.3)' : 'rgba(26,35,66,0.3)' }}
              aria-hidden
            />
          </Box>
          <Typography variant="body2" sx={{ color: isDark ? 'rgba(240,237,230,0.45)' : '#9CA3AF' }}>
            No recent activity to show.
          </Typography>
        </Box>
      ) : (
        <List disablePadding>
          {activities.map((item) => {
            const meta = ACTIVITY_TYPE_META[item.type] ?? ACTIVITY_TYPE_META.EMPLOYEE_JOINED;
            const IconComponent = meta.icon;
            const color = colorHex[meta.color] ?? '#1A2342';
            return (
              <ListItem key={item.id} alignItems="flex-start" sx={{ px: 0, py: 0.75 }}>
                <ListItemAvatar sx={{ minWidth: 48 }}>
                  <Avatar
                    sx={{
                      width: 36,
                      height: 36,
                      bgcolor: `${color}18`,
                      color,
                      border: `1px solid ${color}28`,
                    }}
                    aria-hidden="true"
                  >
                    <IconComponent sx={{ fontSize: 17 }} />
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={
                    <Typography
                      variant="body2"
                      fontWeight={500}
                      sx={{ color: isDark ? 'rgba(240,237,230,0.9)' : '#1A2342' }}
                    >
                      {item.description}
                    </Typography>
                  }
                  secondary={
                    <Typography
                      variant="caption"
                      sx={{ color: isDark ? 'rgba(240,237,230,0.4)' : '#9CA3AF' }}
                    >
                      {formatRelative(item.timestamp)}
                      {item.actorName ? ` · ${item.actorName}` : ''}
                    </Typography>
                  }
                />
              </ListItem>
            );
          })}
        </List>
      )}
    </SectionCard>
  );
}
