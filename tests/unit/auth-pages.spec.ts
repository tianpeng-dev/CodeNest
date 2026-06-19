import { readFileSync } from 'node:fs';
import { join } from 'node:path';
import { describe, expect, it } from 'vitest';

const source = (file: string) => {
  return readFileSync(join(process.cwd(), file), 'utf8');
};

describe('auth pages', () => {
  it('renders login form fields, quick fill accounts, and redirect navigation', () => {
    const loginPage = source('src/pages/auth/LoginPage.vue');

    expect(loginPage).toContain('<el-form');
    expect(loginPage).toContain('username');
    expect(loginPage).toContain('password');
    expect(loginPage).toContain('writer');
    expect(loginPage).toContain('password123');
    expect(loginPage).toContain('admin');
    expect(loginPage).toContain('admin123');
    expect(loginPage).toContain("route.query.redirect");
    expect(loginPage).toContain('/register');
  });

  it('renders register form fields and creator overview navigation', () => {
    const registerPage = source('src/pages/auth/RegisterPage.vue');

    expect(registerPage).toContain('<el-form');
    expect(registerPage).toContain('displayName');
    expect(registerPage).toContain('username');
    expect(registerPage).toContain('password');
    expect(registerPage).toContain('/creator/overview');
    expect(registerPage).toContain('/login');
  });
});
