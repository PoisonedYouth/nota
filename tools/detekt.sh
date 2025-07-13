#!/bin/bash

# Check for --no-interactive flag
NO_INTERACTIVE=false
for arg in "$@"; do
    if [ "$arg" == "--no-interactive" ]; then
        NO_INTERACTIVE=true
    fi
done

# Check if detekt is installed
if ! command -v detekt &> /dev/null; then
    echo "detekt is not installed. Installing..."
    cd "$PWD/tools"
    curl -sSLO https://github.com/detekt/detekt/releases/download/v1.23.8/detekt-cli-1.23.8-all.jar
    chmod +x detekt-cli-1.23.8-all.jar
    cd -

    # Create a wrapper script for detekt
    echo '#!/bin/bash
java -jar "'$PWD'/tools/detekt-cli-1.23.8-all.jar" "$@"' > detekt
    chmod +x detekt
    sudo mv detekt /usr/local/bin/
fi

# Run detekt
echo "Running detekt..."
java -jar "$PWD/tools/detekt-cli-1.23.8-all.jar" --config "$PWD/tools/default-detekt-config.yml" --input src/
DETEKT_EXIT_CODE=$?

# Check if there are any errors
if [ $DETEKT_EXIT_CODE -eq 0 ]; then
    echo "No issues found!"
    exit 0
else
    echo "Issues found. Please review the report above."

    if [ "$NO_INTERACTIVE" = true ]; then
        # In non-interactive mode, just exit with the error code
        exit $DETEKT_EXIT_CODE
    else
        # In interactive mode, ask if user wants to fix issues
        read -p "Do you want to run detekt with the --auto-correct flag? (y/n) " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            java -jar "$PWD/tools/detekt-cli-1.23.8-all.jar" --config "$PWD/tools/default-detekt-config.yml" --input src/ --auto-correct
            echo "Auto-correctable issues fixed!"
            exit 0
        else
            exit $DETEKT_EXIT_CODE
        fi
    fi
fi
