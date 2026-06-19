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

  it('preserves safe link query strings without double escaping', () => {
    const html = renderMarkdown(
      '[docs](https://example.com?a=1&b=2)',
    );

    expect(html).toContain('href="https://example.com?a=1&amp;b=2"');
    expect(html).not.toContain('&amp;amp;');
  });

  it('blocks unsafe link protocols', () => {
    const html = renderMarkdown(
      '[bad](javascript:alert(1)) [also-bad](data:text/html,alert)',
    );

    expect(html).not.toContain('javascript:');
    expect(html).not.toContain('data:text');
    expect(html).toContain('href="#"');
  });
});
