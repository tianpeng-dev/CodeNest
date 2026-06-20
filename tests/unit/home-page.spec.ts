import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const homePageSource = () => {
  return readFileSync(
    join(process.cwd(), 'src/pages/public/HomePage.vue'),
    'utf8',
  );
};

describe('home page filtering', () => {
  it('uses slug filters, current radio value API, and latest-request guards', () => {
    const source = homePageSource();

    expect(source).toContain('categorySlug');
    expect(source).toContain('requestSequence');
    expect(source).toContain("const isAllCategory = computed(() => activeCategory.value === 'all')");
    expect(source).toContain('<section v-if="isAllCategory" class="top-grid">');
    expect(source).toContain('const isSidebarCollapsed = ref(false)');
    expect(source).toContain('function toggleSidebar()');
    expect(source).toContain("'home-page--sidebar-collapsed': isSidebarCollapsed");
    expect(source).toContain("isSidebarCollapsed ? '展开左侧主菜单' : '折叠左侧主菜单'");
    expect(source).toContain(':value="category.value"');
    expect(source).not.toContain(':label="category.id"');
    expect(source).not.toContain("categoryId:");
    expect(source).not.toContain("id: 'cat-");
  });
});
