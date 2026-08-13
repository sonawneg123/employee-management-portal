/**
 * @fileoverview Tests for DepartmentsPage.
 *
 * Covers:
 *   - Renders page heading
 *   - Renders search input
 *   - Shows skeleton while loading
 *   - Shows empty state when no departments
 *   - Shows error alert when query fails
 *   - Add Department button visible for ADMIN/HR
 *   - Add Department button hidden for MANAGER
 *   - Opens create dialog when Add is clicked
 *   - Renders department name in the table
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

import DepartmentsPage from '@/pages/departments/DepartmentsPage';

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('@/hooks/useDepartmentHooks', () => ({
  useDepartmentList: vi.fn(),
  useCreateDepartment: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false, error: null })),
  useUpdateDepartment: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false, error: null })),
  useDeleteDepartment: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
  AuthContext: { Provider: ({ children }) => children },
}));

import { useDepartmentList } from '@/hooks/useDepartmentHooks';
import { useAuth } from '@/contexts/AuthContext';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const DEPT = {
  id: 'dept-1',
  name: 'Engineering',
  code: 'ENG',
  description: 'Software team',
  headName: 'Alice',
  employeeCount: 20,
  createdAt: '2022-01-01T00:00:00Z',
  updatedAt: '2022-01-01T00:00:00Z',
};

const PAGE_RESPONSE = {
  content: [DEPT],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  last: true,
};

// ── Wrapper ───────────────────────────────────────────────────────────────────

function renderPage(authOverrides = {}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const theme = createTheme();

  useAuth.mockReturnValue({
    user: {
      userId: 'u1',
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'User',
      roles: ['ROLE_ADMIN'],
    },
    isAuthenticated: true,
    hasAnyRole: (roles) => roles.some((r) => ['ROLE_ADMIN', 'ROLE_HR'].includes(r)),
    ...authOverrides,
  });

  return render(
    <HelmetProvider>
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={qc}>
          <MemoryRouter>
            <DepartmentsPage />
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    </HelmetProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks();
  useDepartmentList.mockReturnValue({
    data: PAGE_RESPONSE,
    isLoading: false,
    isFetching: false,
    isError: false,
    error: null,
    refresh: vi.fn(),
  });
});

describe('DepartmentsPage', () => {
  it('renders the Departments heading', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /departments/i })).toBeInTheDocument();
  });

  it('renders the search input', () => {
    renderPage();
    expect(screen.getByRole('textbox', { name: /department search input/i })).toBeInTheDocument();
  });

  it('shows skeleton while loading', () => {
    useDepartmentList.mockReturnValue({
      data: undefined,
      isLoading: true,
      isFetching: true,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });
    renderPage();
    expect(screen.getByLabelText('Loading departments')).toBeInTheDocument();
  });

  it('shows empty state when no departments', () => {
    useDepartmentList.mockReturnValue({
      data: { content: [], totalElements: 0, page: 0, size: 20, totalPages: 0, last: true },
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });
    renderPage();
    expect(screen.getByText('No departments yet')).toBeInTheDocument();
  });

  it('shows error alert when query fails', () => {
    useDepartmentList.mockReturnValue({
      data: undefined,
      isLoading: false,
      isFetching: false,
      isError: true,
      error: { message: 'Service unavailable' },
      refresh: vi.fn(),
    });
    renderPage();
    expect(screen.getByText('Service unavailable')).toBeInTheDocument();
  });

  it('shows Add Department button for ADMIN', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /add new department/i })).toBeInTheDocument();
  });

  it('hides Add Department button for MANAGER', () => {
    renderPage({ hasAnyRole: () => false });
    expect(screen.queryByRole('button', { name: /add new department/i })).not.toBeInTheDocument();
  });

  it('opens create dialog when Add is clicked', async () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /add new department/i }));
    await waitFor(() => {
      expect(screen.getByRole('dialog', { name: /add new department/i })).toBeInTheDocument();
    });
  });

  it('renders department name in the table', () => {
    renderPage();
    expect(screen.getByText('Engineering')).toBeInTheDocument();
  });
});
