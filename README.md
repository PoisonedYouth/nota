# Nota

A note-taking web application built with Spring Boot and [Amper](https://github.com/JetBrains/amper) build tool.

## Project Description

Nota is a simple web application that allows users to create and view notes. It features:
- A clean, modern web interface
- HTMX integration for dynamic content updates
- Thymeleaf templating
- H2 and PostgreSQL database support

## Project Structure

- `src/com/poisonedyouth/nota/` - Kotlin source code
  - `Application.kt` - Application entry point
  - `notes/` - Notes feature
    - `CreateNoteDto.kt` - DTO for creating notes
    - `Note.kt` - Domain model for notes
    - `NoteController.kt` - Web controller for notes
    - `NoteDto.kt` - DTO for note representation
    - `NoteRepository.kt` - Data access layer for notes
    - `NoteService.kt` - Business logic for notes
- `resources/` - Application resources
  - `static/` - Static resources
    - `css/` - Stylesheets
  - `templates/` - Thymeleaf templates
    - `layout/` - Layout templates
    - `notes/` - Note-related templates

## Building and Running

### Prerequisites

- IntelliJ IDEA with Amper plugin installed
- JDK 11 or newer

### Using IntelliJ IDEA with Amper Plugin

1. Open the project in IntelliJ IDEA
2. The Amper plugin should automatically detect the Amper configuration
3. Use the run configuration to run the application

### Using Command Line

Amper provides a command-line interface for building and running projects:

```bash
# Build the project
amper build

# Run the application
amper run
```

## Configuration

The project uses the following Amper configuration file:

- `module.yaml` - Module definition, dependencies, and settings

The main application class is `com.poisonedyouth.nota.Application` with the entry point in `ApplicationKt`.

## Code Style and Static Analysis

This project uses two tools to ensure code quality:

1. [ktlint](https://github.com/pinterest/ktlint) - For code style enforcement
2. [detekt](https://github.com/detekt/detekt) - For static code analysis

### Running ktlint

To check your code for style violations:

```bash
./tools/ktlint.sh
```

This script will:
1. Check if ktlint is installed, and install it if necessary
2. Run ktlint on all Kotlin files in the project
3. Offer to automatically fix any style violations found

### Running detekt

To perform static code analysis:

```bash
./tools/detekt.sh
```

This script will:
1. Check if detekt is installed, and install it if necessary
2. Run detekt on all Kotlin files in the project
3. Offer to automatically fix auto-correctable issues if any are found

### Git Hooks

This project includes Git hooks to automatically run ktlint and detekt before each commit. This ensures that all committed code meets the project's style and quality standards.

#### Installing Git Hooks

To install the Git hooks:

```bash
./tools/install-hooks.sh
```

This script will:
1. Create a symbolic link from the pre-commit hook script to the Git hooks directory
2. Make the hook executable

#### Pre-commit Hook

The pre-commit hook will:
1. Run ktlint on all Kotlin files
2. Run detekt on all Kotlin files
3. Prevent the commit if any issues are found

If the hook blocks your commit due to style or quality issues, you can fix them by running:
```bash
./tools/ktlint.sh  # To fix style issues
./tools/detekt.sh  # To fix quality issues
```

### Configuration

- Ktlint is configured using the `.editorconfig` file at the root of the project.
- Detekt is configured using the `default-detekt-config.yml` file in the tools directory.

These files define the code style and quality rules that the tools will enforce.

### IDE Integration

For the best development experience, it's recommended to install plugins for your IDE:

- IntelliJ IDEA: 
  - [Ktlint plugin](https://plugins.jetbrains.com/plugin/15057-ktlint-unofficial-)
  - [Detekt plugin](https://plugins.jetbrains.com/plugin/10761-detekt)
- VS Code: 
  - [Ktlint extension](https://marketplace.visualstudio.com/items?itemName=mathiasfrohlich.Kotlin)
  - [Detekt extension](https://marketplace.visualstudio.com/items?itemName=detekt.vscode-detekt)
