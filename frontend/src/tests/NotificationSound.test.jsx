/**
 * @fileoverview Tests for useNotificationSound hook.
 * Tests that the hook respects the muted state and localStorage preference.
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useNotificationSound } from '@/hooks/useNotificationSound';

// Mock AudioContext so tests don't fail in jsdom
class MockAudioContext {
  state = 'running';
  currentTime = 0;
  destination = {};

  createOscillator() {
    return {
      type: '',
      frequency: { setValueAtTime: vi.fn() },
      connect: vi.fn(),
      start: vi.fn(),
      stop: vi.fn(),
    };
  }

  createGain() {
    return {
      gain: {
        setValueAtTime: vi.fn(),
        linearRampToValueAtTime: vi.fn(),
        exponentialRampToValueAtTime: vi.fn(),
      },
      connect: vi.fn(),
    };
  }

  resume() {
    return Promise.resolve();
  }

  close() {
    return Promise.resolve();
  }
}

beforeEach(() => {
  window.AudioContext = MockAudioContext;
  localStorage.clear();
});

afterEach(() => {
  vi.clearAllMocks();
});

describe('useNotificationSound', () => {
  it('starts unmuted by default', () => {
    const { result } = renderHook(() => useNotificationSound());
    expect(result.current.muted).toBe(false);
  });

  it('reads muted state from localStorage', () => {
    localStorage.setItem('notif_sound_muted', 'true');
    const { result } = renderHook(() => useNotificationSound());
    expect(result.current.muted).toBe(true);
  });

  it('toggleMute toggles muted state', () => {
    const { result } = renderHook(() => useNotificationSound());
    expect(result.current.muted).toBe(false);

    act(() => {
      result.current.toggleMute();
    });
    expect(result.current.muted).toBe(true);
    expect(localStorage.getItem('notif_sound_muted')).toBe('true');

    act(() => {
      result.current.toggleMute();
    });
    expect(result.current.muted).toBe(false);
    expect(localStorage.getItem('notif_sound_muted')).toBe('false');
  });

  it('playSound does not throw when muted', () => {
    localStorage.setItem('notif_sound_muted', 'true');
    const { result } = renderHook(() => useNotificationSound());
    expect(() => result.current.playSound()).not.toThrow();
  });

  it('playSound does not throw when AudioContext is unavailable', () => {
    delete window.AudioContext;
    delete window.webkitAudioContext;
    const { result } = renderHook(() => useNotificationSound());
    expect(() => result.current.playSound()).not.toThrow();
  });
});
