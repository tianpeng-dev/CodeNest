import { expect, test } from '@playwright/test';

test('home page renders public content', async ({ page }) => {
  await page.goto('/');

  await expect(page.getByText('CodeNest').first()).toBeVisible();
  await expect(page.getByText('推荐文章')).toBeVisible();
});

test('guest is redirected from creator center to login', async ({ page }) => {
  await page.goto('/creator/overview');

  await expect(page).toHaveURL(/\/login/);
});

test('admin can open dashboard', async ({ page }) => {
  await page.goto('/login');
  await page.locator('input[name="username"]').fill('admin');
  await page.locator('input[name="password"]').fill('admin123');
  await page.getByRole('button', { name: /登录/ }).click();
  await expect(page).toHaveURL('/');
  await page.goto('/admin');

  await expect(page.getByRole('heading', { name: '管理后台' })).toBeVisible();
});

test('home page adapts to mobile width', async ({ page }) => {
  await page.setViewportSize({ width: 390, height: 844 });
  await page.goto('/');

  await expect(page.getByText('CodeNest').first()).toBeVisible();
  await expect(page.getByText('推荐文章')).toBeVisible();
});
