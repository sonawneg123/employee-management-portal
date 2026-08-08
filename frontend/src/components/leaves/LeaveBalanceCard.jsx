/**
 * @fileoverview LeaveBalanceCard — shows leave entitlement vs used days.
 *
 * Renders a card per leave type showing how many days are available,
 * used, and remaining. Uses frontend-calculated entitlements as defaults.
 */

import React from 'react';
import {
  Box,
  Card,
  CardContent,
  Divider,
  LinearProgress,
  Skeleton,
  Typography,
} from '@mui/material';
import {
  LEAVE_TYPE_OPTIONS,
  LEAVE_DEFAULT_ENTITLEMENT,
} from '@/constants/leaveConstants';
import { usedDaysByType } from '@/utils/leaveCalculations';

/**
 * @typedef {Object} LeaveBalanceCardProps
 * @property {import('@/services/leaveApi').LeaveRequestResponse[]} [approvedLeaves=[]]
 *   - The current user's approved leave requests for the calculation.
 * @property {boolean} [isLoading]
 */

/**
 * Leave balance summary card for the My Leaves page.
 *
 * @param {LeaveBalanceCardProps} props
 * @returns {JSX.Element}
 */
export default function LeaveBalanceCard({ approvedLeaves = [], isLoading = false }) {
  return (
    <Card>
      <CardContent>
        <Typography variant="subtitle1" fontWeight={700} gutterBottom>
          Leave Balance
        </Typography>
        <Divider sx={{ mb: 2 }} />

        {isLoading ? (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {[0, 1, 2].map((i) => (
              <Box key={i}>
                <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                  <Skeleton variant="text" width="40%" />
                  <Skeleton variant="text" width="20%" />
                </Box>
                <Skeleton variant="rectangular" height={6} sx={{ borderRadius: 3 }} />
              </Box>
            ))}
          </Box>
        ) : (
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
            {LEAVE_TYPE_OPTIONS.filter((t) =>
              ['ANNUAL', 'SICK', 'MATERNITY', 'PATERNITY', 'EMERGENCY'].includes(t.value)
            ).map((typeOpt) => {
              const entitlement = LEAVE_DEFAULT_ENTITLEMENT[typeOpt.value] ?? 0;
              const used        = usedDaysByType(approvedLeaves, typeOpt.value);
              const remaining   = Math.max(0, entitlement - used);
              const pct         = entitlement > 0 ? Math.min(100, (used / entitlement) * 100) : 0;

              return (
                <Box key={typeOpt.value}>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', mb: 0.5 }}>
                    <Typography variant="body2">
                      {typeOpt.icon} {typeOpt.label}
                    </Typography>
                    <Typography variant="body2" fontWeight={600}>
                      {remaining} / {entitlement} days
                    </Typography>
                  </Box>
                  <LinearProgress
                    variant="determinate"
                    value={pct}
                    color={pct >= 90 ? 'error' : pct >= 70 ? 'warning' : 'primary'}
                    sx={{ height: 6, borderRadius: 3 }}
                    aria-label={`${typeOpt.label}: ${remaining} days remaining`}
                  />
                </Box>
              );
            })}
          </Box>
        )}
      </CardContent>
    </Card>
  );
}
