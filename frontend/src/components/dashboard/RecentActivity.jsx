/**
 * @fileoverview RecentActivity — time-ordered list of recent portal events.
 *
 * Fetches data from {@link useDashboardActivity} and renders each event as
 * a timeline-style list item with icon, description, and relative timestamp.
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
} from '@mui/material';
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
    <ListItem sx={{ px: 0 }}>
      <ListItemAvatar>
        <Skeleton variant="circular" width={40} height={40} />
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

  const isEmpty = !isLoading && (!activities || activities.length === 0);

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
            gap: 1,
            color: 'text.disabled',
          }}
          role="status"
        >
          <Typography variant="body2">No recent activity to show.</Typography>
        </Box>
      ) : (
        <List disablePadding>
          {activities.map((item) => {
            const meta = ACTIVITY_TYPE_META[item.type] ?? ACTIVITY_TYPE_META.EMPLOYEE_JOINED;
            const IconComponent = meta.icon;
            return (
              <ListItem key={item.id} alignItems="flex-start" sx={{ px: 0, py: 0.75 }}>
                <ListItemAvatar sx={{ minWidth: 44 }}>
                  <Avatar
                    sx={{
                      width: 36,
                      height: 36,
                      bgcolor: `${meta.color}.lighter`,
                      color: `${meta.color}.main`,
                    }}
                    aria-hidden="true"
                  >
                    <IconComponent sx={{ fontSize: 18 }} />
                  </Avatar>
                </ListItemAvatar>
                <ListItemText
                  primary={
                    <Typography variant="body2" fontWeight={500}>
                      {item.description}
                    </Typography>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary">
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
