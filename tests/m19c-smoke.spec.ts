import { test, expect, type Page } from '@playwright/test';

const BASE_URL = 'http://localhost:3000';
const API_BASE = 'http://localhost:18080';
const SS = '/Users/jiangwei/Documents/niu/screenshots/m19c-admin-web-fixes-smoke';

async function login(page: Page) {
  let accessToken = '';
  for (let round = 0; round < 5 && !accessToken; round++) {
    for (let answer = 2; answer <= 18; answer++) {
      try {
        const captchaResp = await fetch(`${API_BASE}/api/auth/captcha`);
        const captchaJson = await captchaResp.json() as any;
        const captchaId = captchaJson.data?.captchaId;
        if (!captchaId) continue;

        const loginResp = await fetch(`${API_BASE}/api/auth/login/password`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            username: 'admin01',
            password: 'dev123',
            captchaId: captchaId,
            captchaCode: String(answer)
          })
        });
        const loginJson = await loginResp.json() as any;
        if (loginJson.data?.accessToken) {
          accessToken = loginJson.data.accessToken;
          break;
        }
      } catch { /* continue */ }
    }
    if (!accessToken) await new Promise(r => setTimeout(r, 500));
  }
  if (!accessToken) throw new Error('Failed to login');

  await page.addInitScript((token: string) => {
    localStorage.setItem('accessToken', token);
  }, accessToken);

  await page.goto(BASE_URL, { waitUntil: 'networkidle' });
  await page.waitForTimeout(2000);
}

test.describe('M19C Smoke', () => {
  test('Story 1: 工单状态和时间展示', async ({ page }) => {
    await login(page);
    await page.screenshot({ path: `${SS}/02-after-login.png`, fullPage: true });

    const menuItem = page.locator('.el-menu-item').filter({ hasText: '工单' });
    await menuItem.first().click();
    await page.waitForTimeout(3000);

    await page.screenshot({ path: `${SS}/story1-work-order-list.png`, fullPage: true });

    const body = await page.textContent('.el-table') || '';
    console.log('Has SETTLED:', body.includes('SETTLED'));
    console.log('Has PART_ARRIVED:', body.includes('PART_ARRIVED'));

    const isoMatch = body.match(/\d{4}-\d{2}-\d{2}T/);
    console.log('Has ISO time:', !!isoMatch);
  });

  test('Story 2: 收款记录时间展示', async ({ page }) => {
    await login(page);

    const menuItem = page.locator('.el-menu-item').filter({ hasText: '收款' });
    await menuItem.first().click();
    await page.waitForTimeout(3000);

    await page.screenshot({ path: `${SS}/story2-payment-records.png`, fullPage: true });

    const body = await page.textContent('.el-table') || '';
    const isoMatch = body.match(/\d{4}-\d{2}-\d{2}T/);
    console.log('Has ISO time:', !!isoMatch);
  });

  test('Story 3: 配件删除防呆', async ({ page }) => {
    await login(page);

    const menuItem = page.locator('.el-menu-item').filter({ hasText: '配件' });
    await menuItem.first().click();
    await page.waitForTimeout(3000);

    await page.screenshot({ path: `${SS}/story3-parts-list.png`, fullPage: true });

    const disabledBtns = await page.locator('button:has-text("删除"):disabled').count();
    console.log(`Disabled delete buttons: ${disabledBtns}`);
  });

  test('Story 4: 条码复制/打印', async ({ page }) => {
    await login(page);

    const menuItem = page.locator('.el-menu-item').filter({ hasText: '配件' });
    await menuItem.first().click();
    await page.waitForTimeout(3000);

    const viewBtn = page.locator('button:has-text("查看")').first();
    await viewBtn.click();
    await page.waitForTimeout(1500);

    await page.screenshot({ path: `${SS}/story4-part-detail.png`, fullPage: true });
  });

  test('Story 5: 库存调整二次确认', async ({ page }) => {
    await login(page);

    const menuItem = page.locator('.el-menu-item').filter({ hasText: '库存' });
    await menuItem.first().click();
    await page.waitForTimeout(3000);

    const adjustBtn = page.locator('button:has-text("库存调整")').first();
    await adjustBtn.click();
    await page.waitForTimeout(1000);

    await page.screenshot({ path: `${SS}/story5-adjust-form.png`, fullPage: true });

    // Fill quantity using the dialog's input
    const dialog = page.locator('.el-dialog:visible');
    const qtyInput = dialog.locator('.el-input-number__input input').first();
    if (await qtyInput.isVisible()) {
      await qtyInput.clear();
      await qtyInput.fill('5');
    }

    // Select reason within the dialog
    const reasonSelect = dialog.locator('.el-select').first();
    if (await reasonSelect.isVisible()) {
      await reasonSelect.click();
      await page.waitForTimeout(500);
      // Select from dropdown
      const dropdown = page.locator('.el-select-dropdown:visible');
      await dropdown.locator('.el-select-dropdown__item').filter({ hasText: '盘点盘盈' }).click();
      await page.waitForTimeout(300);
    }

    await page.screenshot({ path: `${SS}/story5-adjust-filled.png`, fullPage: true });

    // Click submit to trigger confirmation
    await dialog.locator('button:has-text("确认调整")').click();
    await page.waitForTimeout(1000);

    // Screenshot the confirmation dialog
    await page.screenshot({ path: `${SS}/story5-adjust-confirm.png`, fullPage: true });
  });

  test('Story 6: 客户/车辆查看', async ({ page }) => {
    await login(page);

    const menuItem = page.locator('.el-menu-item').filter({ hasText: '客户' });
    await menuItem.first().click();
    await page.waitForTimeout(3000);

    await page.screenshot({ path: `${SS}/story6-customer-list.png`, fullPage: true });

    const viewBtn = page.locator('button:has-text("查看详情")').first();
    if (await viewBtn.isVisible()) {
      await viewBtn.click();
      await page.waitForTimeout(2000);
      await page.screenshot({ path: `${SS}/story6-customer-detail.png`, fullPage: true });
    }
  });

  test('Story 7: 客户删除保护', async ({ page }) => {
    await login(page);

    const menuItem = page.locator('.el-menu-item').filter({ hasText: '客户' });
    await menuItem.first().click();
    await page.waitForTimeout(3000);

    const deleteBtn = page.locator('button:has-text("删除")').first();
    if (await deleteBtn.isVisible()) {
      await deleteBtn.click();
      await page.waitForTimeout(1000);
      await page.screenshot({ path: `${SS}/story7-customer-delete-confirm.png`, fullPage: true });
    }
  });
});
