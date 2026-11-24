import { describe, it, expect } from 'vitest';
import { resolveApiBaseUrl } from '../api/client';

describe('resolveApiBaseUrl', () => {
  it('defaults to /api when input is undefined, null, or empty', () => {
    expect(resolveApiBaseUrl(undefined)).toBe('/api');
    expect(resolveApiBaseUrl('')).toBe('/api');
    expect(resolveApiBaseUrl('   ')).toBe('/api');
  });

  it('keeps /api intact for relative paths', () => {
    expect(resolveApiBaseUrl('/api')).toBe('/api');
    expect(resolveApiBaseUrl('/api/')).toBe('/api');
    expect(resolveApiBaseUrl('api')).toBe('/api');
  });

  it('ensures /api is appended to full Render backend domain without duplicating', () => {
    expect(resolveApiBaseUrl('https://jobtracker-xys2.onrender.com')).toBe(
      'https://jobtracker-xys2.onrender.com/api'
    );
    expect(resolveApiBaseUrl('https://jobtracker-xys2.onrender.com/')).toBe(
      'https://jobtracker-xys2.onrender.com/api'
    );
    expect(resolveApiBaseUrl('https://jobtracker-xys2.onrender.com/api')).toBe(
      'https://jobtracker-xys2.onrender.com/api'
    );
    expect(resolveApiBaseUrl('https://jobtracker-xys2.onrender.com/api/')).toBe(
      'https://jobtracker-xys2.onrender.com/api'
    );
  });

  it('handles http localhost URLs correctly', () => {
    expect(resolveApiBaseUrl('http://localhost:8080')).toBe('http://localhost:8080/api');
    expect(resolveApiBaseUrl('http://localhost:8080/api')).toBe('http://localhost:8080/api');
    expect(resolveApiBaseUrl('http://localhost:8080/api/')).toBe('http://localhost:8080/api');
  });
});
