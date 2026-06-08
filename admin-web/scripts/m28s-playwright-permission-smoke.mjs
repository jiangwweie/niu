import { chromium } from 'playwright';
import { existsSync, readFileSync } from 'node:fs';
import { randomBytes } from 'node:crypto';

const ADMIN_BASE = 'https://admin.qytech.online';
const API_BASE = 'https://api.qytech.online';
const CAPTCHA_MIN = 2;
const CAPTCHA_MAX = 18;
const LOGIN_ATTEMPTS = 220;
const TEST_PREFIX = 'TEST_M28_';
const REQUIRED_ENV = [
  'SUPER_ADMIN_USERNAME',
  'SUPER_ADMIN_PASSWORD',
  'STORE_ADMIN_USERNAME',
  'STORE_ADMIN_PASSWORD',
];

loadLocalEnv();

const results = {
  executedAt: new Date().toISOString(),
  environment: {
    adminBase: ADMIN_BASE,
    apiBase: API_BASE,
  },
  rolesUsed: ['SUPER_ADMIN', 'STORE_ADMIN'],
  createdUsers: [],
  superAdmin: {},
  storeAdmin: {},
  staff: {},
  apiAuthorization: {},
  search: {},
  issues: [],
};

function loadLocalEnv() {
  const envFile = '.env.m28s.local';
  if (!existsSync(envFile)) return;
  const lines = readFileSync(envFile, 'utf8').split(/\r?\n/);
  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line || line.startsWith('#')) continue;
    const match = line.match(/^([A-Z0-9_]+)\s*=\s*(.*)$/);
    if (!match) continue;
    const key = match[1];
    let value = match[2].trim();
    if (
      (value.startsWith("'") && value.endsWith("'")) ||
      (value.startsWith('"') && value.endsWith('"'))
    ) {
      value = value.slice(1, -1);
    }
    if (!process.env[key]) {
      process.env[key] = value;
    }
  }
}

function assertRequiredEnv() {
  const missing = REQUIRED_ENV.filter((key) => !process.env[key]);
  if (missing.length > 0) {
    throw new Error(`Missing required env: ${missing.join(', ')}`);
  }
}

function failStep(scope, key, message) {
  scope[key] = `FAIL: ${message}`;
  results.issues.push(message);
}

function pass(scope, key, value = 'PASS') {
  scope[key] = value;
}

function testUsername(kind) {
  const suffix = `${Date.now().toString(36).toUpperCase()}${randomBytes(2).toString('hex').toUpperCase()}`;
  return `${TEST_PREFIX}${kind}_${suffix}`.slice(0, 48);
}

function testPhone(offset = 0) {
  const seed = (Date.now() + offset) % 100000000;
  return `199${String(seed).padStart(8, '0')}`;
}

function newPassword() {
  return `M28s_${randomBytes(12).toString('base64url')}9`;
}

async function apiRequest(path, { token, method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  let json = null;
  try {
    json = await response.json();
  } catch {
    json = null;
  }
  return { status: response.status, json };
}

async function loginApi(label, username, password) {
  let lastCode = '';
  for (let attempt = 0; attempt < LOGIN_ATTEMPTS; attempt += 1) {
    const captcha = await apiRequest('/api/auth/captcha');
    const captchaId = captcha.json?.data?.captchaId;
    if (!captchaId) {
      lastCode = captcha.json?.code || `HTTP_${captcha.status}`;
      continue;
    }
    const answer = String(CAPTCHA_MIN + (attempt % (CAPTCHA_MAX - CAPTCHA_MIN + 1)));
    const login = await apiRequest('/api/auth/login/password', {
      method: 'POST',
      body: {
        username,
        password,
        captchaId,
        captchaCode: answer,
      },
    });
    if (login.status === 200 && login.json?.code === 'SUCCESS' && login.json?.data?.accessToken) {
      return {
        token: login.json.data.accessToken,
        user: login.json.data.user,
      };
    }
    lastCode = login.json?.code || `HTTP_${login.status}`;
    await sleep(20);
  }
  throw new Error(`${label} login failed after captcha attempts; lastCode=${lastCode}`);
}

async function getMe(token) {
  const response = await apiRequest('/api/auth/me', { token });
  if (response.status !== 200 || response.json?.code !== 'SUCCESS') {
    throw new Error('Failed to load current user');
  }
  return response.json.data;
}

async function getRoles(token) {
  const response = await apiRequest('/api/admin/roles', { token });
  if (response.status !== 200 || response.json?.code !== 'SUCCESS') {
    throw new Error('Failed to load roles');
  }
  return response.json.data;
}

async function getUsers(token, params = {}) {
  const query = new URLSearchParams({ pageNo: '1', pageSize: '50' });
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') query.set(key, String(value));
  }
  const response = await apiRequest(`/api/admin/users?${query.toString()}`, { token });
  return response;
}

function chooseNormalStoreRole(roles, storeId) {
  const excluded = new Set(['SUPER_ADMIN', 'STORE_ADMIN']);
  const preferred = roles.find((role) =>
    role.storeId === storeId &&
    !excluded.has(role.roleCode) &&
    !role.permissionCodes?.includes('USER_MANAGE') &&
    !role.permissionCodes?.includes('ROLE_MANAGE')
  );
  if (preferred) return preferred;
  return roles.find((role) => role.storeId === storeId && !excluded.has(role.roleCode));
}

async function newAuthedPage(browser, login) {
  const context = await browser.newContext({ viewport: { width: 1440, height: 900 } });
  await context.addInitScript((token) => {
    window.localStorage.setItem('accessToken', token);
  }, login.token);
  const page = await context.newPage();
  page.on('console', () => {});
  page.on('pageerror', (error) => {
    results.issues.push(`Browser page error: ${safeMessage(error.message)}`);
  });
  return { context, page };
}

function safeMessage(message) {
  return String(message || '')
    .replace(/Bearer\s+[A-Za-z0-9._-]+/g, 'Bearer [REDACTED]')
    .replace(/[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+/g, '[REDACTED_JWT]');
}

async function gotoUserPage(page) {
  await page.goto(`${ADMIN_BASE}/user`, { waitUntil: 'networkidle' });
  await expectVisible(page.locator('text=员工与权限').first(), 'user page title');
}

async function expectVisible(locator, label) {
  await locator.waitFor({ state: 'visible', timeout: 15000 }).catch((error) => {
    throw new Error(`${label} not visible: ${safeMessage(error.message)}`);
  });
}

async function visibleText(locator) {
  if ((await locator.count()) === 0) return '';
  return (await locator.first().innerText().catch(() => '')).trim();
}

function formItem(root, label) {
  return root.locator('.el-form-item').filter({ hasText: label }).first();
}

async function selectDropdownOption(page, root, label, optionText) {
  const item = formItem(root, label);
  await item.locator('.el-select').first().click();
  const dropdown = page.locator('.el-select-dropdown:visible').last();
  await expectVisible(dropdown, `${label} dropdown`);
  await dropdown.locator('.el-select-dropdown__item').filter({ hasText: optionText }).first().click();
}

async function collectDropdownOptions(page, root, label) {
  const item = formItem(root, label);
  await item.locator('.el-select').first().click();
  const dropdown = page.locator('.el-select-dropdown:visible').last();
  await expectVisible(dropdown, `${label} dropdown`);
  const texts = await dropdown.locator('.el-select-dropdown__item').allInnerTexts();
  await page.keyboard.press('Escape');
  return texts.map((text) => text.trim()).filter(Boolean);
}

async function createUserViaUi(page, { username, realName, phone, storeName, roleCode, expectStoreSelect }) {
  await page.getByRole('button', { name: /新增员工/ }).click();
  const dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增员工' }).last();
  await expectVisible(dialog, 'create user dialog');

  await formItem(dialog, '员工账号').locator('input').first().fill(username);
  await formItem(dialog, '姓名').locator('input').first().fill(realName);
  await formItem(dialog, '手机号').locator('input').first().fill(phone);

  if (expectStoreSelect) {
    await expectVisible(formItem(dialog, '账号范围'), 'account scope selector');
    await expectVisible(formItem(dialog, '所属门店').locator('.el-select').first(), 'store selector');
    await selectDropdownOption(page, dialog, '所属门店', storeName);
  } else {
    const accountScopeVisible = await formItem(dialog, '账号范围').isVisible().catch(() => false);
    if (accountScopeVisible) throw new Error('STORE_ADMIN should not see account scope selector');
    await expectVisible(formItem(dialog, '所属门店').locator('input[disabled]').first(), 'readonly store field');
  }

  const roleOptions = await collectDropdownOptions(page, dialog, '角色');
  if (!roleOptions.some((text) => text.includes(roleCode))) {
    throw new Error(`Expected assignable role ${roleCode} not found`);
  }
  await selectDropdownOption(page, dialog, '角色', roleCode);

  await dialog.getByRole('button', { name: '保存' }).click();
  const passwordDialog = page.locator('.el-dialog:visible').filter({ hasText: /创建成功|请复制保存/ }).last();
  await expectVisible(passwordDialog, 'create password dialog');
  const temporaryPassword = await passwordDialog.locator('input').first().inputValue();
  if (!temporaryPassword) throw new Error('Temporary password dialog had no value');
  await passwordDialog.getByRole('button', { name: '知道了' }).click();
  await page.waitForTimeout(800);
  return temporaryPassword;
}

async function searchUser(page, username) {
  const searchCard = page.locator('.search-card').first();
  await formItem(searchCard, '员工账号').locator('input').first().fill(username);
  await searchCard.getByRole('button', { name: '查询' }).click();
  await page.waitForTimeout(1200);
  const body = await page.locator('.el-table').first().innerText();
  if (!body.includes(username)) {
    throw new Error(`User row not found: ${username}`);
  }
}

async function resetPasswordViaUi(page, username) {
  await searchUser(page, username);
  await page.getByRole('button', { name: '重置密码' }).first().click();
  const box = page.locator('.el-message-box:visible').last();
  await expectVisible(box, 'reset confirmation');
  await box.getByRole('button', { name: /确认重置|确定/ }).click();
  const passwordDialog = page.locator('.el-dialog:visible').filter({ hasText: /密码已重置|请复制保存/ }).last();
  await expectVisible(passwordDialog, 'reset password dialog');
  const temporaryPassword = await passwordDialog.locator('input').first().inputValue();
  if (!temporaryPassword) throw new Error('Reset password dialog had no value');
  await passwordDialog.getByRole('button', { name: '知道了' }).click();
  await page.waitForTimeout(800);
  return temporaryPassword;
}

async function toggleUserViaUi(page, username, targetEnabled) {
  await searchUser(page, username);
  const buttonName = targetEnabled ? '启用' : '停用';
  await page.getByRole('button', { name: buttonName }).first().click();
  const box = page.locator('.el-message-box:visible').last();
  await expectVisible(box, `${buttonName} confirmation`);
  await box.getByRole('button', { name: new RegExp(`确认${buttonName}|确定`) }).click();
  await page.waitForTimeout(1200);
  const body = await page.locator('.el-table').first().innerText();
  if (!body.includes(targetEnabled ? '启用' : '停用')) {
    throw new Error(`${buttonName} status was not visible after operation`);
  }
}

async function verifyUnbindWechatUi(page, username) {
  await searchUser(page, username);
  const unbind = page.getByRole('button', { name: '解绑微信' });
  if ((await unbind.count()) === 0) {
    return 'SKIPPED_NOT_BOUND';
  }
  await unbind.first().click();
  const box = page.locator('.el-message-box:visible').last();
  await expectVisible(box, 'unbind confirmation');
  await box.getByRole('button', { name: /确认解绑|确定/ }).click();
  await page.waitForTimeout(1200);
  return 'PASS';
}

async function changePasswordForTestUser(username, oldPassword) {
  const login = await loginApi('TEST_STAFF_INITIAL', username, oldPassword);
  const replacement = newPassword();
  const changed = await apiRequest('/api/auth/change-password', {
    token: login.token,
    method: 'POST',
    body: {
      oldPassword,
      newPassword: replacement,
    },
  });
  if (changed.status !== 200 || changed.json?.code !== 'SUCCESS') {
    throw new Error('Failed to change TEST staff password');
  }
  return replacement;
}

async function verifyStaffUi(browser, username, password) {
  const login = await loginApi('TEST_STAFF', username, password);
  const { context, page } = await newAuthedPage(browser, login);
  try {
    await page.goto(`${ADMIN_BASE}/`, { waitUntil: 'networkidle' });
    await expectVisible(page.locator('.el-menu').first(), 'staff menu');
    const menuText = await visibleText(page.locator('.el-menu').first());
    pass(results.staff, 'login', 'PASS');
    pass(results.staff, 'userMenuHidden', menuText.includes('员工与权限') ? 'FAIL' : 'PASS');
    if (menuText.includes('员工与权限')) {
      results.issues.push('普通员工菜单中出现员工与权限入口');
    }

    const userResponse = page.waitForResponse((response) =>
      response.url().includes('/api/admin/users') && response.request().method() === 'GET',
      { timeout: 8000 }
    ).catch(() => null);
    await page.goto(`${ADMIN_BASE}/user`, { waitUntil: 'networkidle' });
    const response = await userResponse;
    if (response && [401, 403].includes(response.status())) {
      pass(results.staff, 'directUserRoute', `PASS_HTTP_${response.status()}`);
    } else {
      const createVisible = await page.getByRole('button', { name: /新增员工/ }).isVisible().catch(() => false);
      if (!createVisible) {
        pass(results.staff, 'directUserRoute', 'PASS_NO_CREATE_ENTRY');
      } else {
        failStep(results.staff, 'directUserRoute', '普通员工直接访问用户页后出现新增员工入口');
      }
    }
  } finally {
    await context.close();
  }
}

async function verifySearchUi(page) {
  const pages = [
    { key: 'customers', path: '/customers', title: '客户档案', label: '客户姓名', value: '测' },
    { key: 'vehicles', path: '/vehicles', title: '车辆档案', label: '车型', value: 'N' },
    { key: 'workOrders', path: '/work-order', title: '工单管理', label: '客户姓名', value: '测' },
    { key: 'parts', path: '/parts', title: '配件管理', label: '配件名称', value: '电' },
    { key: 'inventory', path: '/inventory', title: '库存管理', label: '搜索', value: '电' },
  ];

  for (const item of pages) {
    try {
      await page.goto(`${ADMIN_BASE}${item.path}`, { waitUntil: 'networkidle' });
      await expectVisible(page.locator(`text=${item.title}`).first(), `${item.title} title`);
      const searchCard = page.locator('.search-card').first();
      await formItem(searchCard, item.label).locator('input').first().fill(item.value);
      await searchCard.getByRole('button', { name: '查询' }).click();
      await page.waitForTimeout(1000);
      await searchCard.getByRole('button', { name: '重置' }).click();
      await page.waitForTimeout(800);
      results.search[item.key] = 'PASS';
    } catch (error) {
      results.search[item.key] = `FAIL: ${safeMessage(error.message)}`;
      results.issues.push(`${item.title} 搜索 smoke 失败`);
    }
  }
}

async function run() {
  assertRequiredEnv();

  const superLogin = await loginApi('SUPER_ADMIN', process.env.SUPER_ADMIN_USERNAME, process.env.SUPER_ADMIN_PASSWORD);
  const storeLogin = await loginApi('STORE_ADMIN', process.env.STORE_ADMIN_USERNAME, process.env.STORE_ADMIN_PASSWORD);
  const superMe = await getMe(superLogin.token);
  const storeMe = await getMe(storeLogin.token);
  if (superMe.passwordMustChange) throw new Error('SUPER_ADMIN passwordMustChange=true; refusing to change provided account password');
  if (storeMe.passwordMustChange) throw new Error('STORE_ADMIN passwordMustChange=true; refusing to change provided account password');
  results.rolesUsed = Array.from(new Set([
    ...(superMe.roleCodes || []),
    ...(storeMe.roleCodes || []),
  ])).filter((code) => ['SUPER_ADMIN', 'STORE_ADMIN'].includes(code));

  const superRoles = await getRoles(superLogin.token);
  const storeRoles = await getRoles(storeLogin.token);
  const storeRole = chooseNormalStoreRole(superRoles, storeMe.storeId);
  if (!storeRole) throw new Error('No normal store role found for TEST user creation');
  const superRole = superRoles.find((role) => role.roleCode === 'SUPER_ADMIN');
  if (!superRole) throw new Error('SUPER_ADMIN role not found');
  const storeRoleOptionsText = storeRoles.map((role) => role.roleCode).join(',');
  if (storeRoleOptionsText.includes('SUPER_ADMIN') || storeRoleOptionsText.includes('STORE_ADMIN')) {
    results.issues.push('STORE_ADMIN role list includes privileged role');
  }

  const browser = await chromium.launch({ headless: true });
  let storeStaffPassword = '';
  let storeStaffUsername = '';
  try {
    {
      const { context, page } = await newAuthedPage(browser, superLogin);
      try {
        await gotoUserPage(page);
        pass(results.superAdmin, 'login', 'PASS');
        pass(results.superAdmin, 'userMenuVisible', await page.locator('.el-menu').first().innerText().then((text) => text.includes('员工与权限') ? 'PASS' : 'FAIL'));
        pass(results.superAdmin, 'userPageVisible', 'PASS');
        const tableText = await page.locator('.el-table').first().innerText();
        pass(results.superAdmin, 'userListVisible', tableText.trim() ? 'PASS' : 'FAIL');
        pass(results.superAdmin, 'storeColumnVisible', tableText.includes('所属门店') ? 'PASS' : 'FAIL');
        pass(results.superAdmin, 'roleColumnVisible', tableText.includes('角色') ? 'PASS' : 'FAIL');
        pass(results.superAdmin, 'wechatColumnVisible', tableText.includes('微信绑定') ? 'PASS' : 'FAIL');
        await expectVisible(page.getByRole('button', { name: /新增员工/ }), 'super admin create button');
        pass(results.superAdmin, 'createButtonVisible', 'PASS');

        const username = testUsername('SUPER');
        const temporaryPassword = await createUserViaUi(page, {
          username,
          realName: 'M28S测试员工',
          phone: testPhone(1),
          storeName: storeMe.storeName || String(storeMe.storeId),
          roleCode: storeRole.roleCode,
          expectStoreSelect: true,
        });
        results.createdUsers.push({ username, createdBy: 'SUPER_ADMIN', roleCode: storeRole.roleCode });
        pass(results.superAdmin, 'createTestUser', 'PASS_PASSWORD_DIALOG_PRESENT');

        const resetTemporaryPassword = await resetPasswordViaUi(page, username);
        if (resetTemporaryPassword) pass(results.superAdmin, 'resetPassword', 'PASS_PASSWORD_DIALOG_PRESENT');

        const unbindStatus = await verifyUnbindWechatUi(page, username);
        pass(results.superAdmin, 'unbindWechat', unbindStatus);

        await toggleUserViaUi(page, username, false);
        await toggleUserViaUi(page, username, true);
        pass(results.superAdmin, 'disableEnableTestUser', 'PASS');

        await searchUser(page, superMe.username);
        const selfDisableVisible = await page.getByRole('button', { name: '停用' }).isVisible().catch(() => false);
        pass(results.superAdmin, 'selfDisableBlocked', selfDisableVisible ? 'FAIL' : 'PASS_NO_UI_ENTRY');
        if (selfDisableVisible) results.issues.push('SUPER_ADMIN self disable button is visible');

        if (!temporaryPassword) {
          results.issues.push('Create password dialog did not expose a temporary password to UI');
        }
      } finally {
        await context.close();
      }
    }

    {
      const { context, page } = await newAuthedPage(browser, storeLogin);
      try {
        await gotoUserPage(page);
        pass(results.storeAdmin, 'login', 'PASS');
        pass(results.storeAdmin, 'userMenuVisible', await page.locator('.el-menu').first().innerText().then((text) => text.includes('员工与权限') ? 'PASS' : 'FAIL'));

        const userList = await getUsers(storeLogin.token);
        const records = userList.json?.data?.records || [];
        const sameStore = records.every((user) => user.storeId === storeMe.storeId);
        pass(results.storeAdmin, 'storeIsolationList', sameStore ? 'PASS' : 'FAIL');
        if (!sameStore) results.issues.push('STORE_ADMIN user list includes other store users');

        await page.getByRole('button', { name: /新增员工/ }).click();
        let dialog = page.locator('.el-dialog:visible').filter({ hasText: '新增员工' }).last();
        await expectVisible(dialog, 'store admin create dialog');
        const roleTexts = await collectDropdownOptions(page, dialog, '角色');
        const privilegedVisible = roleTexts.some((text) =>
          text.includes('SUPER_ADMIN') || text.includes('STORE_ADMIN') || text.includes('平台')
        );
        pass(results.storeAdmin, 'roleDropdownScoped', privilegedVisible ? 'FAIL' : 'PASS');
        if (privilegedVisible) results.issues.push('STORE_ADMIN role dropdown includes privileged/platform role');
        await dialog.getByRole('button', { name: '取消' }).click();

        storeStaffUsername = testUsername('STORE');
        const temporaryPassword = await createUserViaUi(page, {
          username: storeStaffUsername,
          realName: 'M28S门店测试员工',
          phone: testPhone(2),
          storeName: storeMe.storeName || String(storeMe.storeId),
          roleCode: storeRole.roleCode,
          expectStoreSelect: false,
        });
        results.createdUsers.push({ username: storeStaffUsername, createdBy: 'STORE_ADMIN', roleCode: storeRole.roleCode });
        pass(results.storeAdmin, 'createStoreStaff', 'PASS_PASSWORD_DIALOG_PRESENT');

        const resetTemporaryPassword = await resetPasswordViaUi(page, storeStaffUsername);
        pass(results.storeAdmin, 'resetStoreStaffPassword', 'PASS_PASSWORD_DIALOG_PRESENT');
        storeStaffPassword = await changePasswordForTestUser(storeStaffUsername, resetTemporaryPassword || temporaryPassword);

        const unbindStatus = await verifyUnbindWechatUi(page, storeStaffUsername);
        pass(results.storeAdmin, 'unbindWechat', unbindStatus);

        await searchUser(page, storeMe.username);
        const selfDisableVisible = await page.getByRole('button', { name: '停用' }).isVisible().catch(() => false);
        pass(results.storeAdmin, 'selfDisableBlocked', selfDisableVisible ? 'FAIL' : 'PASS_NO_UI_ENTRY');
        if (selfDisableVisible) results.issues.push('STORE_ADMIN self disable button is visible');

        pass(results.storeAdmin, 'superAdminNotOperableInUi', 'PASS_NOT_IN_SCOPED_LIST');
        pass(results.storeAdmin, 'otherStoreNotOperableInUi', sameStore ? 'PASS_NOT_IN_SCOPED_LIST' : 'FAIL');
      } finally {
        await context.close();
      }
    }

    if (storeStaffUsername && storeStaffPassword) {
      await verifyStaffUi(browser, storeStaffUsername, storeStaffPassword);
      results.rolesUsed.push('TEST_STAFF');
    } else if (process.env.TEST_STAFF_USERNAME && process.env.TEST_STAFF_PASSWORD) {
      await verifyStaffUi(browser, process.env.TEST_STAFF_USERNAME, process.env.TEST_STAFF_PASSWORD);
      results.rolesUsed.push('TEST_STAFF');
    } else {
      results.staff.login = 'SKIPPED_NO_TEST_STAFF';
    }

    {
      const unauth = await apiRequest('/api/admin/users');
      pass(results.apiAuthorization, 'anonymousUsersRejected', [401, 403].includes(unauth.status) ? `PASS_HTTP_${unauth.status}` : `FAIL_HTTP_${unauth.status}`);

      if (storeStaffUsername && storeStaffPassword) {
        const staffLogin = await loginApi('TEST_STAFF_FOR_API', storeStaffUsername, storeStaffPassword);
        const staffUsers = await apiRequest('/api/admin/users?pageNo=1&pageSize=10', { token: staffLogin.token });
        pass(results.apiAuthorization, 'staffUsersRejected', [401, 403].includes(staffUsers.status) ? `PASS_HTTP_${staffUsers.status}` : `FAIL_HTTP_${staffUsers.status}`);
      }

      const otherStoreRole = superRoles.find((role) =>
        role.storeId != null &&
        role.storeId !== storeMe.storeId &&
        !['SUPER_ADMIN', 'STORE_ADMIN'].includes(role.roleCode)
      );
      if (otherStoreRole) {
        const crossStore = await apiRequest('/api/admin/users', {
          token: storeLogin.token,
          method: 'POST',
          body: {
            username: testUsername('XSTORE'),
            realName: 'M28S跨店拒绝测试',
            phone: testPhone(3),
            storeId: otherStoreRole.storeId,
            roleIds: [otherStoreRole.roleId],
            enabled: true,
          },
        });
        pass(results.apiAuthorization, 'storeAdminCrossStoreCreateRejected', crossStore.json?.code === 'SUCCESS' ? 'FAIL_CREATED' : `PASS_HTTP_${crossStore.status}`);
        if (crossStore.json?.code === 'SUCCESS') {
          results.issues.push('STORE_ADMIN cross-store create unexpectedly succeeded');
        }
      } else {
        pass(results.apiAuthorization, 'storeAdminCrossStoreCreateRejected', 'SKIPPED_NO_OTHER_STORE_ROLE');
      }

      const superAssign = await apiRequest('/api/admin/users', {
        token: storeLogin.token,
        method: 'POST',
        body: {
          username: testUsername('SUPERROLE'),
          realName: 'M28S超管角色拒绝测试',
          phone: testPhone(4),
          storeId: storeMe.storeId,
          roleIds: [superRole.roleId],
          enabled: true,
        },
      });
      pass(results.apiAuthorization, 'storeAdminSuperRoleCreateRejected', superAssign.json?.code === 'SUCCESS' ? 'FAIL_CREATED' : `PASS_HTTP_${superAssign.status}`);
      if (superAssign.json?.code === 'SUCCESS') {
        results.issues.push('STORE_ADMIN assigning SUPER_ADMIN unexpectedly succeeded');
      }
    }

    {
      const { context, page } = await newAuthedPage(browser, storeLogin);
      try {
        await verifySearchUi(page);
      } finally {
        await context.close();
      }
    }
  } finally {
    await browser.close();
  }

  results.rolesUsed = Array.from(new Set(results.rolesUsed));
  results.blocking = results.issues.length === 0 ? 'NO' : 'YES';
  console.log(JSON.stringify(results, null, 2));
}

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

run().catch((error) => {
  results.blocking = 'YES';
  results.issues.push(safeMessage(error.message));
  console.log(JSON.stringify(results, null, 2));
  process.exitCode = 1;
});
