import { describe, expect, it } from 'vitest';
import { renderMarkdown } from '@/utils/markdown';

describe('markdown renderer', () => {
  it('renders basic markdown while removing unsafe HTML', () => {
    const html = renderMarkdown([
      '# Safe title',
      '',
      'A paragraph with **bold** and `code`.',
      '',
      '<img src=x onerror=alert(1)>',
      '<script>alert(2)</script>',
    ].join('\n'));

    expect(html).toContain('<h1>Safe title</h1>');
    expect(html).toContain('<strong>bold</strong>');
    expect(html).toContain('<code>code</code>');
    expect(html).not.toContain('onerror');
    expect(html).not.toContain('<script');
  });
});
