/**
 * @fileoverview AI Copilot API service — Phase 7E.
 *
 * Wraps the POST /api/ai/agent/chat endpoint.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * Sends a message to the Agentic AI Copilot and returns the structured response.
 *
 * @param {string} message - The user's message
 * @param {string|null} confirmationToken - Optional token to confirm a proposed action
 * @returns {Promise<import('./agentApiTypes').AgentChatResponse>}
 */
export async function sendAgentMessage(message, confirmationToken = null) {
  const payload = { message };
  if (confirmationToken) {
    payload.confirmationToken = confirmationToken;
  }
  const response = await axiosInstance.post(API_ENDPOINTS.AI_AGENT_CHAT, payload);
  return response.data;
}
