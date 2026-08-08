/**
 * @fileoverview EmployeeStatusChart — Recharts BarChart showing the count
 * of employees per status (ACTIVE, INACTIVE, ON_LEAVE, TERMINATED).
 *
 * Consumes {@link useDashboardCharts} and maps the status breakdown from the
 * API response to a format Recharts can consume directly.
 */

import React from 'react';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { useTheme } from '@mui/material/styles';
import { useDashboardCharts } from '@/hooks/useDashboard';
import { EMPLOYEE_STATUS_COLORS } from '@/constants/dashboard';
import ChartCard from './ChartCard';

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Converts a raw status key such as "ON_LEAVE" into a title-cased label
 * "On Leave" suitable for axis ticks and tooltips.
 *
 * @param {string} status - Raw enum string from the API.
 * @returns {string}
 */
function toLabel(status) {
  return status
    .split('_')
    .map((w) => w.charAt(0) + w.slice(1).toLowerCase())
    .join(' ');
}

// ── Custom tooltip ────────────────────────────────────────────────────────────

/**
 * @typedef {Object} CustomTooltipProps
 * @property {boolean}  active
 * @property {Array<{name: string, value: number, payload: Object}>} [payload]
 */

/**
 * Custom Recharts tooltip for the employee status bar chart.
 *
 * @param {CustomTooltipProps} props
 * @returns {JSX.Element|null}
 */
function StatusTooltip({ active, payload }) {
  const theme = useTheme();
  if (!active || !payload?.length) return null;

  const { label, value, fill } = payload[0].payload;
  return (
    <div
      style={{
        background: theme.palette.background.paper,
        border: `1px solid ${theme.palette.divider}`,
        borderRadius: 8,
        padding: '8px 12px',
        fontSize: 13,
        boxShadow: theme.shadows[3],
      }}
    >
      <span style={{ color: fill, fontWeight: 600 }}>{label}</span>
      <br />
      {value} {value === 1 ? 'employee' : 'employees'}
    </div>
  );
}

// ── Main chart component ──────────────────────────────────────────────────────

/**
 * Employee status breakdown as a vertical BarChart.
 *
 * @returns {JSX.Element}
 */
export default function EmployeeStatusChart() {
  const { data: charts, isLoading, isFetching, refresh } = useDashboardCharts();

  const rawData = charts?.employeeStatusBreakdown ?? [];
  const chartData = rawData.map((item) => ({
    status: item.status,
    label:  toLabel(item.status),
    value:  item.count,
    fill:   EMPLOYEE_STATUS_COLORS[item.status] ?? '#1976d2',
  }));

  const isEmpty = !isLoading && chartData.length === 0;

  return (
    <ChartCard
      title="Employee Status"
      subtitle="Headcount breakdown by employment status"
      showRefresh
      onRefresh={refresh}
      isFetching={isFetching}
      loading={isLoading}
      isEmpty={isEmpty}
      emptyText="No status data available"
      height={300}
    >
      <ResponsiveContainer width="100%" height={300}>
        <BarChart
          data={chartData}
          margin={{ top: 8, right: 16, left: 0, bottom: 4 }}
          aria-label="Employee status chart"
        >
          <CartesianGrid strokeDasharray="3 3" vertical={false} />
          <XAxis
            dataKey="label"
            tick={{ fontSize: 12 }}
            axisLine={false}
            tickLine={false}
          />
          <YAxis
            allowDecimals={false}
            tick={{ fontSize: 12 }}
            axisLine={false}
            tickLine={false}
            width={36}
          />
          <Tooltip content={<StatusTooltip />} cursor={{ fill: 'rgba(0,0,0,0.04)' }} />
          <Bar dataKey="value" radius={[4, 4, 0, 0]} maxBarSize={60}>
            {chartData.map((entry) => (
              <Cell key={entry.status} fill={entry.fill} />
            ))}
          </Bar>
        </BarChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}
