/**
 * @fileoverview Phase 7E frontend tests — AI Copilot component.
 */

import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { vi, describe, it, expect, beforeEach } from 'vitest';
import AiCopilotChat from '../components/ai/AiCopilotChat';
import * as agentApi from '../services/agentApi';

// Mock the API service
vi.mock('../services/agentApi', () => ({
  sendAgentMessage: vi.fn(),
}));

// jsdom does not implement scrollIntoView — mock it globally
beforeEach(() => {
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
});

const mockInfoResponse = {
  answer: 'You have 3 active tasks.',
  responseType: 'INFORMATION',
  toolsExecuted: ['search_tasks'],
  confirmationToken: null,
  actionSummary: null,
};

const mockActionProposalResponse = {
  answer:
    'I\'m ready to perform this action:\n\n**Reassign task "Fix bug" to Rahul**\n\nDo you want me to proceed?',
  responseType: 'ACTION_PROPOSAL',
  toolsExecuted: ['get_task', 'get_employee_availability'],
  confirmationToken: 'test-token-123',
  actionSummary: 'Reassign task "Fix bug" to Rahul',
};

const mockActionCompletedResponse = {
  answer: 'Done. Task "Fix bug" has been reassigned to Rahul.',
  responseType: 'ACTION_COMPLETED',
  toolsExecuted: [],
  confirmationToken: null,
  actionSummary: 'Reassign task "Fix bug" to Rahul',
};

const mockErrorResponse = {
  answer: 'Access denied.',
  responseType: 'ERROR',
  toolsExecuted: [],
  confirmationToken: null,
  actionSummary: null,
};

describe('AiCopilotChat — Phase 7E', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders the empty state with suggested questions', () => {
    render(<AiCopilotChat />);
    // "AI Copilot" appears in both the header and the empty state; use getAllByText
    expect(screen.getAllByText('AI Copilot').length).toBeGreaterThanOrEqual(1);
    expect(screen.getByText(/Who is overloaded/i)).toBeInTheDocument();
  });

  it('sends a message and displays an INFORMATION response', async () => {
    agentApi.sendAgentMessage.mockResolvedValueOnce(mockInfoResponse);

    render(<AiCopilotChat />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'What are my tasks?' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('You have 3 active tasks.')).toBeInTheDocument();
    });

    expect(agentApi.sendAgentMessage).toHaveBeenCalledWith('What are my tasks?');
  });

  it('shows ACTION_PROPOSAL confirmation card', async () => {
    agentApi.sendAgentMessage.mockResolvedValueOnce(mockActionProposalResponse);

    render(<AiCopilotChat />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'Reassign task Fix bug to Rahul' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('Action Requires Confirmation')).toBeInTheDocument();
    });

    expect(screen.getByRole('button', { name: /Confirm/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
  });

  it('sends confirmation token when user clicks Confirm', async () => {
    agentApi.sendAgentMessage
      .mockResolvedValueOnce(mockActionProposalResponse)
      .mockResolvedValueOnce(mockActionCompletedResponse);

    render(<AiCopilotChat />);

    // Send initial message
    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'Reassign task Fix bug to Rahul' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('Action Requires Confirmation')).toBeInTheDocument();
    });

    // Click confirm
    fireEvent.click(screen.getByRole('button', { name: /Confirm/i }));

    await waitFor(() => {
      expect(agentApi.sendAgentMessage).toHaveBeenCalledWith('Yes, proceed.', 'test-token-123');
    });

    await waitFor(() => {
      expect(screen.getByText(/Done\. Task "Fix bug" has been reassigned/)).toBeInTheDocument();
    });
  });

  it('cancels an action proposal when user clicks Cancel', async () => {
    agentApi.sendAgentMessage.mockResolvedValueOnce(mockActionProposalResponse);

    render(<AiCopilotChat />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'Reassign task' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Cancel/i })).toBeInTheDocument();
    });

    fireEvent.click(screen.getByRole('button', { name: /Cancel/i }));

    await waitFor(() => {
      expect(screen.getByText(/The action has been cancelled/)).toBeInTheDocument();
    });
  });

  it('displays tools executed as chips', async () => {
    agentApi.sendAgentMessage.mockResolvedValueOnce(mockInfoResponse);

    render(<AiCopilotChat />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'Show tasks' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('Search Tasks')).toBeInTheDocument();
    });
  });

  it('shows error when API fails', async () => {
    agentApi.sendAgentMessage.mockRejectedValueOnce({ status: 500, message: 'Server error' });

    render(<AiCopilotChat />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'What are my tasks?' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });

  it('prevents message longer than 4000 chars', () => {
    render(<AiCopilotChat />);
    const input = screen.getByRole('textbox');
    const longMessage = 'a'.repeat(4001);
    fireEvent.change(input, { target: { value: longMessage } });

    const sendButton = screen.getByRole('button', { name: /Send message/i });
    expect(sendButton).toBeDisabled();
  });

  it('clears conversation when clear button is clicked', async () => {
    agentApi.sendAgentMessage.mockResolvedValueOnce(mockInfoResponse);

    render(<AiCopilotChat />);

    const input = screen.getByRole('textbox');
    fireEvent.change(input, { target: { value: 'Test message' } });
    fireEvent.keyDown(input, { key: 'Enter' });

    await waitFor(() => {
      expect(screen.getByText('You have 3 active tasks.')).toBeInTheDocument();
    });

    fireEvent.click(screen.getByLabelText('Clear conversation'));

    expect(screen.queryByText('You have 3 active tasks.')).not.toBeInTheDocument();
    expect(screen.getAllByText('AI Copilot').length).toBeGreaterThanOrEqual(1);
  });
});
