/**
 * @fileoverview DepartmentDistributionChart — Recharts PieChart showing
 * the employee headcount per department.
 *
 * Consumes {@link useDashboardCharts} and formats the raw department
 * distribution via {@link toPieChartData} before passing it to Recharts.
 */

import React from 'react';
import {
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
} from 'recharts';
import { useTheme } from '@mui/material/styles';
import { useDashboardCharts } from '@/hooks/useDashboard';
import { toPieChartData } from '@/utils/dashboardFormatters';
import { CHART_COLORS } from '@/constants/dashboard';
import ChartCard from './ChartCard';

// ── Custom tooltip ────────────────────────────────────────────────────────────

/**
 * @typedef {Object} CustomTooltipProps
 * @property {boolean}                        active
 * @property {Array<{name: string, value: number, payload: Object}>} [payload]
 * @property {string}                         [label]
 */

/**
 * Custom Recharts tooltip for the department pie chart.
 *
 * @param {CustomTooltipProps} props
 * @returns {JSX.Element|null}
 */
function DeptTooltip({ active, payload }) {
  const theme = useTheme();
  if (!active || !payload?.length) return null;

  const { name, value } = payload[0];
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
      <strong>{name}</strong>
      <br />
      {value} {value === 1 ? 'employee' : 'employees'}
    </div>
  );
}

// ── Main chart component ──────────────────────────────────────────────────────

/**
 * Department headcount distribution as an interactive PieChart.
 *
 * @returns {JSX.Element}
 */
export default function DepartmentDistributionChart() {
  const { data: charts, isLoading, isFetching, refresh } = useDashboardCharts();

  const pieData = toPieChartData(charts?.departmentDistribution ?? []);
  const isEmpty = !isLoading && pieData.length === 0;

  return (
    <ChartCard
      title="Department Distribution"
      subtitle="Employee headcount per department"
      showRefresh
      onRefresh={refresh}
      isFetching={isFetching}
      loading={isLoading}
      isEmpty={isEmpty}
      emptyText="No department data available"
      height={300}
    >
      <ResponsiveContainer width="100%" height={300}>
        <PieChart>
          <Pie
            data={pieData}
            cx="50%"
            cy="50%"
            innerRadius={60}
            outerRadius={100}
            paddingAngle={3}
            dataKey="value"
            aria-label="Department distribution chart"
          >
            {pieData.map((entry, index) => (
              <Cell
                key={`cell-${entry.name}`}
                fill={CHART_COLORS[index % CHART_COLORS.length]}
              />
            ))}
          </Pie>
          <Tooltip content={<DeptTooltip />} />
          <Legend
            formatter={(value) => (
              <span style={{ fontSize: 12 }}>{value}</span>
            )}
          />
        </PieChart>
      </ResponsiveContainer>
    </ChartCard>
  );
}
