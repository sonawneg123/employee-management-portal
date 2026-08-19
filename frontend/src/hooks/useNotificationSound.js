/**
 * @fileoverview useNotificationSound — Web Audio API–based notification tones.
 *
 * Plays a subtle two-tone chime when new notifications arrive.
 * Plays a pleasant "happy" two-tone chime for positive events
 * (LEAVE_APPROVED, ROLE_UPDATED).
 * Respects browser autoplay restrictions: if the AudioContext cannot be
 * created or playback is blocked, the hook fails silently.
 *
 * Sound preference is persisted in localStorage under "notif_sound_muted".
 */

import { useCallback, useEffect, useRef, useState } from 'react';

const STORAGE_KEY = 'notif_sound_muted';

/** Notification types that should play the "happy" chime instead of the standard one. */
const HAPPY_TYPES = new Set(['LEAVE_APPROVED', 'ROLE_UPDATED']);

/**
 * Returns controls for the notification sound.
 *
 * @returns {{ muted: boolean, toggleMute: () => void, playSound: () => void, playHappySound: () => void, playSoundForType: (type: string) => void }}
 */
export function useNotificationSound() {
  const [muted, setMuted] = useState(() => {
    try {
      return localStorage.getItem(STORAGE_KEY) === 'true';
    } catch {
      return false;
    }
  });

  const audioCtxRef = useRef(null);

  /**
   * Lazily creates the AudioContext on first user gesture.
   */
  const getAudioCtx = useCallback(() => {
    if (!audioCtxRef.current) {
      try {
        audioCtxRef.current = new (window.AudioContext || window.webkitAudioContext)();
      } catch {
        return null;
      }
    }
    return audioCtxRef.current;
  }, []);

  /**
   * Plays a short two-tone notification chime (standard).
   * Does nothing when muted or when the browser blocks autoplay.
   */
  const playSound = useCallback(() => {
    if (muted) return;

    try {
      const ctx = getAudioCtx();
      if (!ctx) return;

      // Resume context if suspended (needed after user-gesture requirements)
      const resume = ctx.state === 'suspended' ? ctx.resume() : Promise.resolve();

      resume
        .then(() => {
          const now = ctx.currentTime;

          // Tone 1: 880 Hz (A5), short envelope
          const osc1 = ctx.createOscillator();
          const gain1 = ctx.createGain();
          osc1.type = 'sine';
          osc1.frequency.setValueAtTime(880, now);
          gain1.gain.setValueAtTime(0, now);
          gain1.gain.linearRampToValueAtTime(0.18, now + 0.02);
          gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.25);
          osc1.connect(gain1);
          gain1.connect(ctx.destination);
          osc1.start(now);
          osc1.stop(now + 0.25);

          // Tone 2: 1047 Hz (C6), follows tone 1
          const osc2 = ctx.createOscillator();
          const gain2 = ctx.createGain();
          osc2.type = 'sine';
          osc2.frequency.setValueAtTime(1047, now + 0.18);
          gain2.gain.setValueAtTime(0, now + 0.18);
          gain2.gain.linearRampToValueAtTime(0.18, now + 0.2);
          gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.45);
          osc2.connect(gain2);
          gain2.connect(ctx.destination);
          osc2.start(now + 0.18);
          osc2.stop(now + 0.45);
        })
        .catch(() => {
          // Autoplay blocked — silent failure
        });
    } catch {
      // AudioContext not supported — silent failure
    }
  }, [muted, getAudioCtx]);

  /**
   * Plays a pleasant "happy" two-tone chime for positive events
   * (leave approval, role update, etc.).
   * Uses higher frequencies and a brighter ascending melody.
   * Does nothing when muted or when the browser blocks autoplay.
   */
  const playHappySound = useCallback(() => {
    if (muted) return;

    try {
      const ctx = getAudioCtx();
      if (!ctx) return;

      const resume = ctx.state === 'suspended' ? ctx.resume() : Promise.resolve();

      resume
        .then(() => {
          const now = ctx.currentTime;

          // Tone 1: 1047 Hz (C6) — bright, pleasant
          const osc1 = ctx.createOscillator();
          const gain1 = ctx.createGain();
          osc1.type = 'sine';
          osc1.frequency.setValueAtTime(1047, now);
          gain1.gain.setValueAtTime(0, now);
          gain1.gain.linearRampToValueAtTime(0.15, now + 0.02);
          gain1.gain.exponentialRampToValueAtTime(0.001, now + 0.28);
          osc1.connect(gain1);
          gain1.connect(ctx.destination);
          osc1.start(now);
          osc1.stop(now + 0.28);

          // Tone 2: 1319 Hz (E6) — ascending, cheerful
          const osc2 = ctx.createOscillator();
          const gain2 = ctx.createGain();
          osc2.type = 'sine';
          osc2.frequency.setValueAtTime(1319, now + 0.16);
          gain2.gain.setValueAtTime(0, now + 0.16);
          gain2.gain.linearRampToValueAtTime(0.15, now + 0.18);
          gain2.gain.exponentialRampToValueAtTime(0.001, now + 0.5);
          osc2.connect(gain2);
          gain2.connect(ctx.destination);
          osc2.start(now + 0.16);
          osc2.stop(now + 0.5);
        })
        .catch(() => {
          // Autoplay blocked — silent failure
        });
    } catch {
      // AudioContext not supported — silent failure
    }
  }, [muted, getAudioCtx]);

  /**
   * Plays the appropriate sound based on the notification type.
   * Happy types get the ascending chime; all others get the standard chime.
   *
   * @param {string} [type] - NotificationType string, e.g. "LEAVE_APPROVED"
   */
  const playSoundForType = useCallback(
    (type) => {
      if (HAPPY_TYPES.has(type)) {
        playHappySound();
      } else {
        playSound();
      }
    },
    [playSound, playHappySound],
  );

  const toggleMute = useCallback(() => {
    setMuted((prev) => {
      const next = !prev;
      try {
        localStorage.setItem(STORAGE_KEY, String(next));
      } catch {
        // localStorage not available
      }
      return next;
    });
  }, []);

  // Cleanup AudioContext on unmount
  useEffect(() => {
    return () => {
      audioCtxRef.current?.close().catch(() => {});
    };
  }, []);

  return { muted, toggleMute, playSound, playHappySound, playSoundForType };
}
