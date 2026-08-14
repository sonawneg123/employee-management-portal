/**
 * @fileoverview Tests for ProfilePage — personal info editing flow.
 *
 * Covers:
 * - Profile data is displayed from API response.
 * - Edit form populates with current values.
 * - Successful save shows success toast.
 * - Successful save updates AuthContext (name visible in header/avatar).
 * - Failed save shows error toast.
 * - Cancel edit restores original values.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { HelmetProvider } from 'react-helmet-async';

import ProfilePage from '@/pages/profile/ProfilePage';
import { AuthContext } from '@/contexts/AuthContext';

// ── Mock the profile API ───────────────────────────────────────────────────────
vi.mock('@/services/profileApi', () => ({
  getProfile: vi.fn(),
  updatePersonalInfo: vi.fn(),
}));

import * as profileApi from '@/services/profileApi';

// ── Helpers ───────────────────────────────────────────────────────────────────

const testTheme = createTheme();

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

const BASE_USER = {
  userId: 'user-1',
  email: 'john.doe@example.com',
  firstName: 'John',
  lastName: 'Doe',
  roles: ['ROLE_EMPLOYEE'],
};

const BASE_PROFILE = {
  userId: 'user-1',
  email: 'john.doe@example.com',
  firstName: 'John',
  lastName: 'Doe',
  roles: 'ROLE_EMPLOYEE',
  employeeId: 'emp-1',
  employeeCode: 'EMP-0001',
  departmentId: 'dept-1',
  departmentName: 'Engineering',
  jobTitle: 'Software Engineer',
  phone: '+1-555-0100',
  address: '123 Main St',
  dateOfJoining: '2024-01-15',
  salary: 75000,
  status: 'ACTIVE',
};

/**
 * Renders ProfilePage with all required providers.
 *
 * @param {object} [opts]
 * @param {Partial<typeof BASE_USER>} [opts.user]
 * @param {Function} [opts.updateUser]
 * @returns {{ user: import('@testing-library/user-event').UserEvent } & import('@testing-library/react').RenderResult}
 */
function renderProfile({ user: userOverride = {}, updateUser = vi.fn() } = {}) {
  const authUser = { ...BASE_USER, ...userOverride };
  const qc = makeQueryClient();
  const ue = userEvent.setup();

  const authValue = {
    user: authUser,
    token: 'mock-token',
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    updateUser,
    hasRole: (r) => authUser.roles.includes(r),
    hasAnyRole: (rs) => rs.some((r) => authUser.roles.includes(r)),
  };

  const result = render(
    <HelmetProvider>
      <QueryClientProvider client={qc}>
        <ThemeProvider theme={testTheme}>
          <MemoryRouter>
            <AuthContext.Provider value={authValue}>
              <ProfilePage />
            </AuthContext.Provider>
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </HelmetProvider>,
  );

  return { ...result, user: ue, updateUser };
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('ProfilePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  describe('Rendering', () => {
    it('displays profile data once the API resolves', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      renderProfile();

      await waitFor(() => {
        expect(screen.getByText('John Doe')).toBeInTheDocument();
      });

      expect(screen.getByText('Software Engineer')).toBeInTheDocument();
      expect(screen.getByText('Engineering')).toBeInTheDocument();
    });

    it('shows avatar initials derived from profile name', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      renderProfile();

      await waitFor(() => {
        expect(screen.getByText('JD')).toBeInTheDocument();
      });
    });
  });

  describe('Edit mode', () => {
    it('populates edit fields with current profile values', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      const { user } = renderProfile();

      // Wait for profile to load, then click Edit (button text is "Edit")
      await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /^edit$/i }));

      await waitFor(() => {
        expect(screen.getByDisplayValue('John')).toBeInTheDocument();
        expect(screen.getByDisplayValue('Doe')).toBeInTheDocument();
      });
    });

    it('cancel edit restores original values without calling the API', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      const { user } = renderProfile();

      await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /^edit$/i }));

      // Change first name
      const firstNameInput = await screen.findByDisplayValue('John');
      await user.clear(firstNameInput);
      await user.type(firstNameInput, 'Changed');

      // Cancel
      await user.click(screen.getByRole('button', { name: /^cancel$/i }));

      // Edit form is closed — original name still shown
      await waitFor(() => {
        expect(screen.queryByDisplayValue('Changed')).not.toBeInTheDocument();
        expect(screen.getByText('John Doe')).toBeInTheDocument();
      });

      expect(profileApi.updatePersonalInfo).not.toHaveBeenCalled();
    });
  });

  describe('Save mutation', () => {
    it('calls updatePersonalInfo when the Save Changes button is clicked', async () => {
      const updateUserMock2 = vi.fn();
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      vi.mocked(profileApi.updatePersonalInfo).mockResolvedValue(BASE_PROFILE);

      const { user } = renderProfile({ updateUser: updateUserMock2 });

      await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /^edit$/i }));

      // Save Changes button without changing anything — verifies the API is called
      await user.click(screen.getByRole('button', { name: /save changes/i }));

      await waitFor(() => {
        expect(profileApi.updatePersonalInfo).toHaveBeenCalled();
      });
    });

    it('calls updateUser with returned firstName/lastName on success', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      const updatedProfile = { ...BASE_PROFILE, firstName: 'Jane', lastName: 'Smith' };
      vi.mocked(profileApi.updatePersonalInfo).mockResolvedValue(updatedProfile);

      const updateUserMock = vi.fn();
      const { user } = renderProfile({ updateUser: updateUserMock });

      await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /^edit$/i }));

      const firstNameInput = await screen.findByDisplayValue('John');
      await user.clear(firstNameInput);
      await user.type(firstNameInput, 'Jane');

      const lastNameInput = await screen.findByDisplayValue('Doe');
      await user.clear(lastNameInput);
      await user.type(lastNameInput, 'Smith');

      // Save Changes button
      await user.click(screen.getByRole('button', { name: /save changes/i }));

      await waitFor(() => {
        expect(updateUserMock).toHaveBeenCalledWith({ firstName: 'Jane', lastName: 'Smith' });
      });
    });

    it('shows success snackbar on successful save', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      vi.mocked(profileApi.updatePersonalInfo).mockResolvedValue(BASE_PROFILE);

      const { user } = renderProfile();

      await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /^edit$/i }));
      // Save Changes button
      await user.click(screen.getByRole('button', { name: /save changes/i }));

      await waitFor(() => {
        expect(screen.getByText(/profile updated successfully/i)).toBeInTheDocument();
      });
    });

    it('shows error snackbar when save fails', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      vi.mocked(profileApi.updatePersonalInfo).mockRejectedValue({
        message: 'Server error',
      });

      const { user } = renderProfile();

      await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /^edit$/i }));
      // Save Changes button
      await user.click(screen.getByRole('button', { name: /save changes/i }));

      await waitFor(() => {
        expect(screen.getByText(/server error/i)).toBeInTheDocument();
      });
    });

    it('does not call updateUser when save fails', async () => {
      vi.mocked(profileApi.getProfile).mockResolvedValue(BASE_PROFILE);
      vi.mocked(profileApi.updatePersonalInfo).mockRejectedValue({ message: 'Fail' });

      const updateUserMock = vi.fn();
      const { user } = renderProfile({ updateUser: updateUserMock });

      await waitFor(() => expect(screen.getByText('John Doe')).toBeInTheDocument());
      await user.click(screen.getByRole('button', { name: /^edit$/i }));
      // Save Changes button
      await user.click(screen.getByRole('button', { name: /save changes/i }));

      await waitFor(() => {
        expect(screen.getByText(/fail/i)).toBeInTheDocument();
      });

      expect(updateUserMock).not.toHaveBeenCalled();
    });
  });
});
