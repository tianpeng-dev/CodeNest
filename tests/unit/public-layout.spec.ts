import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const publicLayoutSource = () => {
  return readFileSync(
    join(process.cwd(), 'src/layouts/PublicLayout.vue'),
    'utf8',
  );
};

describe('public layout header', () => {
  it('renders centered search and account actions', () => {
    const source = publicLayoutSource();

    expect(source).toContain('class="public-layout__search"');
    expect(source).toContain('submitHeaderSearch');
    expect(source).toContain('placeholder="搜索技术文章、问题和作者"');
    expect(source).toContain('class="public-layout__login"');
    expect(source).toContain('class="public-layout__register"');
    expect(source).toContain('class="public-layout__post-button"');
    expect(source).toContain('to="/creator/editor"');
    expect(source).not.toContain('class="public-layout__menu"');
  });
});
