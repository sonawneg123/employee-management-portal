/**
 * @fileoverview Task Submission API service.
 *
 * Covers:
 *  - Creating a submission (employee submits work for review) — multipart/form-data
 *  - Listing and fetching submissions for a task
 *  - Resubmitting after changes requested — multipart/form-data
 *  - Manager approve / request-changes
 *  - Attachment download
 *
 * Phase 6B.1: createSubmission and resubmit now send multipart/form-data
 * with a 'submission' JSON part and an optional 'file' part.
 * Text-only submissions (no file) remain fully supported.
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} TaskSubmissionResponse
 * @property {string}                 id
 * @property {string}                 taskId
 * @property {string}                 taskTitle
 * @property {string}                 submittedById
 * @property {string}                 submittedByName
 * @property {string|null}            submissionNotes
 * @property {string|null}            workCompleted
 * @property {string|null}            additionalComments
 * @property {string}                 submittedAt
 * @property {'PENDING_REVIEW'|'APPROVED'|'CHANGES_REQUESTED'} reviewStatus
 * @property {string|null}            reviewComment
 * @property {string|null}            reviewedById
 * @property {string|null}            reviewedByName
 * @property {string|null}            reviewedAt
 * @property {string}                 createdAt
 * @property {string}                 updatedAt
 * @property {string|null}            attachmentOriginalName
 * @property {string|null}            attachmentMimeType
 * @property {number|null}            attachmentSizeBytes
 * @property {string|null}            attachmentUploadedAt
 * @property {boolean}                hasAttachment
 */

/**
 * Builds a FormData payload for create/resubmit requests.
 * The 'submission' part is a JSON blob; 'file' is optional.
 *
 * @param {{ submissionNotes: string, workCompleted?: string|null, additionalComments?: string|null }} submissionData
 * @param {File|null} [file]
 * @returns {FormData}
 */
function buildSubmissionFormData(submissionData, file) {
  const form = new FormData();
  form.append(
    'submission',
    new Blob([JSON.stringify(submissionData)], { type: 'application/json' }),
  );
  if (file) {
    form.append('file', file);
  }
  return form;
}

/**
 * Creates a new task submission.
 * Employee submits IN_PROGRESS task work for manager review.
 * Transitions task: IN_PROGRESS → SUBMITTED.
 *
 * @param {string} taskId
 * @param {{ submissionNotes: string, workCompleted?: string|null, additionalComments?: string|null }} payload
 * @param {File|null} [file] optional file attachment
 * @returns {Promise<TaskSubmissionResponse>}
 */
export async function createSubmission(taskId, payload, file = null) {
  const form = buildSubmissionFormData(payload, file);
  const { data } = await axiosInstance.post(API_ENDPOINTS.TASK_SUBMISSIONS(taskId), form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return data;
}

/**
 * Returns all submissions for the given task (ordered by submission time descending).
 *
 * @param {string} taskId
 * @returns {Promise<TaskSubmissionResponse[]>}
 */
export async function getSubmissionsForTask(taskId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_SUBMISSIONS(taskId));
  return data;
}

/**
 * Returns the latest submission for the given task.
 *
 * @param {string} taskId
 * @returns {Promise<TaskSubmissionResponse>}
 */
export async function getLatestSubmission(taskId) {
  const { data } = await axiosInstance.get(API_ENDPOINTS.TASK_SUBMISSIONS_LATEST(taskId));
  return data;
}

/**
 * Employee resubmits after manager has requested changes.
 * Submission must be in CHANGES_REQUESTED state.
 * Transitions task: IN_PROGRESS → SUBMITTED.
 *
 * @param {string} submissionId
 * @param {{ submissionNotes: string, workCompleted?: string|null, additionalComments?: string|null }} payload
 * @param {File|null} [file] optional replacement file attachment
 * @returns {Promise<TaskSubmissionResponse>}
 */
export async function resubmit(submissionId, payload, file = null) {
  const form = buildSubmissionFormData(payload, file);
  const { data } = await axiosInstance.put(
    API_ENDPOINTS.TASK_SUBMISSION_RESUBMIT(submissionId),
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  );
  return data;
}

/**
 * Manager approves a task submission.
 * Transitions task: SUBMITTED → COMPLETED.
 *
 * @param {string} submissionId
 * @returns {Promise<TaskSubmissionResponse>}
 */
export async function approveSubmission(submissionId) {
  const { data } = await axiosInstance.post(API_ENDPOINTS.TASK_SUBMISSION_APPROVE(submissionId));
  return data;
}

/**
 * Manager requests changes on a task submission.
 * Transitions task: SUBMITTED → IN_PROGRESS.
 *
 * @param {string} submissionId
 * @param {{ reviewComment: string }} payload
 * @returns {Promise<TaskSubmissionResponse>}
 */
export async function requestChanges(submissionId, payload) {
  const { data } = await axiosInstance.post(
    API_ENDPOINTS.TASK_SUBMISSION_REQUEST_CHANGES(submissionId),
    payload,
  );
  return data;
}

/**
 * Downloads the attachment for the given submission.
 * The browser will be triggered to download or open the file based on MIME type.
 *
 * @param {string} submissionId
 * @param {string} filename suggested download filename
 * @returns {Promise<void>}
 */
export async function downloadAttachment(submissionId, filename) {
  const response = await axiosInstance.get(
    API_ENDPOINTS.TASK_SUBMISSION_ATTACHMENT(submissionId),
    { responseType: 'blob' },
  );
  const url = window.URL.createObjectURL(response.data);
  const link = document.createElement('a');
  link.href = url;
  link.setAttribute('download', filename || 'attachment');
  document.body.appendChild(link);
  link.click();
  link.parentNode.removeChild(link);
  window.URL.revokeObjectURL(url);
}
