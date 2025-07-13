# Makefile for Nota project
# Provides convenient targets for build, test, code quality checks

.PHONY: help build test ktlint detekt clean all check pre-commit install-hooks

# Default target
help:
	@echo "Available targets:"
	@echo "  build        - Build the project using Amper"
	@echo "  test         - Run all tests using Amper"
	@echo "  ktlint       - Run ktlint code style checks"
	@echo "  ktlint-fix   - Run ktlint and auto-fix style issues"
	@echo "  detekt       - Run detekt static analysis"
	@echo "  detekt-fix   - Run detekt with auto-correction"
	@echo "  check        - Run all code quality checks (ktlint + detekt)"
	@echo "  pre-commit   - Run pre-commit hooks (ktlint + detekt non-interactive)"
	@echo "  clean        - Clean build artifacts"
	@echo "  all          - Build, test, and run all checks"
	@echo "  install-hooks- Install git pre-commit hooks"
	@echo "  help         - Show this help message"

# Build the project
build:
	@echo "Building project with Amper..."
	./amper build

# Run tests
test:
	@echo "Running tests with Amper..."
	./amper test

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

# Run everything: build, test, and checks
all: build test check
	@echo "Build, test, and all checks completed successfully"
