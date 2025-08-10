import { test, expect } from '@playwright/test';

test.describe('Accessibility Tests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/auth/login');
  });

  test('should have proper semantic HTML structure', async ({ page }) => {
    // Check for main landmarks
    await expect(page.locator('main')).toBeVisible();
    await expect(page.locator('nav')).toBeVisible();
    
    // Login form should have proper form structure
    await expect(page.locator('form')).toBeVisible();
    
    // Check for proper heading hierarchy
    const headings = page.locator('h1, h2, h3, h4, h5, h6');
    const headingCount = await headings.count();
    expect(headingCount).toBeGreaterThan(0);
  });

  test('should have proper form labels', async ({ page }) => {
    // Check that form inputs have associated labels
    const usernameInput = page.locator('input[name="username"]');
    const passwordInput = page.locator('input[name="password"]');
    
    await expect(usernameInput).toBeVisible();
    await expect(passwordInput).toBeVisible();
    
    // Check for labels (either explicit labels or aria-label)
    const usernameId = await usernameInput.getAttribute('id');
    const passwordId = await passwordInput.getAttribute('id');
    
    if (usernameId) {
      await expect(page.locator(`label[for="${usernameId}"]`)).toBeVisible();
    }
    if (passwordId) {
      await expect(page.locator(`label[for="${passwordId}"]`)).toBeVisible();
    }
  });

  test('should be keyboard navigable', async ({ page }) => {
    // Tab through interactive elements
    await page.keyboard.press('Tab');
    let focusedElement = await page.locator(':focus').first();
    await expect(focusedElement).toBeVisible();
    
    await page.keyboard.press('Tab');
    focusedElement = await page.locator(':focus').first();
    await expect(focusedElement).toBeVisible();
    
    await page.keyboard.press('Tab');
    focusedElement = await page.locator(':focus').first();
    await expect(focusedElement).toBeVisible();
  });

  test('should have visible focus indicators', async ({ page }) => {
    const usernameInput = page.locator('input[name="username"]');
    
    // Focus the input
    await usernameInput.focus();
    
    // Check that focus is visible (outline or box-shadow)
    const styles = await usernameInput.evaluate((el) => getComputedStyle(el));
    const hasVisibleFocus = styles.outline !== 'none' || styles.boxShadow !== 'none';
    expect(hasVisibleFocus).toBe(true);
  });

  test('should provide proper button accessibility after login', async ({ page }) => {
    // Login first
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check buttons have proper accessibility
    const buttons = page.locator('button, .btn');
    const buttonCount = await buttons.count();
    
    for (let i = 0; i < Math.min(buttonCount, 5); i++) {
      const button = buttons.nth(i);
      if (await button.isVisible()) {
        // Button should have text content or aria-label
        const textContent = await button.textContent();
        const ariaLabel = await button.getAttribute('aria-label');
        const hasAccessibleName = (textContent && textContent.trim().length > 0) || ariaLabel;
        expect(hasAccessibleName).toBe(true);
      }
    }
  });

  test('should have proper color contrast', async ({ page }) => {
    // Login to access main interface
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check text elements for reasonable contrast
    const textElements = page.locator('p, span, h1, h2, h3, h4, h5, h6, .note-title');
    const elementCount = await textElements.count();
    
    for (let i = 0; i < Math.min(elementCount, 3); i++) {
      const element = textElements.nth(i);
      if (await element.isVisible()) {
        const styles = await element.evaluate((el) => getComputedStyle(el));
        
        // Basic check that text has color (not default transparent)
        expect(styles.color).not.toBe('rgba(0, 0, 0, 0)');
        expect(styles.color).not.toBe('transparent');
      }
    }
  });

  test('should handle reduced motion preference', async ({ page }) => {
    // Set reduced motion preference
    await page.emulateMedia({ reducedMotion: 'reduce' });
    
    // Login to access main interface
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check that animations are disabled or minimized
    const noteCard = page.locator('.note-card').first();
    if (await noteCard.isVisible()) {
      const styles = await noteCard.evaluate((el) => getComputedStyle(el));
      // Animation duration should be very short or none with reduced motion
      const animationDuration = styles.animationDuration;
      const transitionDuration = styles.transitionDuration;
      
      // If animations are present, they should be very quick
      if (animationDuration !== 'none' && animationDuration !== '0s') {
        const duration = parseFloat(animationDuration);
        expect(duration).toBeLessThan(0.1); // Should be very short
      }
    }
  });

  test('should provide proper ARIA labels for interactive elements', async ({ page }) => {
    // Login to access main interface
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check search input
    const searchInput = page.locator('.search-input');
    if (await searchInput.isVisible()) {
      const placeholder = await searchInput.getAttribute('placeholder');
      const ariaLabel = await searchInput.getAttribute('aria-label');
      const hasAccessibleName = placeholder || ariaLabel;
      expect(hasAccessibleName).toBeTruthy();
    }
  });

  test('should be screen reader friendly in note tabs', async ({ page }) => {
    // Login and navigate to note detail
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    await page.locator('.note-card').first().click();
    await page.waitForURL(/\/notes\/\d+/);
    
    // Check tab accessibility
    const tabButtons = page.locator('.tab-button');
    const tabCount = await tabButtons.count();
    
    for (let i = 0; i < tabCount; i++) {
      const tab = tabButtons.nth(i);
      
      // Tab should have role or be a proper button
      const role = await tab.getAttribute('role');
      const tagName = await tab.evaluate(el => el.tagName.toLowerCase());
      
      expect(tagName === 'button' || role === 'tab').toBe(true);
      
      // Tab should have accessible text
      const textContent = await tab.textContent();
      expect(textContent?.trim().length).toBeGreaterThan(0);
    }
    
    // Tab panels should have proper roles
    const tabPanels = page.locator('.tab-panel');
    const panelCount = await tabPanels.count();
    
    for (let i = 0; i < panelCount; i++) {
      const panel = tabPanels.nth(i);
      if (await panel.isVisible()) {
        // Panel should be focusable or have role
        const role = await panel.getAttribute('role');
        const tabIndex = await panel.getAttribute('tabindex');
        
        // Should be accessible to screen readers
        expect(role === 'tabpanel' || tabIndex !== null).toBe(true);
      }
    }
  });

  test('should handle high contrast mode', async ({ page }) => {
    // Simulate high contrast mode by checking that elements have proper borders
    await page.fill('#username', 'testuser');
    await page.fill('#password', 'password');
    await page.click('button[type="submit"]');
    await page.waitForURL('/notes');
    
    // Check that interactive elements have visible boundaries
    const buttons = page.locator('.btn').first();
    if (await buttons.isVisible()) {
      const styles = await buttons.evaluate((el) => getComputedStyle(el));
      
      // Button should have some form of visible boundary
      const hasBorder = styles.border !== 'none' && styles.border !== '0px';
      const hasBackground = styles.backgroundColor !== 'transparent' && styles.backgroundColor !== 'rgba(0, 0, 0, 0)';
      
      expect(hasBorder || hasBackground).toBe(true);
    }
  });

  test('should provide skip links for keyboard navigation', async ({ page }) => {
    // Check for skip to main content link
    await page.keyboard.press('Tab');
    
    // Look for skip link (might be visually hidden)
    const skipLink = page.locator('a[href="#main-content"], .skip-link, a:has-text("Skip to")');
    
    // If skip link exists, it should be focusable
    if (await skipLink.count() > 0) {
      await expect(skipLink.first()).toBeFocused();
    }
  });
});