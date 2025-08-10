# Playwright UI Tests for Nota

This directory contains comprehensive UI tests using Playwright to verify the behavior and functionality of the Nota web application's user interface.

## Test Categories

### 1. Note Tabs Tests (`note-tabs.spec.ts`)
Tests the new tabbed interface for note details:
- ✅ Tab switching functionality
- ✅ Content visibility in each tab
- ✅ Mobile responsiveness of tabs
- ✅ Keyboard navigation accessibility

### 2. UI Components Tests (`ui-components.spec.ts`)
Tests the modern design system components:
- ✅ Button styling and hover effects
- ✅ Form controls and focus states
- ✅ Navigation bar responsiveness
- ✅ Note card interactions
- ✅ Search interface functionality

### 3. Responsive Design Tests (`responsive-design.spec.ts`)
Tests UI behavior across different screen sizes:
- ✅ Desktop view (1200px)
- ✅ Tablet view (768px)
- ✅ Mobile view (390px)
- ✅ Touch interactions
- ✅ Layout adaptations

### 4. Accessibility Tests (`accessibility.spec.ts`)
Tests compliance with accessibility standards:
- ✅ Semantic HTML structure
- ✅ Keyboard navigation
- ✅ Focus indicators
- ✅ ARIA labels and roles
- ✅ Color contrast
- ✅ Reduced motion support

### 5. Dark Mode Tests (`dark-mode.spec.ts`)
Tests the dark theme implementation:
- ✅ Theme switching
- ✅ CSS variable updates
- ✅ Readability in dark mode
- ✅ Component adaptation
- ✅ System preference respect

## Running the Tests

### Prerequisites
```bash
npm install
npx playwright install
```

### Run All Tests
```bash
npm run test:ui
```

### Run in Headed Mode (See Browser)
```bash
npm run test:ui:headed
```

### Debug Tests
```bash
npm run test:ui:debug
```

### Generate Test Report
```bash
npm run test:ui:report
```

### Run Specific Test File
```bash
npx playwright test note-tabs.spec.ts
```

### Run Tests on Specific Browser
```bash
npx playwright test --project=chromium
npx playwright test --project=firefox
npx playwright test --project=webkit
```

## Test Configuration

The tests are configured in `playwright.config.ts` with:
- **Base URL**: `http://localhost:8080`
- **Browsers**: Chromium, Firefox, WebKit
- **Mobile Testing**: Pixel 5, iPhone 12
- **Auto Server**: Starts application with `./amper run`

## Test Structure

Each test file follows this pattern:
```typescript
test.describe('Feature Name', () => {
  test.beforeEach(async ({ page }) => {
    // Setup code (login, navigation, etc.)
  });

  test('should do something specific', async ({ page }) => {
    // Test implementation with assertions
  });
});
```

## Key Test Features

### Authentication
All tests automatically handle login with test credentials:
- Username: `testuser`
- Password: `TestPassword123!`

### Wait Strategies
Tests use appropriate waiting strategies:
- `page.waitForURL()` for navigation
- `page.waitForSelector()` for element appearance
- `page.waitForTimeout()` for animations

### Assertions
Tests use Playwright's built-in assertions:
- `expect(element).toBeVisible()`
- `expect(element).toHaveClass()`
- `expect(element).toContainText()`

### Cross-Browser Testing
All tests run on multiple browsers to ensure compatibility:
- **Chromium** (Chrome/Edge)
- **Firefox**
- **WebKit** (Safari)

### Mobile Testing
Responsive tests include mobile viewports:
- **Pixel 5** (390x844)
- **iPhone 12** (390x844)

## Maintenance

### Adding New Tests
1. Create new `.spec.ts` file in this directory
2. Follow existing naming conventions
3. Include appropriate test categories
4. Add authentication setup in `beforeEach`
5. Use proper assertions and wait strategies

### Updating Tests
When UI changes are made:
1. Update relevant selectors
2. Adjust wait times if needed
3. Update assertions for new behavior
4. Run tests to verify changes

### CI Integration
Tests can be integrated into CI/CD pipelines:
```yaml
- name: Run Playwright tests
  run: |
    npm ci
    npx playwright install --with-deps
    npm run test:ui
```

## Troubleshooting

### Common Issues

**Tests timing out:**
- Increase timeout in playwright.config.ts
- Add explicit waits for slow operations

**Selectors not found:**
- Check if UI elements have changed
- Update selectors in test files
- Use Playwright Inspector: `npx playwright test --debug`

**Server not starting:**
- Ensure `./amper run` command works locally
- Check port 8080 is available
- Verify application builds successfully

### Debug Mode
Use debug mode to step through tests:
```bash
npx playwright test --debug note-tabs.spec.ts
```

This opens the Playwright Inspector for interactive debugging.