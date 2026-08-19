/**
 * @fileoverview Tests for AiAssistantChat component.
 *
 * Uses Vitest + React Testing Library.
 * The aiApi service is mocked so no real HTTP calls are made.
 *
 * Scenarios covered:
 * 1.  Renders the chat interface correctly
 * 2.  Shows empty state ("How can I help you?") when no messages exist
 * 3.  Suggested questions are rendered in the empty state
 * 4.  Clicking a suggested question triggers an API call and shows the question
 * 5.  Loading state while waiting for a response
 * 6.  "AI Assistant is thinking…" text is visible during loading
 * 7.  Displays AI response after successful call
 * 8.  Empty message validation — send button disabled for empty/whitespace input
 * 9.  Sends message on Enter key
 * 10. Clears input after sending
 * 11. Clear conversation button appears after messages
 * 12. 401 error is handled with a session-expired message
 * 13. Generic API error is handled gracefully
 * 14. Network error is handled gracefully
 * 15. No internal exception details are exposed
 * 16. API is called with the correct payload
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { HelmetProvider } from 'react-helmet-async';

// Default Vitest timeout (5 000 ms) is occasionally hit by slow jsdom+MUI
// renders when userEvent.type() walks every character.  Set a generous
// per-file budget so tests remain reliable without changing test logic.
const TEST_TIMEOUT = 15_000;

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

    it('shows "How can I help you?" heading in the empty state', () => {
      renderChat();
      expect(screen.getByText(/how can i help you/i)).toBeInTheDocument();
    });

    it('shows the HR context hint text in the empty state', () => {
      renderChat();
      expect(screen.getByText(/hr policies, leave, remote work/i)).toBeInTheDocument();
    });

    it('does not show clear conversation button when there are no messages', () => {
      renderChat();
      expect(screen.queryByRole('button', { name: /clear conversation/i })).not.toBeInTheDocument();
    });
  });

  // ── Empty state & suggested questions ─────────────────────────────────────

  describe('Empty state and suggested questions', () => {
    it('renders at least one suggested question chip', () => {
      renderChat();
      // All suggested questions use aria-label "Suggested question: ..."
      const chips = screen.getAllByRole('button', { name: /suggested question:/i });
      expect(chips.length).toBeGreaterThan(0);
    });

    it('renders the remote work advance notice question', () => {
      renderChat();
      expect(
        screen.getByRole('button', {
          name: /suggested question:.*how many days in advance should i request remote work/i,
        }),
      ).toBeInTheDocument();
    });

    it('renders the manager approval question', () => {
      renderChat();
      expect(
        screen.getByRole('button', {
          name: /suggested question:.*does my manager need to approve remote work/i,
        }),
      ).toBeInTheDocument();
    });

    it('renders the remote work denial question', () => {
      renderChat();
      expect(
        screen.getByRole('button', {
          name: /suggested question:.*when can remote work be denied/i,
        }),
      ).toBeInTheDocument();
    });

    it('clicking a suggested question calls sendAiMessage with that question', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'You need 2 working days notice.' });
      const { user } = renderChat();

      const chip = screen.getByRole('button', {
        name: /suggested question:.*how many days in advance should i request remote work/i,
      });
      await user.click(chip);

      await waitFor(() => {
        expect(sendAiMessage).toHaveBeenCalledWith(
          'How many days in advance should I request remote work?',
        );
      });
    });

    it('clicking a suggested question adds the question as a user message', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'Manager approval is required.' });
      const { user } = renderChat();

      const chip = screen.getByRole('button', {
        name: /suggested question:.*does my manager need to approve remote work/i,
      });
      await user.click(chip);

      await waitFor(() => {
        expect(
          screen.getByText('Does my manager need to approve remote work?'),
        ).toBeInTheDocument();
      });
    });

    it('clicking a suggested question shows the AI response', async () => {
      sendAiMessage.mockResolvedValue({
        answer: 'Yes, manager approval is required for remote work.',
      });
      const { user } = renderChat();

      const chip = screen.getByRole('button', {
        name: /suggested question:.*does my manager need to approve remote work/i,
      });
      await user.click(chip);

      await waitFor(() => {
        expect(
          screen.getByText('Yes, manager approval is required for remote work.'),
        ).toBeInTheDocument();
      });
    });

    it('suggested questions are hidden after the first message is sent', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'OK' });
      const { user } = renderChat();

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Hello');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        // Empty state (and chips) should no longer be visible
        expect(screen.queryByText(/how can i help you/i)).not.toBeInTheDocument();
      });
    });
  });

  // ── Empty message validation ───────────────────────────────────────────────

  describe('Empty message validation', () => {
    it('send button is disabled when input is empty', () => {
      renderChat();
      expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
    });

    it('send button is disabled when input is only whitespace', async () => {
      renderChat();
      fireEvent.change(screen.getByRole('textbox', { name: /message input/i }), {
        target: { value: '   ' },
      });
      expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
    });

    it('send button is enabled when input has content', async () => {
      renderChat();
      fireEvent.change(screen.getByRole('textbox', { name: /message input/i }), {
        target: { value: 'What is the leave policy?' },
      });
      expect(screen.getByRole('button', { name: /send message/i })).not.toBeDisabled();
    });
  });

  // ── Sending a message ──────────────────────────────────────────────────────

  describe('Sending a message', () => {
    it(
      'calls sendAiMessage with the typed message when send is clicked',
      async () => {
        sendAiMessage.mockResolvedValue({ answer: 'You have 20 days.' });
        const { user } = renderChat();

        fireEvent.change(screen.getByRole('textbox', { name: /message input/i }), {
          target: { value: 'How many leave days?' },
        });
        await user.click(screen.getByRole('button', { name: /send message/i }));

        await waitFor(() => {
          expect(sendAiMessage).toHaveBeenCalledWith('How many leave days?');
        });
      },
      TEST_TIMEOUT,
    );

    it(
      'displays the user message in the chat after sending',
      async () => {
        sendAiMessage.mockResolvedValue({ answer: 'Annual leave is 20 days.' });
        const { user } = renderChat();

        fireEvent.change(screen.getByRole('textbox', { name: /message input/i }), {
          target: { value: 'What is annual leave?' },
        });
        await user.click(screen.getByRole('button', { name: /send message/i }));

        await waitFor(() => {
          expect(screen.getByText('What is annual leave?')).toBeInTheDocument();
        });
      },
      TEST_TIMEOUT,
    );

    it(
      'sends message when Enter key is pressed (without shift)',
      async () => {
        sendAiMessage.mockResolvedValue({ answer: 'OK' });
        renderChat();

        const input = screen.getByRole('textbox', { name: /message input/i });
        // Set input value then fire keyDown directly on the element so the
        // component's onKeyDown handler is triggered reliably in jsdom.
        fireEvent.change(input, { target: { value: 'Hello' } });
        fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', shiftKey: false });

        await waitFor(() => {
          expect(sendAiMessage).toHaveBeenCalledWith('Hello');
        });
      },
      TEST_TIMEOUT,
    );

    it(
      'clears the input field after sending',
      async () => {
        sendAiMessage.mockResolvedValue({ answer: 'OK' });
        const { user } = renderChat();

        const input = screen.getByRole('textbox', { name: /message input/i });
        fireEvent.change(input, { target: { value: 'Test message' } });
        await user.click(screen.getByRole('button', { name: /send message/i }));

        await waitFor(() => {
          expect(input).toHaveValue('');
        });
      },
      TEST_TIMEOUT,
    );
  });

  // ── Loading state ──────────────────────────────────────────────────────────

  describe('Loading state', () => {
    it('disables send button while loading', async () => {
      // Never resolves — simulates in-flight request
      sendAiMessage.mockImplementation(() => new Promise(() => {}));
      const { user } = renderChat();

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Test');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /send message/i })).toBeDisabled();
      });
    });

    it('shows "AI Assistant is thinking…" text while loading', async () => {
      sendAiMessage.mockImplementation(() => new Promise(() => {}));
      const { user } = renderChat();

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Test');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByText(/ai assistant is thinking/i)).toBeInTheDocument();
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

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Hi');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /clear conversation/i })).toBeInTheDocument();
      });
    });
  });

  // ── Error handling ─────────────────────────────────────────────────────────

  describe('Error handling', () => {
    it('displays "session has expired" message for 401 error', async () => {
      sendAiMessage.mockRejectedValue({
        status: 401,
        isNetwork: false,
        message: 'Your session has expired. Please log in again.',
      });
      const { user } = renderChat();

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Question');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/your session has expired/i);
      });
    });

    it('displays a friendly error when the API call fails with a generic error', async () => {
      sendAiMessage.mockRejectedValue({
        status: 409,
        message: 'The AI assistant is temporarily unavailable. Please try again later.',
        isNetwork: false,
      });
      const { user } = renderChat();

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Question');
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

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Question');
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

  // ── JWT / auth integration ─────────────────────────────────────────────────

  describe('JWT / auth integration', () => {
    it('does not hard-code any token strings in the component output', () => {
      const { container } = renderChat();
      const html = container.innerHTML;
      // Should not contain any hardcoded JWT-like strings
      expect(html).not.toMatch(/Bearer\s+ey/i);
      expect(html).not.toMatch(/gsk_/);
      expect(html).not.toMatch(/hf_/);
    });

    it('sendAiMessage is called exactly once per user message', async () => {
      sendAiMessage.mockResolvedValue({ answer: 'Test answer' });
      const { user } = renderChat();

      await user.type(screen.getByRole('textbox', { name: /message input/i }), 'Single message');
      await user.click(screen.getByRole('button', { name: /send message/i }));

      await waitFor(() => {
        expect(sendAiMessage).toHaveBeenCalledTimes(1);
      });
    });
  });
});
