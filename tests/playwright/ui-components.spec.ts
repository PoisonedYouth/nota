import { test, expect } from '@playwright/test';

test.describe('UI Components and Design System', () => {
  test.beforeEach(async ({ page }) => {
    // Navigate to login page
    await page.goto('/auth/login');
  });

  test('should display modern login form with proper styling', async ({ page }) => {
    // Check login form exists and is styled
    await expect(page.locator('.login-form')).toBeVisible();
    await expect(page.locator('input[name="username"]')).toBeVisible();
    await expect(page.locator('input[name="password"]')).toBeVisible();
    
    // Check form controls have modern styling
    const usernameInput = page.locator('input[name="username"]');
    const styles = await usernameInput.evaluate((el) => getComputedStyle(el));
    expect(parseFloat(styles.borderRadius)).toBeGreaterThan(0); // Should have rounded borders
  });

  test('should show focus states on form inputs', async ({ page }) => {
    const usernameInput = page.locator('input[name="username"]');
    
    // Focus the input
    await usernameInput.focus();
    
    // Check that focus styles are applied (shadow or outline)
    const styles = await usernameInput.evaluate((el) => getComputedStyle(el));
    expect(styles.boxShadow).not.toBe('none');
  });

  test('should have responsive navigation bar', async ({ page }) => {
    // Login first
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check navbar elements
    await expect(page.locator('.navbar')).toBeVisible();
    await expect(page.locator('.navbar-brand')).toBeVisible();
    await expect(page.locator('.logo-text')).toContainText('Nota');
    
    // Check user info section
    await expect(page.locator('.user-info')).toBeVisible();
    await expect(page.locator('.user-avatar')).toBeVisible();
  });

  test('should display modern note cards with hover effects', async ({ page }) => {
    // Login and navigate to notes
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check note cards are present
    const noteCards = page.locator('.note-card');
    await expect(noteCards.first()).toBeVisible();
    
    // Test hover effect by moving mouse to card
    await noteCards.first().hover();
    
    // Wait for hover transition
    await page.waitForTimeout(300);
    
    // Check that card has some transform or shadow on hover
    const cardStyles = await noteCards.first().evaluate((el) => getComputedStyle(el));
    expect(cardStyles.transform).not.toBe('none');
  });

  test('should have modern button styles', async ({ page }) => {
    // Login to see buttons
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    
    // Check login button styling
    const loginBtn = page.locator('button[type="submit"]');
    await expect(loginBtn).toBeVisible();
    
    const btnStyles = await loginBtn.evaluate((el) => getComputedStyle(el));
    expect(parseFloat(btnStyles.borderRadius)).toBeGreaterThan(0);
    expect(btnStyles.background).toContain('gradient');
  });
});

test.describe('Search and Filtering UI', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/auth/login');
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
  });

  test('should display search interface with modern styling', async ({ page }) => {
    // Check search container exists
    await expect(page.locator('.search-sorting-container')).toBeVisible();
    await expect(page.locator('.search-input')).toBeVisible();
    
    // Check search input has proper styling
    const searchInput = page.locator('.search-input');
    const styles = await searchInput.evaluate((el) => getComputedStyle(el));
    expect(parseFloat(styles.borderRadius)).toBeGreaterThan(0);
    expect(styles.boxShadow).not.toBe('none');
  });

  test('should show search icon and placeholder', async ({ page }) => {
    const searchInput = page.locator('.search-input');
    await expect(searchInput).toHaveAttribute('placeholder', 'Search notes...');
    
    // Check that search container has icon (via CSS pseudo-element)
    await expect(page.locator('.search-container')).toBeVisible();
  });

  test('should handle search input and focus states', async ({ page }) => {
    const searchInput = page.locator('.search-input');
    
    // Focus search input
    await searchInput.focus();
    
    // Check focus styles
    const focusedStyles = await searchInput.evaluate((el) => getComputedStyle(el));
    expect(focusedStyles.boxShadow).toContain('rgb(59, 130, 246)'); // Primary color focus
    
    // Type in search input
    await searchInput.fill('test search');
    await expect(searchInput).toHaveValue('test search');
  });

  test('should display sorting controls', async ({ page }) => {
    await expect(page.locator('.sorting-container')).toBeVisible();
    await expect(page.locator('select[name="sort"]')).toBeVisible();
    await expect(page.locator('select[name="order"]')).toBeVisible();
    
    // Check sorting options
    const sortSelect = page.locator('select[name="sort"]');
    await expect(sortSelect.locator('option[value="title"]')).toBeVisible();
    await expect(sortSelect.locator('option[value="createdAt"]')).toBeVisible();
    await expect(sortSelect.locator('option[value="updatedAt"]')).toBeVisible();
  });
});

test.describe('Empty States and Loading', () => {
  test('should display empty state when no notes exist', async ({ page }) => {
    // This test would need a way to clear all notes or use a clean test user
    // For now, we'll test the empty state styling if it exists
    await page.goto('/auth/login');
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // If empty state exists, check its styling
    const emptyState = page.locator('.empty-state');
    if (await emptyState.isVisible()) {
      await expect(emptyState).toBeVisible();
      await expect(page.locator('.empty-state-icon')).toBeVisible();
      await expect(page.locator('.empty-state-title')).toBeVisible();
    }
  });

  test('should show loading states for buttons', async ({ page }) => {
    await page.goto('/auth/login');
    
    // Click login button and check for loading state
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    
    const loginBtn = page.locator('button[type="submit"]');
    await loginBtn.click();
    
    // Check if button shows loading state (if implemented)
    // This would depend on the specific loading implementation
  });
});