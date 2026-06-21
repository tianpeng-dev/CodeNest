const internalAuthQueryKeys = new Set([
  '__clerk_handshake',
]);

const relativeUrlBase = 'https://codenest.local';

export function stripInternalAuthQuery(target: string) {
  try {
    const url = new URL(target, relativeUrlBase);

    if (url.origin !== relativeUrlBase) {
      return target;
    }

    internalAuthQueryKeys.forEach((key) => url.searchParams.delete(key));

    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return target;
  }
}

export function hasInternalAuthQuery(query: Record<string, unknown>) {
  return Object.keys(query).some((key) => internalAuthQueryKeys.has(key));
}

export function safeRedirectTarget(target: unknown, fallback: string) {
  if (typeof target !== 'string' || !target.startsWith('/') || target.startsWith('//')) {
    return fallback;
  }

  const sanitized = stripInternalAuthQuery(target);
  return sanitized === '/' || sanitized.startsWith('/') ? sanitized : fallback;
}
