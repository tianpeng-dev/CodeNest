import { describe, expect, it } from 'vitest';
import {
  hasInternalAuthQuery,
  safeRedirectTarget,
  stripInternalAuthQuery,
} from '@/app/auth-redirect';

describe('auth redirect helpers', () => {
  it('removes Clerk handshake query params from redirect targets', () => {
    expect(
      stripInternalAuthQuery('/creator/overview?__clerk_handshake=token&tab=drafts#top'),
    ).toBe('/creator/overview?tab=drafts#top');
  });

  it('keeps normal relative redirect targets intact', () => {
    expect(safeRedirectTarget('/creator/overview?tab=drafts', '/')).toBe(
      '/creator/overview?tab=drafts',
    );
  });

  it('falls back for external or protocol-relative redirect targets', () => {
    expect(safeRedirectTarget('https://example.com', '/')).toBe('/');
    expect(safeRedirectTarget('//example.com/path', '/')).toBe('/');
  });

  it('detects Clerk internal auth query params', () => {
    expect(hasInternalAuthQuery({ __clerk_handshake: 'token' })).toBe(true);
    expect(hasInternalAuthQuery({ redirect: '/creator/overview' })).toBe(false);
  });
});
