#!/bin/bash

# Get the repository root directory
REPO_ROOT_DIR=$(git rev-parse --show-toplevel)

# Make sure the hooks directory exists
mkdir -p "$REPO_ROOT_DIR/.git/hooks"

# Create a symbolic link to the pre-commit hook
echo "Installing pre-commit hook..."
ln -sf "$REPO_ROOT_DIR/tools/pre-commit.sh" "$REPO_ROOT_DIR/.git/hooks/pre-commit"

# Make sure the hook is executable
chmod +x "$REPO_ROOT_DIR/tools/pre-commit.sh"
chmod +x "$REPO_ROOT_DIR/.git/hooks/pre-commit"

echo "Git hooks installed successfully!"
