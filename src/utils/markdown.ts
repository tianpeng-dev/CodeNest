import DOMPurify from 'dompurify';

function stripHtml(input: string) {
  return input
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/<style[\s\S]*?>[\s\S]*?<\/style>/gi, '')
    .replace(/<[^>]+>/g, '');
}

function escapeHtml(input: string) {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function unescapeHtml(input: string) {
  return input
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'");
}

function sanitizeUrl(url: string) {
  const trimmed = unescapeHtml(url).trim();
  if (/^(https?:|mailto:|\/|#)/i.test(trimmed)) {
    return escapeHtml(trimmed);
  }

  return '#';
}

function renderInline(input: string) {
  const escaped = escapeHtml(input);

  return escaped
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*]+)\*/g, '<em>$1</em>')
    .replace(
      /\[([^\]]+)\]\(([^)]+)\)/g,
      (_match, label: string, url: string) => {
        return `<a href="${sanitizeUrl(url)}" target="_blank" rel="noreferrer">${label}</a>`;
      },
    );
}

function renderParagraph(lines: string[]) {
  if (lines.length === 0) return '';
  return `<p>${lines.map(renderInline).join('<br />')}</p>`;
}

function renderList(lines: string[]) {
  const items = lines.map((line) => {
    return `<li>${renderInline(line.replace(/^[-*]\s+/, ''))}</li>`;
  });

  return `<ul>${items.join('')}</ul>`;
}

export function renderMarkdown(markdown: string) {
  const safeSource = stripHtml(markdown);
  const lines = safeSource.replace(/\r\n/g, '\n').split('\n');
  const blocks: string[] = [];
  let paragraph: string[] = [];
  let list: string[] = [];
  let code: string[] = [];
  let inCodeBlock = false;

  function flushParagraph() {
    const html = renderParagraph(paragraph);
    if (html) blocks.push(html);
    paragraph = [];
  }

  function flushList() {
    if (list.length > 0) {
      blocks.push(renderList(list));
      list = [];
    }
  }

  for (const line of lines) {
    if (line.trim().startsWith('```')) {
      if (inCodeBlock) {
        blocks.push(`<pre><code>${escapeHtml(code.join('\n'))}</code></pre>`);
        code = [];
        inCodeBlock = false;
      } else {
        flushParagraph();
        flushList();
        inCodeBlock = true;
      }
      continue;
    }

    if (inCodeBlock) {
      code.push(line);
      continue;
    }

    if (!line.trim()) {
      flushParagraph();
      flushList();
      continue;
    }

    const heading = line.match(/^(#{1,3})\s+(.+)$/);
    if (heading) {
      flushParagraph();
      flushList();
      const level = heading[1].length;
      blocks.push(`<h${level}>${renderInline(heading[2])}</h${level}>`);
      continue;
    }

    if (/^[-*]\s+/.test(line)) {
      flushParagraph();
      list.push(line);
      continue;
    }

    flushList();
    paragraph.push(line);
  }

  if (inCodeBlock) {
    blocks.push(`<pre><code>${escapeHtml(code.join('\n'))}</code></pre>`);
  }
  flushParagraph();
  flushList();

  return DOMPurify.sanitize(blocks.join('\n'), {
    ALLOWED_TAGS: [
      'a',
      'br',
      'code',
      'em',
      'h1',
      'h2',
      'h3',
      'li',
      'p',
      'pre',
      'strong',
      'ul',
    ],
    ALLOWED_ATTR: ['href', 'rel', 'target'],
  });
}
