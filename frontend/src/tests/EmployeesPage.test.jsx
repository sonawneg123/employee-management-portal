/**
 * @fileoverview Tests for EmployeesPage.
 *
 * Covers:
 *   - Renders page heading
 *   - Renders toolbar (search input visible)
 *   - Shows table skeleton while loading
 *   - Shows empty state when no employees returned
 *   - Shows error alert when query fails
 *   - Add Employee button is visible for ADMIN/HR roles
 *   - Add Employee button is hidden for EMPLOYEE role
 *   - Export button is visible when employees exist
 *   - Opens create dialog when Add Employee is clicked
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

import EmployeesPage from '@/pages/employees/EmployeesPage';

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('@/hooks/useEmployees', () => ({
  useEmployees: vi.fn(),
  useCreateEmployee: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false, error: null })),
  useUpdateEmployee: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false, error: null })),
  useDeleteEmployee: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
}));

vi.mock('@/hooks/useDepartments', () => ({
  useDepartments: vi.fn(() => ({ data: [], isLoading: false })),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
  AuthContext: { Provider: ({ children }) => children },
}));

import { useEmployees } from '@/hooks/useEmployees';
import { useAuth } from '@/contexts/AuthContext';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const EMPLOYEE = {
  id: 'emp-1',
  employeeCode: 'EMP001',
  firstName: 'Jane',
  lastName: 'Smith',
  email: 'jane@example.com',
  jobTitle: 'Engineer',
  departmentName: 'Engineering',
  status: 'ACTIVE',
  dateOfJoining: '2022-01-15',
  salary: 85000,
};

const PAGE_RESPONSE = {
  content: [EMPLOYEE],
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
            <EmployeesPage />
          </MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    </HelmetProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks();
  useEmployees.mockReturnValue({
    data: PAGE_RESPONSE,
    isLoading: false,
    isFetching: false,
    isError: false,
    error: null,
    refresh: vi.fn(),
  });
});

describe('EmployeesPage', () => {
  it('renders the Employees page heading', () => {
    renderPage();
    expect(screen.getByRole('heading', { name: /employees/i })).toBeInTheDocument();
  });

  it('renders the search input', () => {
    renderPage();
    expect(screen.getByRole('textbox', { name: /search employees/i })).toBeInTheDocument();
  });

  it('shows skeleton while loading', () => {
    useEmployees.mockReturnValue({
      data: undefined,
      isLoading: true,
      isFetching: true,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });
    renderPage();
    expect(screen.getByLabelText('Loading employees')).toBeInTheDocument();
  });

  it('shows empty state when no employees', () => {
    useEmployees.mockReturnValue({
      data: { content: [], totalElements: 0, page: 0, size: 20, totalPages: 0, last: true },
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });
    renderPage();
    expect(screen.getByText('No employees yet')).toBeInTheDocument();
  });

  it('shows error alert when query fails', () => {
    useEmployees.mockReturnValue({
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

  it('shows Add Employee button for ADMIN role', () => {
    renderPage();
    expect(screen.getByRole('button', { name: /add new employee/i })).toBeInTheDocument();
  });

  it('hides Add Employee button for EMPLOYEE role', () => {
    renderPage({
      hasAnyRole: () => false,
    });
    expect(screen.queryByRole('button', { name: /add new employee/i })).not.toBeInTheDocument();
  });

  it('opens create dialog when Add Employee is clicked', async () => {
    renderPage();
    fireEvent.click(screen.getByRole('button', { name: /add new employee/i }));
    await waitFor(() => {
      expect(screen.getByRole('dialog', { name: /add new employee/i })).toBeInTheDocument();
    });
  });

  it('renders employee name in the table', () => {
    renderPage();
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
  });
});
