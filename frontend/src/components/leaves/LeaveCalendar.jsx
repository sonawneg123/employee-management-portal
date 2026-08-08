/**
 * @fileoverview LeaveCalendar — monthly calendar view of leave requests.
 *
 * Renders a calendar grid for the current month. Each day that has
 * an approved or pending leave shows a colour-coded chip. Navigation
 * buttons move forward/back one month at a time.
 */

import React, { useState } from 'react';
import {
  Box,
  Card,
  CardContent,
  Chip,
  Grid,
  IconButton,
  Tooltip,
  Typography,
} from '@mui/material';
import ChevronLeftIcon  from '@mui/icons-material/ChevronLeft';
import ChevronRightIcon from '@mui/icons-material/ChevronRight';
import dayjs from 'dayjs';
import { isDateInLeaveRange } from '@/utils/leaveCalculations';
import { LEAVE_CALENDAR_COLORS, LEAVE_TYPE_MAP } from '@/constants/leaveConstants';

const WEEKDAY_LABELS = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

/**
 * @typedef {Object} LeaveCalendarProps
 * @property {import('@/services/leaveApi').LeaveRequestResponse[]} leaves
 * @property {(leave: Object) => void} [onLeaveClick]
 */

/**
 * Monthly calendar grid showing leave requests.
 *
 * @param {LeaveCalendarProps} props
 * @returns {JSX.Element}
 */
export default function LeaveCalendar({ leaves = [], onLeaveClick }) {
  const [currentMonth, setCurrentMonth] = useState(() => dayjs().startOf('month'));

  const prevMonth = () => setCurrentMonth((m) => m.subtract(1, 'month'));
  const nextMonth = () => setCurrentMonth((m) => m.add(1, 'month'));
  const today     = dayjs();

  // Build calendar day cells (including leading/trailing blanks)
  const startOfMonth = currentMonth.startOf('month');
  const daysInMonth  = currentMonth.daysInMonth();
  const startDow     = startOfMonth.day(); // 0=Sun

  const cells = [
    ...Array.from({ length: startDow }, () => null),
    ...Array.from({ length: daysInMonth }, (_, i) => currentMonth.date(i + 1)),
  ];

  // Pad to complete last row
  while (cells.length % 7 !== 0) cells.push(null);

  /**
   * Returns leaves that include the given date.
   *
   * @param {import('dayjs').Dayjs} date
   * @returns {import('@/services/leaveApi').LeaveRequestResponse[]}
   */
  const leavesOnDate = (date) =>
    leaves.filter(
      (l) =>
        l.status !== 'CANCELLED' &&
        l.status !== 'REJECTED' &&
        isDateInLeaveRange(date.format('YYYY-MM-DD'), l.startDate, l.endDate),
    );

  return (
    <Card>
      <CardContent>
        {/* Header */}
        <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', mb: 2 }}>
          <IconButton size="small" onClick={prevMonth} aria-label="Previous month">
            <ChevronLeftIcon />
          </IconButton>
          <Typography variant="h6" fontWeight={700}>
            {currentMonth.format('MMMM YYYY')}
          </Typography>
          <IconButton size="small" onClick={nextMonth} aria-label="Next month">
            <ChevronRightIcon />
          </IconButton>
        </Box>

        {/* Weekday labels */}
        <Grid container columns={7} sx={{ mb: 0.5 }}>
          {WEEKDAY_LABELS.map((d) => (
            <Grid key={d} size={1}>
              <Typography variant="caption" fontWeight={700} color="text.secondary" textAlign="center" display="block">
                {d}
              </Typography>
            </Grid>
          ))}
        </Grid>

        {/* Day cells */}
        <Grid container columns={7}>
          {cells.map((day, idx) => {
            if (!day) return <Grid key={`blank-${idx}`} size={1} sx={{ minHeight: 80, borderTop: '1px solid', borderColor: 'divider' }} />;

            const dateStr     = day.format('YYYY-MM-DD');
            const isToday     = day.isSame(today, 'day');
            const dayLeaves   = leavesOnDate(day);
            const isWeekend   = day.day() === 0 || day.day() === 6;

            return (
              <Grid key={dateStr} size={1} sx={{
                minHeight: 80,
                borderTop: '1px solid',
                borderColor: 'divider',
                p: 0.5,
                bgcolor: isWeekend ? 'action.hover' : 'transparent',
              }}>
                <Box sx={{
                  width: 26, height: 26,
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  borderRadius: '50%',
                  bgcolor: isToday ? 'primary.main' : 'transparent',
                  mb: 0.25,
                }}>
                  <Typography
                    variant="caption"
                    fontWeight={isToday ? 700 : 400}
                    color={isToday ? 'primary.contrastText' : 'text.primary'}
                  >
                    {day.date()}
                  </Typography>
                </Box>

                <Box sx={{ display: 'flex', flexDirection: 'column', gap: 0.25 }}>
                  {dayLeaves.slice(0, 2).map((leave) => {
                    const color = LEAVE_CALENDAR_COLORS[leave.leaveType] ?? '#757575';
                    const label = leave.employeeName ?? LEAVE_TYPE_MAP[leave.leaveType]?.label;
                    return (
                      <Tooltip key={leave.id} title={`${label} — ${leave.leaveType}`}>
                        <Chip
                          label={label}
                          size="small"
                          onClick={onLeaveClick ? () => onLeaveClick(leave) : undefined}
                          sx={{
                            height: 16,
                            fontSize: 10,
                            bgcolor: color,
                            color: '#fff',
                            maxWidth: '100%',
                            cursor: onLeaveClick ? 'pointer' : 'default',
                            '& .MuiChip-label': { px: 0.5 },
                          }}
                          aria-label={`${label} leave on ${dateStr}`}
                        />
                      </Tooltip>
                    );
                  })}
                  {dayLeaves.length > 2 && (
                    <Typography variant="caption" color="text.secondary">
                      +{dayLeaves.length - 2} more
                    </Typography>
                  )}
                </Box>
              </Grid>
            );
          })}
        </Grid>
      </CardContent>
    </Card>
  );
}
