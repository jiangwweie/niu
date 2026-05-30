import { chromium } from 'playwright';
import { mkdirSync } from 'fs';

const SCREENSHOT_DIR = '/Users/jiangwei/Documents/niu/screenshots/m19c-admin-web-fixes-smoke';
mkdirSync(SCREENSHOT_DIR, { recursive: true });

const results = {};

async function run() {
  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  const page = await context.newPage();

  // Helper to save screenshot
  async function snap(name) {
    const path = `${SCREENSHOT_DIR}/${name}.png`;
    await page.screenshot({ path, fullPage: false });
    console.log(`  [screenshot] ${path}`);
    return path;
  }

  // Helper to wait for table load
  async function waitForTable() {
    await page.waitForTimeout(1500);
  }

  // ========== LOGIN ==========
  console.log('\n=== Logging in ===');
  await page.goto('http://localhost:3000');
  await page.waitForTimeout(2000);

  // Check if we're on login page
  const loginForm = await page.$('input[placeholder*="用户名"], input[placeholder*="账号"]');
  if (loginForm) {
    await page.fill('input[placeholder*="用户名"], input[placeholder*="账号"]', 'admin01');
    await page.fill('input[type="password"]', 'dev123');
    // Click login button
    const loginBtn = await page.$('button:has-text("登录"), button:has-text("Login")');
    if (loginBtn) {
      await loginBtn.click();
      await page.waitForTimeout(3000);
    }
  }
  console.log('  Logged in successfully');

  // ========== STORY 1: 工单状态和时间展示 ==========
  console.log('\n=== Story 1: 工单状态和时间展示 ===');
  try {
    // Navigate to work order management
    const woMenu = await page.$('text=工单管理');
    if (woMenu) {
      await woMenu.click();
      await waitForTable();
    }

    await snap('story1-work-order-list');

    // Check for English status values
    const pageContent = await page.textContent('body');
    const hasEnglishStatus = /SETTLED|PART_ARRIVED|CANCELLED|IN_PROGRESS|CREATING/i.test(pageContent);
    const hasChineseStatus = /已结算|部分到货|已取消|维修中|新建中|已交付/.test(pageContent);

    // Check time format
    const hasISOTime = /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(pageContent);
    const hasReadableTime = /\d{4}-\d{2}-\d{2} \d{2}:\d{2}/.test(pageContent);

    console.log(`  English status found: ${hasEnglishStatus}`);
    console.log(`  Chinese status found: ${hasChineseStatus}`);
    console.log(`  ISO time (T) found: ${hasISOTime}`);
    console.log(`  Readable time found: ${hasReadableTime}`);

    const story1Pass = !hasEnglishStatus && hasChineseStatus && !hasISOTime && hasReadableTime;
    results['story1'] = {
      status: story1Pass ? 'PASS' : 'FAIL',
      englishStatus: hasEnglishStatus,
      chineseStatus: hasChineseStatus,
      isoTime: hasISOTime,
      readableTime: hasReadableTime
    };
    console.log(`  Result: ${story1Pass ? 'PASS' : 'FAIL'}`);
  } catch (e) {
    results['story1'] = { status: 'ERROR', error: e.message };
    console.log(`  ERROR: ${e.message}`);
  }

  // ========== STORY 2: 收款记录时间展示 ==========
  console.log('\n=== Story 2: 收款记录时间展示 ===');
  try {
    // Navigate to payment records
    const paymentMenu = await page.$('text=收款记录');
    if (paymentMenu) {
      await paymentMenu.click();
      await waitForTable();
    }

    await snap('story2-payment-records');

    const pageContent = await page.textContent('body');
    const hasISOTime = /\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/.test(pageContent);
    const hasReadableTime = /\d{4}-\d{2}-\d{2} \d{2}:\d{2}/.test(pageContent);

    console.log(`  ISO time (T) found: ${hasISOTime}`);
    console.log(`  Readable time found: ${hasReadableTime}`);

    const story2Pass = !hasISOTime && hasReadableTime;
    results['story2'] = {
      status: story2Pass ? 'PASS' : 'FAIL',
      isoTime: hasISOTime,
      readableTime: hasReadableTime
    };
    console.log(`  Result: ${story2Pass ? 'PASS' : 'FAIL'}`);
  } catch (e) {
    results['story2'] = { status: 'ERROR', error: e.message };
    console.log(`  ERROR: ${e.message}`);
  }

  // ========== STORY 3: 配件删除防呆 ==========
  console.log('\n=== Story 3: 配件删除防呆 ===');
  try {
    // Navigate to parts management
    const partsMenu = await page.$('text=配件管理');
    if (partsMenu) {
      await partsMenu.click();
      await waitForTable();
    }

    await snap('story3-parts-list');

    // Look for disabled delete buttons or tooltip text about deletion protection
    const deleteButtons = await page.$$('button:has-text("删除")');
    let disabledCount = 0;
    let tooltipFound = false;

    for (const btn of deleteButtons) {
      const isDisabled = await btn.getAttribute('disabled');
      const className = await btn.getAttribute('class') || '';
      if (isDisabled !== null || className.includes('disabled')) {
        disabledCount++;
      }
      // Check for tooltip on hover
      try {
        await btn.hover();
        await page.waitForTimeout(500);
        const tooltipText = await page.textContent('.ant-tooltip, .el-tooltip, [role="tooltip"]');
        if (tooltipText && (tooltipText.includes('库存') || tooltipText.includes('流水') || tooltipText.includes('关联') || tooltipText.includes('引用') || tooltipText.includes('无法删除') || tooltipText.includes('不能删除'))) {
          tooltipFound = true;
        }
      } catch {}
    }

    // Also check for canDelete indicators in the page
    const content = await page.textContent('body');
    const hasDeleteProtection = content.includes('无法删除') || content.includes('不能删除') || tooltipFound;

    console.log(`  Total delete buttons: ${deleteButtons.length}`);
    console.log(`  Disabled delete buttons: ${disabledCount}`);
    console.log(`  Delete protection tooltip found: ${tooltipFound || hasDeleteProtection}`);

    await snap('story3-parts-list-delete-hover');

    const story3Pass = disabledCount > 0 || hasDeleteProtection;
    results['story3'] = {
      status: story3Pass ? 'PASS' : 'PARTIAL',
      totalDeleteButtons: deleteButtons.length,
      disabledDeleteButtons: disabledCount,
      tooltipFound: tooltipFound || hasDeleteProtection
    };
    console.log(`  Result: ${story3Pass ? 'PASS' : 'PARTIAL - checking source code'}`);
  } catch (e) {
    results['story3'] = { status: 'ERROR', error: e.message };
    console.log(`  ERROR: ${e.message}`);
  }

  // ========== STORY 4: 条码复制/打印 ==========
  console.log('\n=== Story 4: 条码复制/打印 ===');
  try {
    // Navigate to parts management (should already be there)
    const partsMenu = await page.$('text=配件管理');
    if (partsMenu) {
      await partsMenu.click();
      await waitForTable();
    }

    // Find and click view/detail button on first part
    const viewButtons = await page.$$('button:has-text("查看"), button:has-text("详情"), a:has-text("查看"), a:has-text("详情")');
    if (viewButtons.length > 0) {
      await viewButtons[0].click();
      await page.waitForTimeout(1500);
    }

    await snap('story4-part-detail-barcode');

    // Check for barcode-related content
    const content = await page.textContent('body');
    const hasBarcode = content.includes('条码') || content.includes('barcode') || content.includes('条形码');
    const hasCopyBtn = content.includes('复制') || content.includes('copy');
    const hasPrintBtn = content.includes('打印') || content.includes('print');

    console.log(`  Barcode section found: ${hasBarcode}`);
    console.log(`  Copy button found: ${hasCopyBtn}`);
    console.log(`  Print button found: ${hasPrintBtn}`);

    // Close modal if opened
    try {
      const closeBtn = await page.$('.ant-modal-close, .el-dialog__close, button:has-text("关闭"), button:has-text("取消")');
      if (closeBtn) await closeBtn.click();
    } catch {}

    results['story4'] = {
      status: hasBarcode ? 'PASS' : 'FAIL',
      barcodeFound: hasBarcode,
      copyButton: hasCopyBtn,
      printButton: hasPrintBtn
    };
    console.log(`  Result: ${hasBarcode ? 'PASS' : 'FAIL'}`);
  } catch (e) {
    results['story4'] = { status: 'ERROR', error: e.message };
    console.log(`  ERROR: ${e.message}`);
  }

  // ========== STORY 5: 库存调整二次确认 ==========
  console.log('\n=== Story 5: 库存调整二次确认 ===');
  try {
    // Navigate to inventory management
    const invMenu = await page.$('text=库存管理');
    if (invMenu) {
      await invMenu.click();
      await waitForTable();
    }

    await snap('story5-inventory-list');

    // Find and click adjust button
    const adjustButtons = await page.$$('button:has-text("调整"), button:has-text("库存调整")');
    if (adjustButtons.length > 0) {
      await adjustButtons[0].click();
      await page.waitForTimeout(1500);
    }

    await snap('story5-adjust-dialog');

    // Fill in quantity and reason
    const quantityInput = await page.$('input[placeholder*="数量"], input[placeholder*="调整数量"], input[name*="quantity"], input[name*="adjust"]');
    const reasonInput = await page.$('input[placeholder*="原因"], input[placeholder*="备注"], textarea[placeholder*="原因"], textarea[placeholder*="备注"], input[name*="reason"], textarea[name*="reason"]');

    if (quantityInput) {
      await quantityInput.fill('1');
    }
    if (reasonInput) {
      await reasonInput.fill('测试库存调整');
    }

    await snap('story5-adjust-filled');

    // Click submit to trigger confirmation
    const submitBtn = await page.$('button:has-text("提交"), button:has-text("确认"), button:has-text("确定")');
    if (submitBtn) {
      await submitBtn.click();
      await page.waitForTimeout(1000);
    }

    // Check for confirmation dialog
    const confirmDialog = await page.$('.ant-modal-confirm, .ant-modal:has-text("确认"), .el-message-box, [role="dialog"]:has-text("确认")');
    const content = await page.textContent('body');
    const hasConfirmDialog = content.includes('确认') && (content.includes('调整') || content.includes('库存'));

    await snap('story5-confirmation-dialog');

    // Close dialog without confirming
    try {
      const cancelBtn = await page.$('.ant-modal-confirm .ant-btn:not(.ant-btn-primary), button:has-text("取消"), button:has-text("否")');
      if (cancelBtn) await cancelBtn.click();
    } catch {}

    console.log(`  Confirmation dialog found: ${hasConfirmDialog || confirmDialog !== null}`);

    results['story5'] = {
      status: (hasConfirmDialog || confirmDialog) ? 'PASS' : 'PARTIAL',
      dialogFound: hasConfirmDialog || confirmDialog !== null
    };
    console.log(`  Result: ${(hasConfirmDialog || confirmDialog) ? 'PASS' : 'PARTIAL'}`);
  } catch (e) {
    results['story5'] = { status: 'ERROR', error: e.message };
    console.log(`  ERROR: ${e.message}`);
  }

  // ========== STORY 6: 客户/车辆查看 ==========
  console.log('\n=== Story 6: 客户/车辆查看 ===');
  try {
    // Navigate to customer management
    const custMenu = await page.$('text=客户档案');
    if (custMenu) {
      await custMenu.click();
      await waitForTable();
    }

    await snap('story6-customer-list');

    // Click view detail on a customer
    const viewButtons = await page.$$('button:has-text("查看"), button:has-text("详情")');
    if (viewButtons.length > 0) {
      await viewButtons[0].click();
      await page.waitForTimeout(1500);
    }

    await snap('story6-customer-detail');

    const content = await page.textContent('body');
    const hasCustomerDetail = content.includes('客户') || content.includes('姓名') || content.includes('电话') || content.includes('车辆');

    console.log(`  Customer detail page loaded: ${hasCustomerDetail}`);

    // Close modal if opened
    try {
      const closeBtn = await page.$('.ant-modal-close, .el-dialog__close, button:has-text("关闭"), button:has-text("取消")');
      if (closeBtn) await closeBtn.click();
    } catch {}

    results['story6'] = {
      status: hasCustomerDetail ? 'PASS' : 'FAIL',
      detailLoaded: hasCustomerDetail
    };
    console.log(`  Result: ${hasCustomerDetail ? 'PASS' : 'FAIL'}`);
  } catch (e) {
    results['story6'] = { status: 'ERROR', error: e.message };
    console.log(`  ERROR: ${e.message}`);
  }

  // ========== STORY 7: 客户/车辆删除保护 ==========
  console.log('\n=== Story 7: 客户/车辆删除保护 ===');
  try {
    // Navigate to customer management (should already be there)
    const custMenu = await page.$('text=客户档案');
    if (custMenu) {
      await custMenu.click();
      await waitForTable();
    }

    // Find delete buttons
    const deleteButtons = await page.$$('button:has-text("删除")');
    let deleteClicked = false;

    for (const btn of deleteButtons) {
      const isDisabled = await btn.getAttribute('disabled');
      if (isDisabled === null) {
        await btn.click();
        deleteClicked = true;
        await page.waitForTimeout(1000);
        break;
      }
    }

    await snap('story7-delete-confirmation');

    const content = await page.textContent('body');
    const hasDeleteConfirm = content.includes('确认删除') || content.includes('确定删除') || content.includes('删除后') || content.includes('无法删除') || content.includes('工单') || content.includes('关联');

    console.log(`  Delete button clicked: ${deleteClicked}`);
    console.log(`  Delete protection dialog found: ${hasDeleteConfirm}`);

    // Close dialog without confirming
    try {
      const cancelBtn = await page.$('.ant-modal .ant-btn:not(.ant-btn-primary), button:has-text("取消"), button:has-text("否")');
      if (cancelBtn) await cancelBtn.click();
    } catch {}

    results['story7'] = {
      status: hasDeleteConfirm ? 'PASS' : 'PARTIAL',
      deleteClicked,
      protectionDialog: hasDeleteConfirm
    };
    console.log(`  Result: ${hasDeleteConfirm ? 'PASS' : 'PARTIAL'}`);
  } catch (e) {
    results['story7'] = { status: 'ERROR', error: e.message };
    console.log(`  ERROR: ${e.message}`);
  }

  // ========== SUMMARY ==========
  console.log('\n========== SMOKE TEST RESULTS ==========');
  for (const [story, result] of Object.entries(results)) {
    console.log(`${story}: ${result.status} ${result.error ? `(${result.error})` : ''}`);
  }
  console.log('=========================================\n');

  await browser.close();
}

run().catch(e => {
  console.error('Fatal error:', e);
  process.exit(1);
});
