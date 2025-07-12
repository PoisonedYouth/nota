#!/bin/bash

echo "Running pre-commit hooks..."

# Store the current directory
REPO_ROOT_DIR=$(git rev-parse --show-toplevel)
cd "$REPO_ROOT_DIR" || exit 1

# Run ktlint
echo "Running ktlint..."
"$REPO_ROOT_DIR/tools/ktlint.sh" --no-interactive

# Check if ktlint found errors
if [ $? -ne 0 ]; then
    echo "ktlint found style errors. Please fix them before committing."
    echo "You can run 'tools/ktlint.sh' to fix them automatically."
    exit 1
fi

# Run detekt
echo "Running detekt..."
"$REPO_ROOT_DIR/tools/detekt.sh" --no-interactive

# Check if detekt found issues
if [ $? -ne 0 ]; then
    echo "detekt found issues. Please fix them before committing."
    echo "You can run 'tools/detekt.sh' to fix them automatically."
    exit 1
fi

echo "All pre-commit hooks passed!"
exit 0
