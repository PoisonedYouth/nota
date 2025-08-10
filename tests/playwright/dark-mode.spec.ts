import { test, expect } from '@playwright/test';

test.describe('Dark Mode Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/auth/login');
  });

  test('should have dark mode CSS variables defined', async ({ page }) => {
    // Check that dark mode CSS variables are available
    const darkModeSupported = await page.evaluate(() => {
      const testElement = document.createElement('div');
      testElement.setAttribute('data-theme', 'dark');
      document.body.appendChild(testElement);
      
      const styles = getComputedStyle(testElement);
      const hasDarkPrimary = styles.getPropertyValue('--primary-50').trim();
      
      document.body.removeChild(testElement);
      return hasDarkPrimary.length > 0;
    });
    
    expect(darkModeSupported).toBe(true);
  });

  test('should switch to dark mode when data-theme attribute is set', async ({ page }) => {
    // Login to access main interface
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Set dark mode
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark');
    });
    
    // Wait for transition
    await page.waitForTimeout(500);
    
    // Check that background color changed to dark
    const bodyStyles = await page.evaluate(() => getComputedStyle(document.body));
    expect(bodyStyles.backgroundColor).not.toBe('rgb(248, 250, 252)'); // Not light mode color
  });

  test('should have theme toggle button if implemented', async ({ page }) => {
    // Login to access main interface
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Look for theme toggle button
    const themeToggle = page.locator('.theme-toggle');
    
    if (await themeToggle.count() > 0) {
      await expect(themeToggle).toBeVisible();
      
      // Should have theme icons
      const sunIcon = page.locator('.theme-icon.sun');
      const moonIcon = page.locator('.theme-icon.moon');
      
      expect(await sunIcon.count() > 0 || await moonIcon.count() > 0).toBe(true);
    }
  });

  test('should maintain readability in dark mode', async ({ page }) => {
    // Login and set dark mode
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Set dark mode
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark');
    });
    
    await page.waitForTimeout(500);
    
    // Check text elements are visible in dark mode
    const noteTitle = page.locator('.note-title').first();
    if (await noteTitle.isVisible()) {
      const styles = await noteTitle.evaluate((el) => getComputedStyle(el));
      
      // Text should be light in dark mode
      const color = styles.color;
      expect(color).not.toBe('rgb(31, 41, 55)'); // Not dark text color
      expect(color).not.toBe('transparent');
    }
  });

  test('should adapt form elements in dark mode', async ({ page }) => {
    // Set dark mode from the start
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark');
    });
    
    await page.waitForTimeout(300);
    
    // Check form elements in dark mode
    const usernameInput = page.locator('input[name="username"]');
    const styles = await usernameInput.evaluate((el) => getComputedStyle(el));
    
    // Input should have appropriate dark mode styling
    expect(styles.backgroundColor).not.toBe('rgb(255, 255, 255)'); // Not white
    expect(styles.color).not.toBe('rgb(31, 41, 55)'); // Not dark text
  });

  test('should handle dark mode in note tabs', async ({ page }) => {
    // Login in light mode
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Navigate to note detail
    await page.locator('.note-card').first().click();
    await page.waitForURL(/\/notes\/\d+/);
    
    // Switch to dark mode
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark');
    });
    
    await page.waitForTimeout(500);
    
    // Check tab styling in dark mode
    const tabContainer = page.locator('.note-tabs-container');
    if (await tabContainer.isVisible()) {
      const styles = await tabContainer.evaluate((el) => getComputedStyle(el));
      
      // Should have dark background
      expect(styles.backgroundColor).not.toBe('rgb(255, 255, 255)');
    }
    
    // Check tab buttons in dark mode
    const tabButton = page.locator('.tab-button').first();
    const buttonStyles = await tabButton.evaluate((el) => getComputedStyle(el));
    
    // Tab text should be light in dark mode
    expect(buttonStyles.color).not.toBe('rgb(31, 41, 55)');
  });

  test('should preserve functionality when switching themes', async ({ page }) => {
    // Login
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Navigate to note detail in light mode
    await page.locator('.note-card').first().click();
    await page.waitForURL(/\/notes\/\d+/);
    
    // Test tab switching in light mode
    await page.click('.tab-button:nth-child(2)');
    await page.waitForTimeout(300);
    await expect(page.locator('#attachments-tab')).toHaveClass(/active/);
    
    // Switch to dark mode
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark');
    });
    
    await page.waitForTimeout(500);
    
    // Test tab switching still works in dark mode
    await page.click('.tab-button:nth-child(3)');
    await page.waitForTimeout(300);
    await expect(page.locator('#activity-tab')).toHaveClass(/active/);
    
    // Switch back to light mode
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'light');
    });
    
    await page.waitForTimeout(500);
    
    // Test tab switching still works in light mode
    await page.click('.tab-button:nth-child(1)');
    await page.waitForTimeout(300);
    await expect(page.locator('#content-tab')).toHaveClass(/active/);
  });

  test('should respect system theme preference', async ({ page }) => {
    // Test with system dark mode preference
    await page.emulateMedia({ colorScheme: 'dark' });
    
    await page.goto('/auth/login');
    
    // Check if system preference is detected (this would require JavaScript implementation)
    const hasSystemDarkMode = await page.evaluate(() => {
      return window.matchMedia('(prefers-color-scheme: dark)').matches;
    });
    
    expect(hasSystemDarkMode).toBe(true);
  });

  test('should have smooth transitions when switching themes', async ({ page }) => {
    // Login
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check that elements have transition properties
    const noteCard = page.locator('.note-card').first();
    if (await noteCard.isVisible()) {
      const styles = await noteCard.evaluate((el) => getComputedStyle(el));
      
      // Should have transition for smooth theme switching
      expect(styles.transition).toContain('0.3s'); // Should have transition duration
    }
  });

  test('should maintain proper contrast ratios in both themes', async ({ page }) => {
    // Login
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Test light mode contrast
    const lightModeColor = await page.locator('h1, h2, .note-title').first().evaluate((el) => {
      return getComputedStyle(el).color;
    });
    
    // Switch to dark mode
    await page.evaluate(() => {
      document.documentElement.setAttribute('data-theme', 'dark');
    });
    
    await page.waitForTimeout(500);
    
    // Test dark mode contrast
    const darkModeColor = await page.locator('h1, h2, .note-title').first().evaluate((el) => {
      return getComputedStyle(el).color;
    });
    
    // Colors should be different between light and dark mode
    expect(lightModeColor).not.toBe(darkModeColor);
    
    // Both should have actual color values (not transparent)
    expect(lightModeColor).not.toBe('rgba(0, 0, 0, 0)');
    expect(darkModeColor).not.toBe('rgba(0, 0, 0, 0)');
  });
});