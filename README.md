# Nota

A secure note-taking web application built with Spring Boot and [Amper](https://github.com/JetBrains/amper) build tool.

## Project Description

Nota is a comprehensive web application for managing personal notes with enterprise-grade security features:

### 🔐 Security Features
- **Enhanced Password Complexity**: 12+ character passwords with uppercase, lowercase, digits, and special characters
- **Secure Password Generation**: Cryptographically secure random password generation for new users
- **Session Management**: Robust authentication and session handling
- **User Role Management**: Admin and user role separation

### 🚀 Application Features
- Clean, modern web interface with responsive design
- HTMX integration for dynamic content updates without page reloads
- Rich text editing with content sanitization
- Note sharing and collaboration
- Activity logging and audit trails
- User management and administrative oversight
- H2 and PostgreSQL database support

### 🏗️ Technical Stack
- **Backend**: Spring Boot 3.x with Kotlin
- **Database**: H2 (development), PostgreSQL (production)
- **Security**: Spring Security with custom authentication
- **Frontend**: Thymeleaf templating with HTMX
- **Build Tool**: Amper build system
- **Testing**: Comprehensive test suite with MockK and Kotest

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
- JDK 21 or newer (Amazon Corretto recommended)
- Git for version control

### Using IntelliJ IDEA with Amper Plugin

1. Open the project in IntelliJ IDEA
2. The Amper plugin should automatically detect the Amper configuration
3. Use the run configuration to run the application

### Using Command Line

Amper provides a command-line interface for building and running projects:

```bash
# Build the project
./amper build

# Run the application
./amper run

# Run tests
./amper test
```

### Using Make (Recommended)

The project includes a Makefile for convenient development tasks:

```bash
# Show all available targets
make help

# Build, test, and run all quality checks
make all

# Run tests only
make test

# Run code style checks
make ktlint

# Run static analysis
make detekt

# Run all quality checks
make check

# Clean build artifacts
make clean

# Install git hooks
make install-hooks
```

## Configuration

The project uses the following Amper configuration file:

- `module.yaml` - Module definition, dependencies, and settings

The main application class is `com.poisonedyouth.nota.Application` with the entry point in `ApplicationKt`.

## CI/CD Pipeline

This project includes a comprehensive GitHub Actions-based CI/CD pipeline for automated testing, code quality checks, and deployments.

### 🔄 Workflows

#### Main CI Pipeline (`ci.yml`)
Runs on every push and pull request to main branches:
- **Parallel Testing**: Unit tests, integration tests, and E2E tests
- **Code Quality**: Automated ktlint style checks and detekt static analysis
- **JAR Building**: Creates versioned application artifacts
- **Security Scanning**: Dependency vulnerability assessment
- **Build Reporting**: Comprehensive summaries with artifact links

#### Release Pipeline (`release.yml`)
Automated release process triggered by GitHub releases:
- **Full Validation**: Complete build and test pipeline execution
- **Artifact Creation**: Versioned JAR files with metadata
- **Release Notes**: Automated generation with installation instructions
- **Docker Preparation**: Dockerfile generation for containerization
- **Asset Management**: Automated upload of release artifacts

#### Dependency Management (`dependency-update.yml`)
Weekly automated maintenance:
- **Dependency Scanning**: Inventory of all project dependencies
- **Security Assessment**: Check for known vulnerabilities
- **Update Recommendations**: Automated analysis of available updates
- **Issue Creation**: Automatic GitHub issues for manual review

#### Code Formatting (`format-code.yml`)
On-demand code formatting:
- **Manual Trigger**: Run via workflow dispatch
- **PR Integration**: Trigger via `/format` comment on pull requests
- **Auto-correction**: Applies ktlint and detekt fixes
- **Auto-commit**: Pushes formatted code back to branch

### 🚀 Usage

#### Running CI Manually
```bash
# Trigger the main CI pipeline
gh workflow run ci.yml

# Create a new release (triggers release pipeline)
gh release create v1.0.0 --title "Release v1.0.0" --notes "Release notes"

# Format code in a PR
# Comment "/format" on any pull request
```

#### Monitoring Builds
- View workflow runs in the GitHub Actions tab
- Download build artifacts from completed runs
- Check build summaries for detailed reports
- Review security scan results and dependency reports

### 🛡️ Quality Gates
All code must pass these automated checks:
- ✅ All unit tests (231+ tests)
- ✅ Integration tests with PostgreSQL
- ✅ ktlint style compliance
- ✅ detekt static analysis
- ✅ Security vulnerability scan
- ✅ JAR build and verification

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
