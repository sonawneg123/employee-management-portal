/**
 * @fileoverview Tests for AiAssistantChat component.
 *
 * Uses Vitest + React Testing Library.
 * The aiApi service is mocked so no real HTTP calls are made.
 *
 * Scenarios covered:
 * - Renders the chat interface correctly
 * - Shows empty state when no messages exist
 * - Loading state while waiting for response
 * - Displays AI response after successful call
 * - Displays API error when the call fails
 * - Empty message validation (send button disabled)
 * - Sends message on Enter key
 * - Clear conversation button appears after messages
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { HelmetProvider } from 'react-helmet-async';

import AiAssistantChat from '@/components/ai/AiAssistantChat';

// ── Mock aiApi ────────────────────────────────────────────────────────────────
vi.mock('@/services/aiApi', () => ({
  sendAiMessage: vi.fn(),
}));

import { sendAiMessage } from '@/services/aiApi';

// ── Test utilities ────────────────────────────────────────────────────────────

const testTheme = createTheme();

/**
 * Renders the AiAssistantChat component inside MUI theme provider.
 *
 * @returns {{ user: import('@testing-library/user-event').UserEvent } & import('@testing-library/react').RenderResult}
 */
function renderChat() {
  const user = userEvent.setup();
  const result = render(
    <HelmetProvider>
      <ThemeProvider theme={testTheme}>
        <AiAssistantChat />
      </ThemeProvider>
    </HelmetProvider>,
  );
  return { ...result, user };
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('AiAssistantChat', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // jsdom does not implement scrollIntoView — stub it to avoid test errors.
    window.HTMLElement.prototype.scrollIntoView = vi.fn();
  });

  afterEach(() => {
    vi.clearAllMocks();
  });

  // ── Rendering ──────────────────────────────────────────────────────────────

  describe('Rendering', () => {
    it('renders the HR AI Assistant heading', () => {
      renderChat();
      expect(screen.getByText('HR AI Assistant')).toBeInTheDocument();
    });

    it('renders the message input field', () => {
      renderChat();
      expect(screen.getByRole('textbox', { name: /message input/i })).toBeInTheDocument();
    });

    it('renders the send button', () => {
      renderChat();
      expect(screen.getByRole('button', { name: /send message/i })).toBeInTheDocument();
    });

    it('shows the empty state hint text when no messages exist', () => {
      renderChat();
      expect(screen.getByText(/ask me anything about hr policies/i)).toBeInTheDocument();
    });

    it('does not show clear conversation button when there are no messages', () => {
      renderChat();
      expect(screen.queryByRole('button', { name: /clear conversation/i })).not.toBeInTheDocument();
    });
  });

  // ── Empty message validation ───────────────────────────────────────────────

  describe('Empty message validation', () => {
    it('send button is disabled when input is empty', () => {
      renderChat();
      expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
    });

    it('send button is disabled when input is only whitespace', async () => {
      const { user } = renderChat();
      await user.type(screen.getByRole('textbox', { name: /message input/i }), '   ');
      expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
    });

    it('send button is enabled when input has content', async () => {
      const { user } = renderChat();
      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'What is the leave policy?',
      );
      expect(screen.getByRole('button', { name: /send message/i })).not.toBeDisabled();
    });
  });

  // ── Sending a message ──────────────────────────────────────────────────────

  describe('Sending a message', () => {
    it('calls sendAiMessage with the typed message when send is clicked', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'You have 20 days.' });
      const { user } = renderChat();

      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'How many leave days?',
      );
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(sendAiMessage).toHaveBeenCalledWith('How many leave days?');
      });
    });

    it('displays the user message in the chat after sending', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'Annual leave is 20 days.' });
      const { user } = renderChat();

      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'What is annual leave?',
      );
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByText('What is annual leave?')).toBeInTheDocument();
      });
    });

    it('sends message when Enter key is pressed (without shift)', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'OK' });
      const { user } = renderChat();

      const input = screen.getByRole('textbox', { name: /message input/i });
      await user.type(input, 'Hello{Enter}');

      await waitFor(() => {
        expect(sendAiMessage).toHaveBeenCalledWith('Hello');
      });
    });

    it('clears the input field after sending', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'OK' });
      const { user } = renderChat();

      const input = screen.getByRole('textbox', { name: /message input/i });
      await user.type(input, 'Test message');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(input).toHaveValue('');
      });
    });
  });

  // ── Loading state ──────────────────────────────────────────────────────────

  describe('Loading state', () => {
    it('disables send button while loading', async () => {
      // Never resolves — simulates in-flight request
      sendAiMessage.mockImplementation(() => new Promise(() => {}));
      const { user } = renderChat();

      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'Test',
      );
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
      });
    });
  });

  // ── Displaying AI response ─────────────────────────────────────────────────

  describe('Displaying AI response', () => {
    it('shows the AI response in the chat after a successful call', async () => {
      sendAiMessage.mockResolvedValue({
        answer: 'The annual leave entitlement is 20 working days.',
      });
      const { user } = renderChat();

      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'Leave entitlement?',
      );
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(
          screen.getByText('The annual leave entitlement is 20 working days.'),
        ).toBeInTheDocument();
      });
    });

    it('shows the clear button after messages are added', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'Hello!' });
      const { user } = renderChat();

      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'Hi',
      );
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /clear conversation/i })).toBeInTheDocument();
      });
    });
  });

  // ── Error handling ─────────────────────────────────────────────────────────

  describe('Error handling', () => {
    it('displays a friendly error when the API call fails', async () => {
      sendAiMessage.mockRejectedValue({
        status: 409,
        message: 'The AI assistant is temporarily unavailable. Please try again later.',
        isNetwork: false,
      });
      const { user } = renderChat();

      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'Question',
      );
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/temporarily unavailable/i);
      });
    });

    it('displays a friendly error on network failure', async () => {
      sendAiMessage.mockRejectedValue({
        status: 0,
        isNetwork: true,
        message: 'Unable to reach the server.',
      });
      const { user } = renderChat();

      await user.type(
        screen.getByRole('textbox', { name: /message input/i }),
        'Question',
      );
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/unable to reach the server/i);
      });
    });

    it('does not expose stack traces or internal exception details', async () => {
      sendAiMessage.mockRejectedValue({
        status: 500,
        message: 'Internal error occurred.',
        isNetwork: false,
      });
      const { user } = renderChat();

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Q');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        const alertText = screen.getByRole('alert').textContent;
        expect(alertText).not.toMatch(/GroqClient/);
        expect(alertText).not.toMatch(/api-key/i);
        expect(alertText).not.toMatch(/gsk_/);
        expect(alertText).not.toMatch(/stack/i);
      });
    });
  });
});
