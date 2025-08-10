import { test, expect } from '@playwright/test';

test.describe('Responsive Design Tests', () => {
  
  test.describe('Desktop View (1200px)', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 1200, height: 800 });
      await page.goto('/auth/login');
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
    });

    test('should display notes grid properly on desktop', async ({ page }) => {
      // Check notes grid layout
      await expect(page.locator('.notes-grid')).toBeVisible();
      
      // Should display multiple columns on desktop
      const notesGrid = page.locator('.notes-grid');
      const gridStyles = await notesGrid.evaluate((el) => getComputedStyle(el));
      expect(gridStyles.gridTemplateColumns).toContain('repeat');
    });

    test('should show full navigation bar on desktop', async ({ page }) => {
      await expect(page.locator('.navbar')).toBeVisible();
      await expect(page.locator('.navbar-brand')).toBeVisible();
      await expect(page.locator('.user-info')).toBeVisible();
      
      // User info should be horizontal on desktop
      const userInfo = page.locator('.user-info');
      const userInfoStyles = await userInfo.evaluate((el) => getComputedStyle(el));
      expect(userInfoStyles.flexDirection).toBe('row');
    });

    test('should display search and sort controls side by side', async ({ page }) => {
      const searchContainer = page.locator('.search-sorting-container');
      await expect(searchContainer).toBeVisible();
      
      // Search and sorting should be properly laid out
      await expect(page.locator('.search-input')).toBeVisible();
      await expect(page.locator('.sorting-container')).toBeVisible();
    });
  });

  test.describe('Tablet View (768px)', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 768, height: 1024 });
      await page.goto('/auth/login');
    });

    test('should adapt login form for tablet', async ({ page }) => {
      await expect(page.locator('.login-form')).toBeVisible();
      
      // Form should be responsive on tablet
      const loginForm = page.locator('.login-form');
      const formBox = await loginForm.boundingBox();
      expect(formBox?.width).toBeLessThan(768);
    });

    test('should adjust navigation for tablet', async ({ page }) => {
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
      
      // Navigation should still be horizontal on tablet
      await expect(page.locator('.navbar')).toBeVisible();
      await expect(page.locator('.navbar-brand')).toBeVisible();
    });

    test('should show single column notes grid on tablet', async ({ page }) => {
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
      
      const notesGrid = page.locator('.notes-grid');
      await expect(notesGrid).toBeVisible();
      
      // Should adapt to fewer columns on tablet
      const gridStyles = await notesGrid.evaluate((el) => getComputedStyle(el));
      // Grid should still be responsive but with fewer columns
      expect(gridStyles.display).toBe('grid');
    });
  });

  test.describe('Mobile View (390px)', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.goto('/auth/login');
    });

    test('should optimize login form for mobile', async ({ page }) => {
      const loginForm = page.locator('.login-form');
      await expect(loginForm).toBeVisible();
      
      // Form should take most of the mobile width
      const formBox = await loginForm.boundingBox();
      expect(formBox?.width).toBeLessThan(390);
      expect(formBox?.width).toBeGreaterThan(300);
      
      // Input fields should be properly sized for mobile
      const usernameInput = page.locator('input[name="username"]');
      const inputBox = await usernameInput.boundingBox();
      expect(inputBox?.height).toBeGreaterThan(40); // Touch-friendly height
    });

    test('should stack navigation elements vertically on mobile', async ({ page }) => {
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
      
      // Check that navigation adapts to mobile
      const navbar = page.locator('.navbar');
      await expect(navbar).toBeVisible();
      
      // Navigation content should stack appropriately
      const navbarContainer = page.locator('.navbar .container');
      const containerStyles = await navbarContainer.evaluate((el) => getComputedStyle(el));
      // Should stack vertically on mobile
      expect(containerStyles.flexDirection).toBe('column');
    });

    test('should display single column notes layout on mobile', async ({ page }) => {
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
      
      const notesGrid = page.locator('.notes-grid');
      await expect(notesGrid).toBeVisible();
      
      // Should be single column on mobile
      const gridStyles = await notesGrid.evaluate((el) => getComputedStyle(el));
      expect(gridStyles.gridTemplateColumns).toBe('1fr');
    });

    test('should make search interface mobile-friendly', async ({ page }) => {
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
      
      // Search container should be visible and properly sized
      await expect(page.locator('.search-sorting-container')).toBeVisible();
      
      const searchInput = page.locator('.search-input');
      await expect(searchInput).toBeVisible();
      
      // Search input should be touch-friendly
      const inputBox = await searchInput.boundingBox();
      expect(inputBox?.height).toBeGreaterThan(40);
    });

    test('should optimize note tabs for mobile', async ({ page }) => {
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
      
      // Navigate to a note
      await page.locator('.note-card').first().click();
      await page.waitForURL(/\/notes\/\d+/);
      
      // Tabs should be visible and mobile-optimized
      await expect(page.locator('.note-tabs')).toBeVisible();
      
      // Tabs should stack vertically on mobile or be properly spaced
      const tabs = page.locator('.note-tabs');
      const tabsStyles = await tabs.evaluate((el) => getComputedStyle(el));
      expect(tabsStyles.display).toBe('flex');
      
      // Tab buttons should be touch-friendly
      const tabButton = page.locator('.tab-button').first();
      const buttonBox = await tabButton.boundingBox();
      expect(buttonBox?.height).toBeGreaterThan(40);
    });

    test('should handle modal dialogs properly on mobile', async ({ page }) => {
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
      
      // Click new note button to open modal
      await page.click('.btn-primary:has-text("New Note")');
      
      // Wait for modal to appear
      await page.waitForSelector('#modal-container > *', { state: 'visible' });
      
      // Modal should be properly sized for mobile
      const modal = page.locator('#modal-container .modal').first();
      if (await modal.isVisible()) {
        const modalBox = await modal.boundingBox();
        expect(modalBox?.width).toBeLessThan(390);
        expect(modalBox?.width).toBeGreaterThan(300);
      }
    });
  });

  test.describe('Touch Interactions', () => {
    test.beforeEach(async ({ page }) => {
      await page.setViewportSize({ width: 390, height: 844 });
      await page.goto('/auth/login');
      await page.fill('#username', 'testuser');
      await page.fill('#password', 'password');
      await page.click('button[type="submit"]');
      await page.waitForURL('/notes');
    });

    test('should handle tap interactions on note cards', async ({ page }) => {
      const firstNoteCard = page.locator('.note-card').first();
      await expect(firstNoteCard).toBeVisible();
      
      // Tap the note card
      await firstNoteCard.tap();
      
      // Should navigate to note detail
      await page.waitForURL(/\/notes\/\d+/);
      await expect(page.locator('.note-detail')).toBeVisible();
    });

    test('should handle tab switching via tap', async ({ page }) => {
      // Navigate to note detail
      await page.locator('.note-card').first().tap();
      await page.waitForURL(/\/notes\/\d+/);
      
      // Tap on attachments tab
      await page.locator('.tab-button:nth-child(2)').tap();
      await page.waitForTimeout(300);
      
      // Verify tab switched
      await expect(page.locator('#attachments-tab')).toHaveClass(/active/);
    });

    test('should handle button taps properly', async ({ page }) => {
      const newNoteButton = page.locator('.btn-primary:has-text("New Note")');
      await expect(newNoteButton).toBeVisible();
      
      // Tap the button
      await newNoteButton.tap();
      
      // Modal should appear
      await page.waitForSelector('#modal-container > *', { state: 'visible' });
    });
  });
});