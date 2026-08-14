/**
 * @fileoverview AI Assistant API service.
 *
 * Forwards user messages to the backend POST /ai/chat endpoint.
 * The GROQ_API_KEY is never used here — it is handled entirely server-side.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} AiChatResponse
 * @property {string} answer - The AI assistant's generated response.
 */

/**
 * Sends a user message to the AI HR Assistant.
 *
 * The backend forwards the message to Groq and returns the generated answer.
 * Authentication is handled automatically by the axios request interceptor.
 *
 * @param {string} message - The user's question or statement.
 * @returns {Promise<AiChatResponse>}
 */
export async function sendAiMessage(message) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.AI_CHAT, { message });
  return data;
}
