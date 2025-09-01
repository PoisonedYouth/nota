#!/bin/bash

# Check for --no-interactive flag
NO_INTERACTIVE=false
for arg in "$@"; do
    if [ "$arg" == "--no-interactive" ]; then
        NO_INTERACTIVE=true
    fi
done

# Check if ktlint is installed
if ! command -v ktlint &> /dev/null; then
    echo "ktlint is not installed. Installing..."
    mkdir -p "$PWD/tools"
    cd "$PWD/tools"
    curl -sSLo ktlint https://github.com/pinterest/ktlint/releases/latest/download/ktlint
    chmod +x ktlint
    sudo mv ktlint /usr/local/bin/
    cd -
fi

# Run ktlint
echo "Running ktlint..."
ktlint --color --reporter=plain "src/**/*.kt" "test/**/*.kt"
KTLINT_EXIT_CODE=$?

# Check if there are any errors
if [ $KTLINT_EXIT_CODE -eq 0 ]; then
    echo "No style errors found!"
    exit 0
else
    echo "Style errors found. Run 'ktlint --format' to fix them automatically."

    if [ "$NO_INTERACTIVE" = true ]; then
        # In non-interactive mode, just exit with the error code
        exit $KTLINT_EXIT_CODE
    else
        # In interactive mode, ask if user wants to fix errors
        read -p "Do you want to fix them automatically? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            ktlint --format "src/**/*.kt" "test/**/*.kt"
            echo "Style errors fixed!"
            exit 0
        else
            exit $KTLINT_EXIT_CODE
        fi
    fi
fi
