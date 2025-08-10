# Makefile for Nota project
# Provides convenient targets for build, test, code quality checks

.PHONY: help build test test-ui test-ui-headed test-ui-debug test-ui-ci ktlint detekt clean all check pre-commit install-hooks

# Default target
help:
	@echo "Available targets:"
	@echo "  build        - Build the project using Amper"
	@echo "  test         - Run all backend tests using Amper"
	@echo "  test-ui      - Run UI tests using Playwright"
	@echo "  test-ui-headed - Run UI tests in headed mode (visible browser)"
	@echo "  test-ui-debug - Run UI tests in debug mode"
	@echo "  test-ui-ci   - Run UI tests in CI mode (list reporter)"
	@echo "  test-all     - Run both backend and UI tests"
	@echo "  ktlint       - Run ktlint code style checks"
	@echo "  ktlint-fix   - Run ktlint and auto-fix style issues"
	@echo "  detekt       - Run detekt static analysis"
	@echo "  detekt-fix   - Run detekt with auto-correction"
	@echo "  check        - Run all code quality checks (ktlint + detekt)"
	@echo "  pre-commit   - Run pre-commit hooks (ktlint + detekt non-interactive)"
	@echo "  clean        - Clean build artifacts"
	@echo "  all          - Build, test (backend + UI), and run all checks"
	@echo "  install-hooks- Install git pre-commit hooks"
	@echo "  help         - Show this help message"

# Build the project
build:
	@echo "Building project with Amper..."
	./amper build

# Run backend tests
test:
	@echo "Running backend tests with Amper..."
	./amper test

# Run UI tests with Playwright
test-ui:
	@echo "Running UI tests with Playwright..."
	npm run test:ui

# Run UI tests in headed mode (visible browser)
test-ui-headed:
	@echo "Running UI tests in headed mode..."
	npm run test:ui:headed

# Run UI tests in debug mode
test-ui-debug:
	@echo "Running UI tests in debug mode..."
	npm run test:ui:debug

# Run UI tests in CI mode (list reporter)
test-ui-ci:
	@echo "Running UI tests with CI-friendly output..."
	npx playwright test --reporter=list,html

# Run all tests (backend + UI)
test-all: test test-ui
	@echo "All tests (backend + UI) completed"

# Run ktlint style checks
ktlint:
	@echo "Running ktlint style checks..."
	./tools/ktlint.sh --no-interactive

# Run ktlint with auto-fix
ktlint-fix:
	@echo "Running ktlint with auto-fix..."
	ktlint --format "src/**/*.kt" "test/**/*.kt" || true

# Run detekt static analysis
detekt:
	@echo "Running detekt static analysis..."
	./tools/detekt.sh --no-interactive

# Run detekt with auto-correction
detekt-fix:
	@echo "Running detekt with auto-correction..."
	detekt --config ./tools/default-detekt-config.yml --input src/ --auto-correct || true

# Run all code quality checks
check: ktlint detekt
	@echo "All code quality checks completed"

# Run pre-commit hooks
pre-commit:
	@echo "Running pre-commit hooks..."
	./tools/pre-commit.sh

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	./amper clean || true
	@echo "Clean completed"

# Install git hooks
install-hooks:
	@echo "Installing git pre-commit hooks..."
	./tools/install-hooks.sh

# Run everything: build, test (backend + UI), and checks
all: build test-all check
	@echo "Build, all tests, and all checks completed successfully"
