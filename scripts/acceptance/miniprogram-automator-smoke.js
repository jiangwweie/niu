#!/usr/bin/env node

const fs = require('fs');
const path = require('path');

const repoRoot = path.resolve(__dirname, '..', '..');
const defaultOutDir = path.join(repoRoot, 'output', 'acceptance', '2026-06-15');
const outDir = process.env.MINI_ACCEPTANCE_OUT_DIR || defaultOutDir;
const screenshotDir = path.join(outDir, 'screenshots');
const dataDir = path.join(outDir, 'page-data');

const automatorModule =
  process.env.MINIPROGRAM_AUTOMATOR_MODULE ||
  '/tmp/niu-qa-tools/node_modules/miniprogram-automator';
const wsEndpoint = process.env.MINIPROGRAM_WS_ENDPOINT || 'ws://127.0.0.1:19420';
const pageTimeoutMs = Number(process.env.MINI_ACCEPTANCE_PAGE_TIMEOUT_MS || 10000);
const accessTokenFile = process.env.MINI_ACCESS_TOKEN_FILE || '';
const authUserFile = process.env.MINI_AUTH_USER_FILE || '';
const roleLabel = process.env.MINI_ACCEPTANCE_ROLE || 'current-session';

const pages = [
  { name: 'dashboard', route: '/pages/dashboard/index', tab: true },
  { name: 'work-orders', route: '/pages/work-orders/index', tab: true },
  { name: 'inventory', route: '/pages/inventory/index', tab: true },
  { name: 'mine', route: '/pages/mine/index', tab: true },
  { name: 'parts', route: '/pages/parts/index' },
  { name: 'part-detail', route: '/pages/part-detail/index?id=8' },
  { name: 'inbound', route: '/pages/inbound-placeholder/index' },
  { name: 'create-work-order', route: '/pages/create-work-order-placeholder/index' },
  { name: 'cashier-workbench', route: '/pages/payment-placeholder/index' },
  { name: 'reimbursement', route: '/pages/reimbursement-placeholder/index' },
];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function safeJson(value) {
  return JSON.stringify(value, null, 2);
}

async function capturePage(miniProgram, pageSpec, index) {
  console.log(`[capture:start] ${pageSpec.name} ${pageSpec.route}`);
  const page = pageSpec.tab
    ? await miniProgram.switchTab(pageSpec.route)
    : await miniProgram.reLaunch(pageSpec.route);
  await page.waitFor(1200);

  const currentPage = await miniProgram.currentPage();
  const data = await currentPage.data();
  const pageStack = await miniProgram.pageStack();
  const fileBase = `${String(index + 1).padStart(2, '0')}-${pageSpec.name}`;

  await miniProgram.screenshot({
    path: path.join(screenshotDir, `${fileBase}.png`),
  });

  fs.writeFileSync(
    path.join(dataDir, `${fileBase}.json`),
    safeJson({
      requestedRoute: pageSpec.route,
      currentRoute: currentPage.path,
      pageStack: pageStack.map((item) => ({ path: item.path, query: item.query })),
      data,
    }),
    'utf8',
  );

  return {
    name: pageSpec.name,
    requestedRoute: pageSpec.route,
    currentRoute: currentPage.path,
    screenshot: path.join(screenshotDir, `${fileBase}.png`),
    data: path.join(dataDir, `${fileBase}.json`),
  };
}

function withTimeout(promise, timeoutMs, label) {
  let timeoutId;
  const timeout = new Promise((_, reject) => {
    timeoutId = setTimeout(() => {
      reject(new Error(`${label} timed out after ${timeoutMs}ms`));
    }, timeoutMs);
  });

  return Promise.race([promise, timeout]).finally(() => clearTimeout(timeoutId));
}

async function main() {
  ensureDir(screenshotDir);
  ensureDir(dataDir);

  const automator = require(automatorModule);
  const miniProgram = await automator.connect({ wsEndpoint });

  if (accessTokenFile) {
    const token = fs.readFileSync(accessTokenFile, 'utf8').trim();
    await miniProgram.callWxMethod('setStorageSync', 'accessToken', token);
  }
  if (authUserFile) {
    const user = fs.readFileSync(authUserFile, 'utf8').trim();
    await miniProgram.callWxMethod('setStorageSync', 'auth_user', user);
  }

  const authUser = await miniProgram.callWxMethod('getStorageSync', 'auth_user');
  const authToken = await miniProgram.callWxMethod('getStorageSync', 'accessToken');
  const summary = {
    generatedAt: new Date().toISOString(),
    wsEndpoint,
    roleLabel,
    auth: {
      hasAccessToken: Boolean(authToken),
      user: authUser ? JSON.parse(authUser) : null,
    },
    pages: [],
  };

  try {
    for (let i = 0; i < pages.length; i += 1) {
      try {
        const result = await withTimeout(
          capturePage(miniProgram, pages[i], i),
          pageTimeoutMs,
          pages[i].name,
        );
        summary.pages.push({ ...result, status: 'ok' });
        console.log(`[capture:ok] ${pages[i].name}`);
      } catch (error) {
        const message = error && error.message ? error.message : String(error);
        summary.pages.push({
          name: pages[i].name,
          requestedRoute: pages[i].route,
          status: 'failed',
          error: message,
        });
        console.error(`[capture:failed] ${pages[i].name}: ${message}`);
      }
    }
  } finally {
    miniProgram.disconnect();
  }

  fs.writeFileSync(path.join(outDir, 'miniprogram-smoke-summary.json'), safeJson(summary), 'utf8');
  console.log(safeJson(summary));
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
