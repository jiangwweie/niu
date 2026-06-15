#!/usr/bin/env node

import fs from 'node:fs';
import path from 'node:path';
import playwright from '../../admin-web/node_modules/playwright/index.js';

const { chromium } = playwright;

const repoRoot = path.resolve(path.dirname(new URL(import.meta.url).pathname), '..', '..');
const baseUrl = process.env.ADMIN_WEB_BASE_URL || 'https://admin.qytech.online';
const outDir = process.env.ADMIN_ACCEPTANCE_OUT_DIR ||
  path.join(repoRoot, 'output', 'acceptance', '2026-06-15', 'admin-web');
const roleLabel = process.env.ADMIN_ACCEPTANCE_ROLE || 'store-admin';
const tokenFile = process.env.ADMIN_WEB_TOKEN_FILE || '';
const token = process.env.ADMIN_WEB_TOKEN || (tokenFile ? fs.readFileSync(tokenFile, 'utf8').trim() : '');
const pageTimeoutMs = Number(process.env.ADMIN_ACCEPTANCE_PAGE_TIMEOUT_MS || 15000);

const routes = [
  { name: 'dashboard', path: '/dashboard' },
  { name: 'work-order', path: '/work-order' },
  { name: 'customers', path: '/customers' },
  { name: 'vehicles', path: '/vehicles' },
  { name: 'parts', path: '/parts' },
  { name: 'inventory', path: '/inventory' },
  { name: 'payment', path: '/payment' },
  { name: 'refund', path: '/refund' },
  { name: 'settlement', path: '/settlement' },
  { name: 'reimbursement', path: '/reimbursement' },
  { name: 'finance', path: '/finance' },
  { name: 'cashier-report', path: '/finance/cashier-report' },
  { name: 'user', path: '/user' },
  { name: 'store', path: '/store' },
  { name: 'export', path: '/export' },
  { name: 'trial-data', path: '/trial-data' },
];

function ensureDir(dir) {
  fs.mkdirSync(dir, { recursive: true });
}

function safeJson(value) {
  return JSON.stringify(value, null, 2);
}

function normalizeText(value) {
  return String(value || '').replace(/\s+/g, ' ').trim();
}

async function withTimeout(promise, timeoutMs, label) {
  let timeoutId;
  const timeout = new Promise((_, reject) => {
    timeoutId = setTimeout(() => reject(new Error(`${label} timed out after ${timeoutMs}ms`)), timeoutMs);
  });
  return Promise.race([promise, timeout]).finally(() => clearTimeout(timeoutId));
}

async function collectPage(page) {
  return await page.evaluate(() => {
    const text = (selector) => Array.from(document.querySelectorAll(selector))
      .map((item) => item.textContent || '')
      .join(' ')
      .replace(/\s+/g, ' ')
      .trim();

    return {
      title: document.title,
      url: location.href,
      bodyText: (document.body.innerText || '').replace(/\s+/g, ' ').trim().slice(0, 6000),
      menuText: text('.el-menu'),
      h1Text: text('h1'),
      h2Text: text('h2'),
      buttonText: text('button,.el-button').slice(0, 3000),
      tableHeaderText: text('th,.el-table__header').slice(0, 3000),
      emptyText: text('.el-empty,.empty,.empty-state'),
      alertText: text('.el-alert,.alert,.notice,.tip').slice(0, 3000),
      dialogText: text('.el-dialog,.el-drawer').slice(0, 3000),
    };
  });
}

async function captureRoute(page, route, index, screenshotDir, dataDir) {
  console.log(`[admin:capture:start] ${route.name} ${route.path}`);
  const consoleMessages = [];
  const requestFailures = [];

  const onConsole = (msg) => {
    if (['error', 'warning'].includes(msg.type())) {
      consoleMessages.push({ type: msg.type(), text: msg.text().slice(0, 1000) });
    }
  };
  const onRequestFailed = (request) => {
    requestFailures.push({
      url: request.url(),
      method: request.method(),
      failure: request.failure()?.errorText || '',
    });
  };

  page.on('console', onConsole);
  page.on('requestfailed', onRequestFailed);

  const fileBase = `${String(index + 1).padStart(2, '0')}-${route.name}`;
  try {
    await page.goto(new URL(route.path, baseUrl).toString(), { waitUntil: 'networkidle', timeout: pageTimeoutMs });
    await page.waitForTimeout(800);

    const pageData = await collectPage(page);
    await page.screenshot({ path: path.join(screenshotDir, `${fileBase}.png`), fullPage: true });

    const result = {
      name: route.name,
      requestedPath: route.path,
      status: 'ok',
      currentUrl: page.url(),
      screenshot: path.join(screenshotDir, `${fileBase}.png`),
      data: path.join(dataDir, `${fileBase}.json`),
      consoleMessages,
      requestFailures,
      page: pageData,
    };

    fs.writeFileSync(result.data, safeJson(result), 'utf8');
    console.log(`[admin:capture:ok] ${route.name}`);
    return result;
  } catch (error) {
    const result = {
      name: route.name,
      requestedPath: route.path,
      status: 'failed',
      currentUrl: page.url(),
      error: error && error.message ? error.message : String(error),
      consoleMessages,
      requestFailures,
    };
    fs.writeFileSync(path.join(dataDir, `${fileBase}.json`), safeJson(result), 'utf8');
    console.error(`[admin:capture:failed] ${route.name}: ${result.error}`);
    return result;
  } finally {
    page.off('console', onConsole);
    page.off('requestfailed', onRequestFailed);
  }
}

async function main() {
  if (!token) {
    throw new Error('ADMIN_WEB_TOKEN or ADMIN_WEB_TOKEN_FILE is required');
  }

  const roleOutDir = path.join(outDir, roleLabel);
  const screenshotDir = path.join(roleOutDir, 'screenshots');
  const dataDir = path.join(roleOutDir, 'page-data');
  ensureDir(screenshotDir);
  ensureDir(dataDir);

  const browser = await chromium.launch({ headless: true });
  const context = await browser.newContext({
    viewport: { width: 1440, height: 1100 },
    deviceScaleFactor: 1,
  });

  await context.addInitScript((value) => {
    localStorage.setItem('accessToken', value);
  }, token);

  const page = await context.newPage();
  const summary = {
    generatedAt: new Date().toISOString(),
    baseUrl,
    roleLabel,
    auth: { hasAccessToken: true },
    pages: [],
  };

  try {
    for (let i = 0; i < routes.length; i += 1) {
      const result = await withTimeout(
        captureRoute(page, routes[i], i, screenshotDir, dataDir),
        pageTimeoutMs + 5000,
        routes[i].name,
      ).catch((error) => ({
        name: routes[i].name,
        requestedPath: routes[i].path,
        status: 'failed',
        error: error && error.message ? error.message : String(error),
      }));
      summary.pages.push({
        name: result.name,
        requestedPath: result.requestedPath,
        status: result.status,
        currentUrl: result.currentUrl,
        screenshot: result.screenshot,
        data: result.data,
        error: result.error,
      });
    }
  } finally {
    await browser.close();
  }

  fs.writeFileSync(path.join(roleOutDir, 'admin-web-product-summary.json'), safeJson(summary), 'utf8');
  console.log(safeJson(summary));
}

main().catch((error) => {
  console.error(error && error.stack ? error.stack : error);
  process.exit(1);
});
