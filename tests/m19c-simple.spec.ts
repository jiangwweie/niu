import { test, expect } from '@playwright/test';

const BASE_URL = 'http://localhost:3000';
const SCREENSHOT_DIR = '/Users/jiangwei/Documents/niu/screenshots/m19c-admin-web-fixes-smoke';

test('simple smoke test', async ({ page }) => {
  // Navigate to login page
  await page.goto(BASE_URL);
  await page.waitForTimeout(2000);

  // Take screenshot of login page
  await page.screenshot({ path: `${SCREENSHOT_DIR}/01-login-page.png`, fullPage: true });

  // Get page content for debugging
  const pageContent = await page.content();
  console.log('Page title:', await page.title());

  // Try to find login form elements
  const inputs = await page.locator('input').count();
  console.log('Number of inputs:', inputs);

  // Try different selectors for username
  const usernameInput = page.locator('input').first();
  const passwordInput = page.locator('input[type="password"]').first();

  if (await usernameInput.isVisible()) {
    await usernameInput.fill('admin01');
  }
  if (await passwordInput.isVisible()) {
    await passwordInput.fill('dev123');
  }

  // Click login button
  const loginButton = page.locator('button:has-text("登录"), button:has-text("Login"), button[type="submit"]').first();
  if (await loginButton.isVisible()) {
    await loginButton.click();
  }

  await page.waitForTimeout(3000);
  await page.screenshot({ path: `${SCREENSHOT_DIR}/02-after-login.png`, fullPage: true });

  console.log('Current URL:', page.url());
});
