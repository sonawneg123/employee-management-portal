/**
 * @fileoverview useEmployeePhoto — authenticated photo URL for an employee.
 *
 * Fetches an employee's profile photo via the authenticated axiosInstance and
 * creates a blob object URL for use in <img> / MUI Avatar src props.
 *
 * Revokes the object URL on unmount to prevent memory leaks.
 *
 * @param {string | null | undefined} employeeId  - UUID of the employee.
 * @param {string | null | undefined} photoUrl    - Relative API URL for the photo, or null/undefined.
 * @returns {{ objectUrl: string | null }}
 */

import { useEffect, useRef, useState } from 'react';

let axiosInstancePromise = null;

function getAxios() {
  if (!axiosInstancePromise) {
    axiosInstancePromise = import('@/api/axiosInstance').then((m) => m.default);
  }
  return axiosInstancePromise;
}

/**
 * Returns a blob object URL for an employee photo that requires JWT authentication,
 * or null if no photo URL is provided.
 *
 * @param {string | null | undefined} photoApiUrl - Relative URL like "/api/employees/{id}/profile-photo"
 * @returns {{ objectUrl: string | null }}
 */
export function useEmployeePhoto(photoApiUrl) {
  const [objectUrl, setObjectUrl] = useState(null);
  const prevPhotoUrl = useRef(null);

  useEffect(() => {
    if (!photoApiUrl) {
      if (objectUrl) {
        URL.revokeObjectURL(objectUrl);
        setObjectUrl(null);
      }
      return;
    }

    // Only re-fetch if the photo URL changed
    if (prevPhotoUrl.current === photoApiUrl && objectUrl) return;
    prevPhotoUrl.current = photoApiUrl;

    let cancelled = false;

    getAxios().then((axiosInstance) => {
      // Strip the /api prefix from the URL since axiosInstance baseURL already includes it
      const path = photoApiUrl.replace(/^\/api/, '');
      axiosInstance
        .get(path, { responseType: 'blob' })
        .then((res) => {
          if (!cancelled) {
            const url = URL.createObjectURL(res.data);
            setObjectUrl((prev) => {
              if (prev) URL.revokeObjectURL(prev);
              return url;
            });
          }
        })
        .catch(() => {
          if (!cancelled) setObjectUrl(null);
        });
    });

    return () => {
      cancelled = true;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [photoApiUrl]);

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { objectUrl };
}
